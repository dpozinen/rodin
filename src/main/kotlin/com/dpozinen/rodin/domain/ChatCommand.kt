package com.dpozinen.rodin.domain

enum class ChatCommand(val msg: String) {
    START("/start"), SET_CURSOR("/cursor"), MORE("/more"), OTHER("other"), NONE("_none_");

    companion object {
        fun from(msg: String): ChatCommand {
            return entries.find { it.msg == msg } ?: OTHER
        }
    }
}