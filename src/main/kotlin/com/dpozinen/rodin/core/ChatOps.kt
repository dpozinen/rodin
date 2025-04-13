package com.dpozinen.rodin.core

import com.dpozinen.rodin.domain.Chat
import org.springframework.data.redis.core.RedisOperations
import org.springframework.stereotype.Service

@Service
class ChatOps(
    private val chatTemplate: RedisOperations<String, Chat>
) {

    fun chat(chatId: String): Chat = chatTemplate.opsForValue().get(chatId)!!

    fun maybeCreate(chatId: String): Chat {
        return if (chatTemplate.hasKey(chatId) == true) {
            chatTemplate.opsForValue().get(chatId)!!
        } else {
            chatTemplate.opsForValue().set(chatId, Chat(chatId))
            chatTemplate.opsForValue().get(chatId)!!
        }
    }

    fun set(chatId: String, set: (Chat) -> Unit) {
        chat(chatId)
            .let { chat ->
                chatTemplate.opsForValue().set(chatId,
                    chat.also { set(it) }
                )
            }
    }

    fun set(chat: Chat) {
        chatTemplate.opsForValue().set(chat.id, chat)
    }
}