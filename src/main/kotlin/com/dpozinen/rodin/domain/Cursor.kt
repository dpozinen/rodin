package com.dpozinen.rodin.domain

import jakarta.persistence.*

@Entity
@Table(name = "chat_cursors")
class Cursor(

    var wordCount: Short,

    var cursor: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var language: ChatLanguage,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
    constructor(wordCount: Short, cursor: Int, language: ChatLanguage, chatId: String) : this(
        wordCount, cursor, language, null
    )
}