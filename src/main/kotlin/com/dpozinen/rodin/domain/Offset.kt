package com.dpozinen.rodin.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "offsets")
class Offset(
    @Id
    var id: String = ID,
    var chatOffset: Long? = null
) {

    companion object {
        const val ID: String = "offset_id"
    }

}