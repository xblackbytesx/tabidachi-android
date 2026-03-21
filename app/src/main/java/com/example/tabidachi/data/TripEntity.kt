package com.example.tabidachi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val title: String,
    val startDate: String,
    val endDate: String,
    val homeLocation: String?,
    val timezone: String?,
    val coverColor: String?,
    val coverImageUrl: String?,
    val coverImageCredit: String?,
    val legCount: Int,
    val updatedAt: String,
    val detailJson: String?,
    val lastSyncedAt: Long,
)
