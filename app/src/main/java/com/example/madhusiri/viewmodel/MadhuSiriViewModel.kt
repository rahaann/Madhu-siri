package com.example.madhusiri.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.madhusiri.data.GeminiBeeAdvisor
import com.example.madhusiri.data.MadhuSiriRepository
import com.example.madhusiri.model.ChatMessage
import com.example.madhusiri.model.DashboardStats
import com.example.madhusiri.model.HealthLog
import com.example.madhusiri.model.Hive
import com.example.madhusiri.model.MadhuUser
import com.example.madhusiri.model.NearbyHiveAlert
import com.example.madhusiri.model.SprayAlert
import com.example.madhusiri.model.UserRole
import com.example.madhusiri.util.calculateDistanceKm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MadhuSiriUiState(
    val isAuthenticated: Boolean = false,
    val currentUser: MadhuUser? = null,
    val role: UserRole = UserRole.Beekeeper,
    val hives: List<Hive> = emptyList(),
    val alerts: List<SprayAlert> = emptyList(),
    val healthLogs: List<HealthLog> = emptyList(),
    val stats: DashboardStats = DashboardStats(),
    val weather: String = "28 C",
    val isSyncing: Boolean = true,
    val authLoading: Boolean = false,
    val authError: String? = null,
    val selectedHive: Hive? = null,
    val nearbyAlerts: List<NearbyHiveAlert> = emptyList(),
    val chatMessages: List<ChatMessage> = listOf(
        ChatMessage(
            id = "welcome",
            fromUser = false,
            text = "Ask me about bee-safe spraying, pesticide risk, hive protection, or health symptoms.",
        ),
    ),
)

class MadhuSiriViewModel(
    private val repository: MadhuSiriRepository = MadhuSiriRepository(),
    private val advisor: GeminiBeeAdvisor = GeminiBeeAdvisor(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(MadhuSiriUiState())
    val uiState: StateFlow<MadhuSiriUiState> = _uiState.asStateFlow()

    private var realtimeJob: Job? = null

    init {
        observeAuth()
    }

    private fun observeAuth() {
        viewModelScope.launch {
            repository.authState().collect { firebaseUser ->
                realtimeJob?.cancel()
                if (firebaseUser == null) {
                    _uiState.update {
                        it.copy(
                            isAuthenticated = false,
                            currentUser = null,
                            hives = emptyList(),
                            alerts = emptyList(),
                            healthLogs = emptyList(),
                            isSyncing = false,
                        )
                    }
                } else {
                    _uiState.update { it.copy(isAuthenticated = true, isSyncing = true) }
                    observeUserAndRealtimeData(firebaseUser.uid)
                    viewModelScope.launch { repository.saveFcmToken() }
                }
            }
        }
    }

    private fun observeUserAndRealtimeData(uid: String) {
        realtimeJob = viewModelScope.launch {
            combine(
                repository.userProfile(uid),
                repository.hives(),
                repository.sprayAlerts(uid),
                repository.healthLogs(uid),
            ) { user, hives, alerts, logs ->
                val activeUser = user ?: MadhuUser(id = uid)
                val visibleHives = if (activeUser.role == UserRole.Beekeeper) {
                    hives.filter { it.ownerId == uid || it.ownerId.isBlank() }
                } else {
                    hives
                }
                val healthy = visibleHives.count { it.health.equals("healthy", ignoreCase = true) }
                _uiState.update {
                    it.copy(
                        currentUser = activeUser,
                        role = activeUser.role,
                        hives = visibleHives,
                        alerts = alerts,
                        healthLogs = logs,
                        stats = DashboardStats(
                            activeHives = visibleHives.size,
                            alertsToday = alerts.size,
                            honeyKg = visibleHives.sumOf { hive -> hive.honeyKg },
                            healthyPercent = if (visibleHives.isEmpty()) 0 else healthy * 100 / visibleHives.size,
                        ),
                        isSyncing = false,
                    )
                }
            }.collect {}
        }
    }

    fun signUp(name: String, email: String, password: String, role: UserRole) {
        if (!validateAuth(email, password)) return
        _uiState.update { it.copy(authLoading = true, authError = null, role = role) }
        viewModelScope.launch {
            val result = repository.signUp(name, email, password, role)
            _uiState.update {
                it.copy(
                    authLoading = false,
                    authError = result.exceptionOrNull()?.localizedMessage,
                )
            }
        }
    }

    fun login(email: String, password: String) {
        if (!validateAuth(email, password)) return
        _uiState.update { it.copy(authLoading = true, authError = null) }
        viewModelScope.launch {
            val result = repository.login(email, password)
            _uiState.update {
                it.copy(
                    authLoading = false,
                    authError = result.exceptionOrNull()?.localizedMessage,
                )
            }
        }
    }

    fun logout() {
        repository.logout()
    }

    private fun validateAuth(email: String, password: String): Boolean {
        val error = when {
            email.isBlank() -> "Enter your email address."
            password.length < 6 -> "Password must be at least 6 characters."
            else -> null
        }
        _uiState.update { it.copy(authError = error) }
        return error == null
    }

    fun selectHive(hive: Hive?) {
        _uiState.update { it.copy(selectedHive = hive) }
    }

    fun saveHive(name: String, colonyCount: Int, latitude: Double, longitude: Double) {
        val user = _uiState.value.currentUser ?: return
        val existing = _uiState.value.selectedHive
        val hive = Hive(
            id = existing?.id.orEmpty(),
            ownerId = user.id,
            ownerName = user.name,
            ownerEmail = user.email,
            name = name.ifBlank { "New Hive" },
            colonyCount = colonyCount.coerceAtLeast(1),
            latitude = latitude,
            longitude = longitude,
            health = existing?.health ?: "Healthy",
            honeyKg = existing?.honeyKg ?: 0.0,
        )

        viewModelScope.launch {
            runCatching { repository.upsertHive(hive, user) }
                .onFailure { error -> _uiState.update { it.copy(authError = error.localizedMessage) } }
        }

        _uiState.update { state ->
            val localHive = hive.copy(id = hive.id.ifBlank { "local-${System.currentTimeMillis()}" })
            val updated = if (hive.id.isBlank()) {
                state.hives + localHive
            } else {
                state.hives.map { if (it.id == hive.id) localHive else it }
            }
            state.copy(hives = updated, selectedHive = null)
        }
    }

    fun sendSprayAlert(crop: String, pesticide: String, time: String, latitude: Double, longitude: Double) {
        val user = _uiState.value.currentUser ?: return
        val alert = SprayAlert(
            cropName = crop,
            pesticideName = pesticide,
            sprayTime = time,
            latitude = latitude,
            longitude = longitude,
        )
        val nearby = _uiState.value.hives.map { hive ->
            NearbyHiveAlert(
                hive = hive,
                distanceKm = calculateDistanceKm(latitude, longitude, hive.latitude, hive.longitude),
            )
        }.filter { it.distanceKm <= 2.0 }
            .sortedBy { it.distanceKm }

        viewModelScope.launch {
            runCatching {
                repository.sendSprayAlert(alert, _uiState.value.hives, user)
            }.onSuccess { savedAlert ->
                _uiState.update { it.copy(nearbyAlerts = nearby, alerts = listOf(savedAlert) + it.alerts) }
            }.onFailure { error ->
                _uiState.update { it.copy(authError = error.localizedMessage) }
            }
        }
        _uiState.update { it.copy(nearbyAlerts = nearby) }
    }

    fun sendChatMessage(message: String) {
        if (message.isBlank()) return
        val userMessage = ChatMessage(
            id = "user-${System.currentTimeMillis()}",
            text = message.trim(),
            fromUser = true,
        )
        val loadingMessage = ChatMessage(
            id = "loading-${System.currentTimeMillis()}",
            text = "Thinking through bee-safe guidance...",
            fromUser = false,
            isLoading = true,
        )
        _uiState.update {
            it.copy(chatMessages = it.chatMessages + userMessage + loadingMessage)
        }

        viewModelScope.launch {
            val response = withContext(Dispatchers.IO) {
                advisor.generateTip(
                    cropName = "User question",
                    pesticideName = message,
                    sprayTime = "Not specified",
                ).getOrElse { error ->
                    error.message ?: "AI assistant is unavailable right now."
                }
            }
            _uiState.update { state ->
                state.copy(
                    chatMessages = state.chatMessages
                        .filterNot { it.id == loadingMessage.id }
                        .plus(
                            ChatMessage(
                                id = "ai-${System.currentTimeMillis()}",
                                text = response,
                                fromUser = false,
                            ),
                        ),
                )
            }
        }
    }
}
