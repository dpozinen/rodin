package com.dpozinen.rodin.core

import com.dpozinen.rodin.domain.Chat
import com.dpozinen.rodin.domain.ChatCommand
import com.dpozinen.rodin.domain.ChatCommand.*
import com.dpozinen.rodin.domain.ChatLanguage
import com.dpozinen.rodin.domain.Offset
import com.dpozinen.rodin.repo.OffsetRepo
import com.dpozinen.rodin.rest.GetUpdatesResponse
import com.dpozinen.rodin.rest.Telegram
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service


@Service
class AugusteRodin(
    private val chatOps: ChatOps,
    private val offsets: OffsetRepo,
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
        if (offsets.findAll().isEmpty()) {
            offsets.save(Offset())
        }
    }

    @Scheduled(cron = "\${rodin.send-messages.cron}")
    fun sendMessages() {
        log.info("Sending messages")
        chatOps.forEach { runBlocking { sendNext(it) } }
    }

    private fun sendNext(chat: Chat) {
        var addRepeatedMessaged = false
        val cursor = chat.currentCursor()

        if (cursor.cursor >= words[chat.currentLanguage]!!.count - cursor.wordCount - 1) {
            cursor.cursor = 0

            addRepeatedMessaged = true
        }

        val newCursor = cursor.cursor + cursor.wordCount
        val batch = words[chat.currentLanguage]!!.words.subList(cursor.cursor, newCursor)

        val chatId = chat.id!!
        if (addRepeatedMessaged) telegram.sendMessage(chatId, "⚠\uFE0F Word cycle complete")

        batch.forEach { word -> telegram.sendMessage(chatId, word.asMessage(), markdown = true) }

        chatOps.set(chatId) { it.currentCursor().cursor = newCursor }

        telegram.sendMessage(chatId, "||cursor at ${newCursor}||\n", markdown = true)
    }

    @Scheduled(cron = "\${rodin.get-updates.cron}")
    fun getUpdates() {
        runBlocking {
            offsets.findById(Offset.ID).ifPresent { offset ->
                runBlocking {
                    telegram.getUpdates(offset.chatOffset)
                        .result
                        .takeIf { it.isNotEmpty() }
                        ?.also {
                            offset.chatOffset = it.last().updateId + 1
                            offsets.save(offset)
                        }
                        ?.groupBy { it.message.chat.id.toString() }
                        ?.forEach { (chatId, updates) -> handleChatUpdates(updates, chatId) }
                }
            }
        }
    }

    private fun handleChatUpdates(updates: List<GetUpdatesResponse.Result>, chatId: String) {
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
                        telegram.sendMessage(chatId, "Now send the desired cursor. Current is ${chat.currentCursor().cursor}")
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
                    SET_CURSOR -> trySetCursor(text, chatId)
                    LANG -> trySetLanguage(text, chatId)
                    else -> {}
                }
            }
        }
    }

    private fun trySetCursor(text: String, chatId: String) {
        runCatching {
            text.trim().toInt().let { cursor -> chatOps.set(chatId) {
                it.currentCursor().cursor = cursor
            } }
            telegram.sendMessage(chatId, "Cursor is now at $text")
        }.onFailure {
            telegram.sendMessage(chatId, "$text is not a valid cursor value, did not update cursor")
        }
    }

    private fun trySetLanguage(text: String, chatId: String) {
        runCatching {
            ChatLanguage.valueOf(text.trim().uppercase())
                .let { language -> chatOps.set(chatId) { it.currentLanguage = language } }
            telegram.sendMessage(chatId, "Language is now $text")
        }.onFailure {
            telegram.sendMessage(chatId, "$text is not a valid language value. Possible values: ${enumValues<ChatLanguage>().map { it.name }}")
        }
    }

}