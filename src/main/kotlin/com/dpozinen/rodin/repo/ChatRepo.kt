package com.dpozinen.rodin.repo

import com.dpozinen.rodin.domain.Chat
import com.dpozinen.rodin.domain.Offset
import org.springframework.data.jpa.repository.JpaRepository

interface ChatRepo : JpaRepository<Chat, String>

interface OffsetRepo : JpaRepository<Offset, String>