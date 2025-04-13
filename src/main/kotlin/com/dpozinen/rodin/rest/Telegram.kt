package com.dpozinen.rodin.rest

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.WebClientResponseException

@Service
class Telegram(private val client: RestClient) {

    val log: Logger = LoggerFactory.getLogger(javaClass)

    fun getUpdates(offset: Long?): GetUpdatesResponse {
        return client.get()
            .uri { builder ->
                builder.path("/getUpdates")
                    .also { offset?.let { builder.queryParam("offset", it) } }
                    .build()
            }
            .retrieve()
            .body(GetUpdatesResponse::class.java)!!
    }

    fun sendMessage(
        chatId: String,
        text: String,
        markdown: Boolean = false,
        hidePreview: Boolean = true,
    ) {
        runCatching {
            client.post().uri("/sendMessage")
                .body(requestBody(chatId, text, hidePreview, markdown))
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .retrieve().toBodilessEntity()
        }.onFailure {
            if (it is WebClientResponseException) {
                log.error("Error response from Telegram for message '$text': ${it.responseBodyAsString}")
            } else {
                log.error("Failed to send message: $it")
            }
        }
    }

    private fun requestBody(
        chatId: String,
        text: String,
        hidePreview: Boolean,
        markdown: Boolean
    ) =
        """{
                "chat_id": "$chatId",
                "text": "$text",
                "link_preview_options": {
                    "is_disabled": $hidePreview
                }
                ${if (markdown) ",\"parse_mode\": \"MarkdownV2\"" else ""}
            }""".trimIndent()


}
