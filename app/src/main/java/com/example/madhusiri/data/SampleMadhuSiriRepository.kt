package com.example.madhusiri.data

import com.example.madhusiri.model.Hive
import com.example.madhusiri.model.NearbyHiveAlert
import com.example.madhusiri.model.SprayAlert
import com.example.madhusiri.util.calculateDistanceKm

class SampleMadhuSiriRepository {
    val hives = listOf(
        Hive(
            id = "hive-1",
            name = "Mango Grove Hive",
            ownerName = "Siri",
            latitude = 12.9716,
            longitude = 77.5946,
            colonyCount = 8,
            health = "Healthy",
            honeyKg = 18.5,
        ),
        Hive(
            id = "hive-2",
            name = "Sunflower Field Hive",
            ownerName = "Madhu",
            latitude = 12.9822,
            longitude = 77.6021,
            colonyCount = 5,
            health = "Needs check",
            honeyKg = 9.0,
        ),
        Hive(
            id = "hive-3",
            name = "Village Edge Hive",
            ownerName = "Anika",
            latitude = 13.0159,
            longitude = 77.6264,
            colonyCount = 10,
            health = "Strong",
            honeyKg = 22.0,
        ),
    )

    fun nearbyHivesFor(alert: SprayAlert, radiusKm: Double = 2.0): List<NearbyHiveAlert> {
        return hives.map { hive ->
            NearbyHiveAlert(
                hive = hive,
                distanceKm = calculateDistanceKm(
                    lat1 = alert.latitude,
                    lon1 = alert.longitude,
                    lat2 = hive.latitude,
                    lon2 = hive.longitude,
                ),
            )
        }.filter { it.distanceKm <= radiusKm }
            .sortedBy { it.distanceKm }
    }
}
