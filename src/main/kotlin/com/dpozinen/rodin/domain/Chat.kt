package com.dpozinen.rodin.domain

import jakarta.persistence.*

@Entity
@Table(name = "chats")
class Chat(

    @Id
    var id: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var currentLanguage: ChatLanguage,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var command: ChatCommand,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    var cursors: MutableList<Cursor> = mutableListOf()
) {

    constructor(chatId: String) : this(
        chatId,
        ChatLanguage.FR,
        ChatCommand.NONE,
        mutableListOf(
            Cursor(5, 0, ChatLanguage.FR, chatId),
            Cursor(5, 0, ChatLanguage.ES, chatId)
        )
    )

    fun currentCursor() = cursors.first { it.language == currentLanguage }

}
