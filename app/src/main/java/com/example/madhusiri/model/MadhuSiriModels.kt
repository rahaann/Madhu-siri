package com.example.madhusiri.model

enum class UserRole {
    Beekeeper,
    Farmer,
}

data class MadhuUser(
    val id: String = "",
    val name: String = "Demo User",
    val email: String = "",
    val role: UserRole = UserRole.Beekeeper,
    val village: String = "Demo village",
    val fcmToken: String = "",
)

data class Hive(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val ownerName: String = "",
    val ownerEmail: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val colonyCount: Int = 0,
    val health: String = "Healthy",
    val honeyKg: Double = 0.0,
    val updatedAt: Long = 0L,
)

data class SprayAlert(
    val id: String = "",
    val cropName: String = "",
    val pesticideName: String = "",
    val sprayTime: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val farmerId: String = "",
    val farmerName: String = "",
    val targetOwnerIds: List<String> = emptyList(),
    val targetFcmTokens: List<String> = emptyList(),
    val status: String = "active",
    val createdAt: Long = 0L,
)

data class HealthLog(
    val id: String = "",
    val hiveId: String = "",
    val ownerId: String = "",
    val activityLevel: String = "Normal",
    val honeyKg: Double = 0.0,
    val notes: String = "",
    val aiSummary: String = "",
    val createdAt: Long = 0L,
)

data class NearbyHiveAlert(
    val hive: Hive,
    val distanceKm: Double,
)

data class ChatMessage(
    val id: String,
    val text: String,
    val fromUser: Boolean,
    val isLoading: Boolean = false,
)

data class DashboardStats(
    val activeHives: Int = 0,
    val alertsToday: Int = 0,
    val honeyKg: Double = 0.0,
    val healthyPercent: Int = 0,
)
