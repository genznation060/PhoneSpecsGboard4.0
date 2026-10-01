package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "phone_fields",
    foreignKeys = [
        ForeignKey(
            entity = PhoneRecord::class,
            parentColumns = ["id"],
            childColumns = ["phoneId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["phoneId"]),
        Index(value = ["category"]),
        Index(value = ["fieldName"]),
        Index(value = ["isFavorite"]),
        Index(value = ["usageCount"])
    ]
)
data class PhoneField(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val phoneId: Long,
    val columnIndex: Int,
    val fieldName: String,
    val category: String,
    val fieldValue: String,
    val usageCount: Int = 0,
    val isFavorite: Boolean = false
)
