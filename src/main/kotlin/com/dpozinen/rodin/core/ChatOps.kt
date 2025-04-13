package com.dpozinen.rodin.core

import com.dpozinen.rodin.domain.Chat
import com.dpozinen.rodin.repo.ChatRepo
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

@Service
class ChatOps(
    private val chats: ChatRepo
) {

    fun chat(chatId: String): Chat = chats.findById(chatId).getOrNull()!!

    fun maybeCreate(chatId: String): Chat {
        return chats.findById(chatId).orElseGet { set(Chat(chatId))  }
    }

    fun set(chatId: String, set: (Chat) -> Unit) {
        chat(chatId)
            .let { chat ->
                chat.also { set(it) }
                chats.save(chat)
            }
    }

    fun set(chat: Chat) = chats.save(chat)

    fun forEach(action: (Chat) -> Unit) {
        chats.findAll().forEach { action(it) }
    }
}