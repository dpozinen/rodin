package com.dpozinen.rodin.rest

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters.fromValue
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import reactor.core.publisher.Mono

@Service
class Telegram(private val webclient: WebClient) {

    val log: Logger = LoggerFactory.getLogger(javaClass)

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
    ) {
        runCatching {
            webclient.post().uri("/sendMessage")
                .body(requestBody(chatId, text, hidePreview, markdown))
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .retrieve()
                .bodyToMono(String::class.java)
                .onErrorResume(WebClientResponseException::class.java) { ex ->
                    Mono.just(ex.responseBodyAsString)
                        .doOnNext { log.error("Error response from Telegram for message '$text': $it") }
                }
                .subscribe()
        }.onFailure {
            log.error("Failed to send message: $it")
        }
    }

    private fun requestBody(
        chatId: String,
        text: String,
        hidePreview: Boolean,
        markdown: Boolean
    ) = fromValue(
        """{
                "chat_id": "$chatId",
                "text": "$text",
                "link_preview_options": {
                    "is_disabled": $hidePreview
                }
                ${if (markdown) ",\"parse_mode\": \"MarkdownV2\"" else ""}
            }""".trimIndent()
    )

}
