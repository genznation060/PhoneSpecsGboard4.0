package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class PhoneWithFields(
    @Embedded
    val phone: PhoneRecord,
    @Relation(
        parentColumn = "id",
        entityColumn = "phoneId"
    )
    val fields: List<PhoneField> = emptyList()
)
