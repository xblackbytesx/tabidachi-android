package com.example.tabidachi.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiTripSummary(
    val id: String,
    val title: String,
    val startDate: String,
    val endDate: String,
    val homeLocation: String? = null,
    val timezone: String? = null,
    val coverColor: String? = null,
    val coverImageUrl: String? = null,
    val coverImageCredit: String? = null,
    val legCount: Int,
    val updatedAt: String,
)

@Serializable
data class ApiTripDetail(
    val id: String,
    val title: String,
    val startDate: String,
    val endDate: String,
    val homeLocation: String? = null,
    val timezone: String? = null,
    val coverColor: String? = null,
    val coverImageUrl: String? = null,
    val coverImageCredit: String? = null,
    val legCount: Int,
    val updatedAt: String,
    val data: ApiTripData,
)

@Serializable
data class ApiTripData(
    val schemaVersion: String = "1.0",
    val title: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val homeLocation: String? = null,
    val timezone: String? = null,
    val legs: List<ApiLeg> = emptyList(),
)

@Serializable
data class ApiLeg(
    val sequence: Int,
    val destination: String,
    val region: String? = null,
    val startDate: String,
    val endDate: String,
    val accommodation: ApiAccommodation? = null,
    val notes: String? = null,
    val days: List<ApiDay> = emptyList(),
    @SerialName("coverImageURL")
    val coverImageUrl: String? = null,
    @SerialName("coverImageCredit")
    val coverImageCredit: String? = null,
)

@Serializable
data class ApiAccommodation(
    val name: String,
    val neighborhood: String? = null,
    val address: String? = null,
    val checkIn: String,
    val checkOut: String,
    val bookingReference: String? = null,
)

@Serializable
data class ApiDay(
    val date: String,
    val label: String? = null,
    val type: String = "normal",
    val notes: String? = null,
    val events: List<ApiEvent> = emptyList(),
)

@Serializable
data class ApiEvent(
    val sequence: Int,
    val type: String,
    val title: String,
    val startTime: String? = null,
    val endTime: String? = null,
    val duration: String? = null,
    val notes: String? = null,
    val optional: Boolean = false,
    // activity fields
    val location: String? = null,
    val address: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val url: String? = null,
    val ticketRequired: Boolean = false,
    val bookingReference: String? = null,
    @SerialName("imageURL")
    val imageUrl: String? = null,
    @SerialName("imageThumbURL")
    val imageThumbUrl: String? = null,
    val imageCredit: String? = null,
    // transit fields
    val transportMode: String? = null,
    val departure: ApiTransitPoint? = null,
    val arrival: ApiTransitPoint? = null,
    val carrier: String? = null,
    val flightNumber: String? = null,
    // accommodation event fields
    val checkIn: Boolean = false,
    val checkOut: Boolean = false,
)

@Serializable
data class ApiTransitPoint(
    val location: String,
    val code: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
)
