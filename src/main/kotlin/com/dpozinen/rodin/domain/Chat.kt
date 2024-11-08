package com.dpozinen.rodin.domain

class Chat(

    val id: String,
    var cursor: Int,
    var wordCount: Short,
    var command: ChatCommand

)
