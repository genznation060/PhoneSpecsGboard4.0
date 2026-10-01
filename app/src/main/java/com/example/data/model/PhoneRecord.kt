package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "phones",
    indices = [
        Index(value = ["brand"]),
        Index(value = ["model"]),
        Index(value = ["fullName"]),
        Index(value = ["isFavorite"]),
        Index(value = ["usageCount"])
    ]
)
data class PhoneRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val brand: String,
    val model: String,
    val fullName: String,
    val originalTsv: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val usageCount: Int = 0,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false
)
