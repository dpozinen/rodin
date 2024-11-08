package com.dpozinen.rodin.rest

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Service
class Telegram(private val webclient: WebClient) {

    suspend fun getUpdates(offset: Long?): GetUpdatesResponse {
        return webclient.get()
            .uri { builder ->
                builder.path("/getUpdates")
                    .also { offset?.let { builder.queryParam("offset", it) } }
                    .build()
            }
            .retrieve()
            .awaitBody<GetUpdatesResponse>()
    }

    suspend fun sendMessage(
        chatId: String,
        text: String,
        markdown: Boolean = false,
        hidePreview: Boolean = true,
    ) = webclient.post().uri("/sendMessage")
        .body(BodyInserters.fromValue(
            """
                    {
                        "chat_id": "$chatId",
                        "text": "$text",
                        "link_preview_options": {
                            "is_disabled": $hidePreview
                        },
                        ${markdown.takeIf { it }?.let { "\"parse_mode\": \"MarkdownV2\"" } ?: ""}
                    }
                """.trimIndent()
        ))
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .retrieve()
        .awaitBody<SendMessageResponse>()

}
