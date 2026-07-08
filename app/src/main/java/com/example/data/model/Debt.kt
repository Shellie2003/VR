package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val debtorName: String,
    val amount: Double,
    val balance: Double,
    val date: Long,
    val note: String,
    val isPaid: Boolean = false
)
