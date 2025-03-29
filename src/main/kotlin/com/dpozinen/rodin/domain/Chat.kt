package com.dpozinen.rodin.domain

class Chat(

    val id: String,
    var currentLanguage: ChatLanguage,
    var cursors: Map<ChatLanguage, Cursor>,
    var command: ChatCommand

) {
    constructor(chatId: String) : this(chatId, ChatLanguage.FR, mapOf(
        ChatLanguage.FR to Cursor(5, 0, ChatLanguage.FR),
        ChatLanguage.ES to Cursor(5, 0, ChatLanguage.ES),
    ), ChatCommand.NONE)

    class Cursor(
        var wordCount: Short,
        var cursor: Int,
        var language: ChatLanguage,
    )

}
