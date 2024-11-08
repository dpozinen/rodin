package com.dpozinen.rodin.core

import java.net.URLEncoder.encode
import kotlin.text.Charsets.UTF_8


private const val gpt = "Breakdown, Translate, Usages of: "

data class Word(val french: String, val english: String) {

    var escaped = "_*[]()~`>#+=|{}.!"

    fun asMessage() = "${escapeMd(french)} \\\\| ||${escapeMd(english)}||" +
            " [ChatGPT](https://chatgpt.com/?q=${gptQuery(french)})\n"

    private fun gptQuery(french: String) = encode(gpt + french, UTF_8)

    private fun escapeMd(text: String) = text
        .map { if (escaped.contains(it)) "\\\\$it" else it.toString() }
        .joinToString("")

}

data class Words(val count: Int, val words: List<Word>)
