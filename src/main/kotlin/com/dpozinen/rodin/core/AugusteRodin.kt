package com.dpozinen.rodin.core

import com.dpozinen.rodin.domain.Chat
import com.dpozinen.rodin.domain.ChatCommand
import com.dpozinen.rodin.domain.ChatCommand.MORE
import com.dpozinen.rodin.domain.ChatCommand.OTHER
import com.dpozinen.rodin.domain.ChatCommand.SET_CURSOR
import com.dpozinen.rodin.domain.Offset
import com.dpozinen.rodin.rest.GetUpdatesResponse
import com.dpozinen.rodin.rest.Telegram
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.ReactiveRedisOperations
import org.springframework.data.redis.core.scanAsFlow
import org.springframework.data.redis.core.setAndAwait
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service


@Service
class AugusteRodin(
    private val chatOps: ChatOps,
    private val chatTemplate: ReactiveRedisOperations<String, Chat>,
    private val offsetTemplate: ReactiveRedisOperations<String, Offset>,
    private val telegram: Telegram,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    private val words: Words = jacksonObjectMapper()
        .readValue({}.javaClass.getResource("/words.json"), Words::class.java)

    @PostConstruct
    fun initOffset() {
        runBlocking {
            offsetTemplate.opsForValue().get(Offset.ID).awaitSingleOrNull()
                ?: offsetTemplate.opsForValue().setAndAwait(Offset.ID, Offset())
        }
    }

    @Scheduled(cron = "\${rodin.send-messages.cron}")
    fun sendMessages() {
        runBlocking {
            log.info("Sending messages")
            chatTemplate.scanAsFlow().map { chatOps.chat(it) }.onEach { sendNext(it) }.collect()
        }
    }

    private suspend fun sendNext(chat: Chat) {
        var addRepeatedMessaged = false

        if (chat.cursor >= words.count - chat.wordCount - 1) {
            chatTemplate.opsForValue()
                .set(chat.id, chat.also { it.cursor = 0 })
                .awaitSingle()

            addRepeatedMessaged = true
        }

        val newCursor = chat.cursor + chat.wordCount
        val batch = words.words.subList(chat.cursor, newCursor)

        if (addRepeatedMessaged) telegram.sendMessage(chat.id, "⚠\uFE0F Word cycle complete")

        batch.forEach { word -> telegram.sendMessage(chat.id, word.asMessage(), markdown = true) }

        chatTemplate.opsForValue()
            .set(chat.id, chat.also { it.cursor = newCursor })
            .awaitSingle()

        telegram.sendMessage(chat.id, "||cursor at ${newCursor}||\n", markdown = true)
    }

    @Scheduled(cron = "\${rodin.get-updates.cron}")
    fun getUpdates() {
        runBlocking {
            offsetTemplate.opsForValue().get(Offset.ID)
                .awaitSingle()
                .offset
                .let { offset ->
                    telegram.getUpdates(offset)
                        .result
                        .takeIf { it.isNotEmpty() }
                        ?.also {
                            offsetTemplate.opsForValue()
                                .set(Offset.ID, Offset(offset = it.last().updateId + 1))
                                .awaitSingle()
                        }
                        ?.groupBy { it.message.chat.id.toString() }
                        ?.forEach { (chatId, updates) -> handleChatUpdates(updates, chatId) }
                }
        }
    }

    private suspend fun handleChatUpdates(updates: List<GetUpdatesResponse.Result>, chatId: String) {
        val chat = chatOps.maybeCreate(chatId)
        var command = chat.command

        updates.forEach { update ->
            val text = update.message.text

            if (text.startsWith("/")) {
                command = ChatCommand.from(text)
                chat.command = command
                when (command) {
                    MORE -> sendNext(chat)
                    OTHER -> telegram.sendMessage(chatId, "$text is not a command")
                    SET_CURSOR -> {
                        telegram.sendMessage(chatId, "Now send the desired cursor. Current is ${chat.cursor}")
                        chatOps.set(chat)
                    }

                    else -> {}
                }
            } else {
                when (command) {
                    SET_CURSOR -> {
                        trySetCursor(text, chatId)
                        telegram.sendMessage(chatId, "Cursor is now at $text")
                    }

                    else -> {}
                }
            }
        }
    }

    private suspend fun trySetCursor(text: String, chatId: String) {
        runCatching {
            text.trim().toInt().let { cursor -> chatOps.set(chatId) { it.cursor = cursor } }
        }.onFailure {
            telegram.sendMessage(chatId, "$text is not a valid cursor value, did not update cursor")
        }
    }

}