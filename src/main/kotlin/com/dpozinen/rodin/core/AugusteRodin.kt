package com.dpozinen.rodin.core

import com.dpozinen.rodin.domain.Chat
import com.dpozinen.rodin.domain.ChatCommand
import com.dpozinen.rodin.domain.ChatCommand.*
import com.dpozinen.rodin.domain.ChatLanguage
import com.dpozinen.rodin.domain.Offset
import com.dpozinen.rodin.rest.GetUpdatesResponse
import com.dpozinen.rodin.rest.Telegram
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisOperations
import org.springframework.data.redis.core.ScanOptions
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service


@Service
class AugusteRodin(
    private val chatOps: ChatOps,
    private val chatTemplate: RedisOperations<String, Chat>,
    private val offsetTemplate: RedisOperations<String, Offset>,
    private val telegram: Telegram,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    private val words: Map<ChatLanguage, Words> = mapOf(
        ChatLanguage.FR to jacksonObjectMapper()
            .readValue({}.javaClass.getResource("/fr-words.json"), Words::class.java),
        ChatLanguage.ES to jacksonObjectMapper()
            .readValue({}.javaClass.getResource("/es-words.json"), Words::class.java)
    )

    @PostConstruct
    fun initOffset() {
        runBlocking {
            offsetTemplate.opsForValue().get(Offset.ID)
                ?: offsetTemplate.opsForValue().set(Offset.ID, Offset())
        }
    }

    @Scheduled(cron = "\${rodin.send-messages.cron}")
    fun sendMessages() {
        log.info("Sending messages")
        chatTemplate.scan(ScanOptions.NONE).forEachRemaining {
            runBlocking {
                sendNext(chatOps.chat(it))
            }
        }
    }

    private suspend fun sendNext(chat: Chat) {
        var addRepeatedMessaged = false
        val cursor = chat.cursors[chat.currentLanguage]!!

        if (cursor.cursor >= words[chat.currentLanguage]!!.count - cursor.wordCount - 1) {
            chatTemplate.opsForValue()
                .set(chat.id, chat.also { it.cursors[it.currentLanguage]!!.cursor = 0 })

            addRepeatedMessaged = true
        }

        val newCursor = cursor.cursor + cursor.wordCount
        val batch = words[chat.currentLanguage]!!.words.subList(cursor.cursor, newCursor)

        if (addRepeatedMessaged) telegram.sendMessage(chat.id, "⚠\uFE0F Word cycle complete")

        batch.forEach { word -> telegram.sendMessage(chat.id, word.asMessage(), markdown = true) }

        chatTemplate.opsForValue()
            .set(chat.id, chat.also { it.cursors[it.currentLanguage]!!.cursor = newCursor })

        telegram.sendMessage(chat.id, "||cursor at ${newCursor}||\n", markdown = true)
    }

    @Scheduled(cron = "\${rodin.get-updates.cron}")
    fun getUpdates() {
        runBlocking {
            offsetTemplate.opsForValue().get(Offset.ID)
                ?.offset
                .let { offset ->
                    telegram.getUpdates(offset)
                        .result
                        .takeIf { it.isNotEmpty() }
                        ?.also {
                            offsetTemplate.opsForValue()
                                .set(Offset.ID, Offset(offset = it.last().updateId + 1))
                        }
                        ?.groupBy { it.message.chat.id.toString() }
                        ?.forEach { (chatId, updates) -> handleChatUpdates(updates, chatId) }
                }
        }
    }

    private suspend fun handleChatUpdates(updates: List<GetUpdatesResponse.Result>, chatId: String) {
        val chat = chatOps.maybeCreate(chatId)
        var command = chat.command
        val cursor = chat.cursors[chat.currentLanguage]!!

        updates.forEach { update ->
            val text = update.message.text

            if (text.startsWith("/")) {
                command = ChatCommand.from(text)
                chat.command = command
                when (command) {
                    MORE -> sendNext(chat)
                    OTHER -> telegram.sendMessage(chatId, "$text is not a command")
                    SET_CURSOR -> {
                        telegram.sendMessage(chatId, "Now send the desired cursor. Current is ${cursor.cursor}")
                        chatOps.set(chat)
                    }
                    LANG -> {
                        telegram.sendMessage(chatId, "Now send the desired language. Current is ${chat.currentLanguage}")
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
                    LANG -> {
                        trySetLanguage(text, chatId)
                        telegram.sendMessage(chatId, "Language is now $text")
                    }

                    else -> {}
                }
            }
        }
    }

    private suspend fun trySetCursor(text: String, chatId: String) {
        runCatching {
            text.trim().toInt().let { cursor -> chatOps.set(chatId) { it.cursors[it.currentLanguage]!!.cursor = cursor } }
        }.onFailure {
            telegram.sendMessage(chatId, "$text is not a valid cursor value, did not update cursor")
        }
    }

    private suspend fun trySetLanguage(text: String, chatId: String) {
        runCatching {
            ChatLanguage.valueOf(text.trim().uppercase())
                .let { cursor -> chatOps.set(chatId) { it.currentLanguage = cursor } }
        }.onFailure {
            telegram.sendMessage(chatId, "$text is not a valid language value. Possible values: ${ChatLanguage.entries.toTypedArray()}")
        }
    }

}