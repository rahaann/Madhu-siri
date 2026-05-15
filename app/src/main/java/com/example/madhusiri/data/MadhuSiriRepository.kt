package com.example.madhusiri.data

import com.example.madhusiri.model.HealthLog
import com.example.madhusiri.model.Hive
import com.example.madhusiri.model.MadhuUser
import com.example.madhusiri.model.SprayAlert
import com.example.madhusiri.model.UserRole
import com.example.madhusiri.util.calculateDistanceKm
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MadhuSiriRepository {
    private val sample = SampleMadhuSiriRepository()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun userProfile(uid: String): Flow<MadhuUser?> = callbackFlow {
        if (uid.isBlank()) {
            trySend(null)
            awaitClose {}
            return@callbackFlow
        }

        val registration = db.collection(USERS).document(uid)
            .addSnapshotListener { snapshot, _ ->
                val user = snapshot?.data?.let { data ->
                    MadhuUser(
                        id = snapshot.id,
                        name = data["name"] as? String ?: "Madhu-Siri User",
                        email = data["email"] as? String ?: "",
                        role = (data["role"] as? String).toUserRole(),
                        village = data["village"] as? String ?: "",
                        fcmToken = data["fcmToken"] as? String ?: "",
                    )
                }
                trySend(user)
            }

        awaitClose { registration.remove() }
    }

    suspend fun signUp(
        name: String,
        email: String,
        password: String,
        role: UserRole,
    ): Result<Unit> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val uid = result.user?.uid ?: error("Could not create user.")
        val token = fetchFcmToken()
        db.collection(USERS).document(uid).set(
            mapOf(
                "id" to uid,
                "name" to name.ifBlank { "Madhu-Siri User" },
                "email" to email.trim(),
                "role" to role.name,
                "village" to "Demo village",
                "fcmToken" to token,
                "createdAt" to System.currentTimeMillis(),
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        saveFcmToken()
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun saveFcmToken() {
        val uid = auth.currentUser?.uid ?: return
        val token = fetchFcmToken()
        if (token.isNotBlank()) {
            db.collection(USERS).document(uid).set(
                mapOf("fcmToken" to token, "updatedAt" to System.currentTimeMillis()),
                SetOptions.merge(),
            ).await()
        }
    }

    private suspend fun fetchFcmToken(): String {
        return runCatching { FirebaseMessaging.getInstance().token.await() }.getOrDefault("")
    }

    fun hives(): Flow<List<Hive>> = callbackFlow {
        val registration = db.collection(HIVES)
            .addSnapshotListener { snapshot, _ ->
                val hives = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(Hive::class.java)?.copy(id = document.id)
                }.orEmpty()
                trySend(hives.ifEmpty { sample.hives })
            }

        awaitClose { registration.remove() }
    }

    fun sprayAlerts(uid: String): Flow<List<SprayAlert>> = callbackFlow {
        val registration = db.collection(SPRAY_ALERTS)
            .addSnapshotListener { snapshot, _ ->
                val alerts = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(SprayAlert::class.java)?.copy(id = document.id)
                }.orEmpty()
                    .filter { alert ->
                        alert.farmerId == uid || alert.targetOwnerIds.contains(uid)
                    }
                    .sortedByDescending { it.createdAt }
                trySend(alerts)
            }

        awaitClose { registration.remove() }
    }

    fun healthLogs(uid: String): Flow<List<HealthLog>> = callbackFlow {
        val registration = db.collection(HEALTH_LOGS)
            .addSnapshotListener { snapshot, _ ->
                val logs = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(HealthLog::class.java)?.copy(id = document.id)
                }.orEmpty()
                    .filter { it.ownerId == uid }
                    .sortedByDescending { it.createdAt }
                trySend(logs)
            }

        awaitClose { registration.remove() }
    }

    suspend fun upsertHive(hive: Hive, user: MadhuUser) {
        val data = hive.copy(
            ownerId = user.id,
            ownerName = user.name,
            ownerEmail = user.email,
            updatedAt = System.currentTimeMillis(),
        )
        if (hive.id.isBlank() || hive.id.startsWith("local-")) {
            db.collection(HIVES).add(data.copy(id = "")).await()
        } else {
            db.collection(HIVES).document(hive.id).set(data, SetOptions.merge()).await()
        }

        db.collection(HEALTH_LOGS).add(
            HealthLog(
                hiveId = hive.id,
                ownerId = user.id,
                honeyKg = hive.honeyKg,
                notes = "Hive details updated",
                aiSummary = "Monitor activity after nearby spray alerts.",
                createdAt = System.currentTimeMillis(),
            ),
        ).await()
    }

    suspend fun sendSprayAlert(alert: SprayAlert, hives: List<Hive>, farmer: MadhuUser): SprayAlert {
        val nearbyHives = hives.filter { hive ->
            calculateDistanceKm(alert.latitude, alert.longitude, hive.latitude, hive.longitude) <= 2.0
        }
        val nearbyOwnerIds = nearbyHives.map { it.ownerId }.filter { it.isNotBlank() }.distinct()
        val nearbyTokens = nearbyOwnerIds.mapNotNull { ownerId ->
            runCatching {
                Tasks.await(db.collection(USERS).document(ownerId).get()).getString("fcmToken")
            }.getOrNull()
        }.filter { it.isNotBlank() }.distinct()

        val savedAlert = alert.copy(
            farmerId = farmer.id,
            farmerName = farmer.name,
            targetOwnerIds = nearbyOwnerIds,
            targetFcmTokens = nearbyTokens,
            createdAt = System.currentTimeMillis(),
        )
        val document = db.collection(SPRAY_ALERTS).add(savedAlert).await()

        db.collection("notification_requests").add(
            mapOf(
                "type" to "spray_alert",
                "alertId" to document.id,
                "title" to "Spray alert near your hive",
                "body" to "${farmer.name} plans to spray ${alert.pesticideName} on ${alert.cropName} at ${alert.sprayTime}.",
                "targetOwnerIds" to nearbyOwnerIds,
                "targetFcmTokens" to nearbyTokens,
                "createdAt" to System.currentTimeMillis(),
                "status" to "pending",
            ),
        ).await()

        return savedAlert.copy(id = document.id)
    }

    companion object {
        const val USERS = "users"
        const val HIVES = "hives"
        const val SPRAY_ALERTS = "spray_alerts"
        const val HEALTH_LOGS = "health_logs"
    }
}

private fun String?.toUserRole(): UserRole {
    return when {
        this.equals(UserRole.Farmer.name, ignoreCase = true) -> UserRole.Farmer
        else -> UserRole.Beekeeper
    }
}
