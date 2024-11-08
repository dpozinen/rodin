package com.dpozinen.rodin.core

import com.dpozinen.rodin.domain.Chat
import com.dpozinen.rodin.domain.ChatCommand.NONE
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.redis.core.ReactiveRedisOperations
import org.springframework.data.redis.core.hasKeyAndAwait
import org.springframework.data.redis.core.setAndAwait
import org.springframework.stereotype.Service

@Service
class ChatOps(
    private val chatTemplate: ReactiveRedisOperations<String, Chat>
) {

    suspend fun chat(chatId: String): Chat = chatTemplate.opsForValue().get(chatId).awaitSingle()

    suspend fun maybeCreate(chatId: String): Chat {
        return if (chatTemplate.hasKeyAndAwait(chatId)) {
            chatTemplate.opsForValue().get(chatId).awaitSingle()
        } else {
            chatTemplate.opsForValue().set(chatId, Chat(chatId, 0, 5, NONE)).awaitSingle()
            chatTemplate.opsForValue().get(chatId).awaitSingle()
        }
    }

    suspend fun set(chatId: String, set: (Chat) -> Unit) {
        chat(chatId)
            .let { chat ->
                chatTemplate.opsForValue().setAndAwait(chatId,
                    chat.also { set(it) }
                )
            }
    }

    suspend fun set(chat: Chat) {
        chatTemplate.opsForValue().setAndAwait(chat.id, chat)
    }
}