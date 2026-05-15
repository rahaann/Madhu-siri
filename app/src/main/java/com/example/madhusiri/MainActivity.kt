package com.example.madhusiri

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Hive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.madhusiri.model.Hive
import com.example.madhusiri.model.NearbyHiveAlert
import com.example.madhusiri.model.UserRole
import com.example.madhusiri.ui.theme.FieldCream
import com.example.madhusiri.ui.theme.HoneyGold
import com.example.madhusiri.ui.theme.LeafGreen
import com.example.madhusiri.ui.theme.MadhuSiriTheme
import com.example.madhusiri.viewmodel.MadhuSiriUiState
import com.example.madhusiri.viewmodel.MadhuSiriViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay

private enum class Screen(
    val label: String,
    val icon: ImageVector,
) {
    Dashboard("Home", Icons.Filled.Home),
    HiveMap("Map", Icons.Filled.LocationOn),
    SprayAlert("Alert", Icons.Filled.Warning),
    Assistant("AI", Icons.AutoMirrored.Filled.Chat),
    Health("Health", Icons.Filled.HealthAndSafety),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MadhuSiriTheme {
                MadhuSiriRoot()
            }
        }
    }
}

@Composable
private fun MadhuSiriRoot() {
    var showSplash by rememberSaveable { mutableStateOf(true) }

    AnimatedContent(
        targetState = showSplash,
        label = "splash transition",
    ) { visible ->
        if (visible) {
            SplashScreen(onFinished = { showSplash = false })
        } else {
            MadhuSiriApp()
        }
    }
}

@Composable
private fun SplashScreen(onFinished: () -> Unit) {
    var animateIn by remember { mutableStateOf(false) }
    val logoAlpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "logo alpha",
    )
    val logoScale by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0.86f,
        animationSpec = tween(durationMillis = 900),
        label = "logo scale",
    )
    val floating = rememberInfiniteTransition(label = "splash float")
    val beeOffset by floating.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "bee float",
    )
    val honeyPulse by floating.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "honeycomb pulse",
    )

    LaunchedEffect(Unit) {
        animateIn = true
        delay(2500)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        SplashHoneycombBackground(alpha = honeyPulse)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .graphicsLayer {
                    alpha = logoAlpha
                    scaleX = logoScale
                    scaleY = logoScale
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center) {
                MadhuSiriLogo(modifier = Modifier.size(148.dp))
                FloatingBee(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 2.dp)
                        .graphicsLayer { translationY = beeOffset },
                )
            }
            Spacer(Modifier.height(26.dp))
            Text(
                "Madhu-Siri",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Bee Farmer Harmony",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun SplashHoneycombBackground(alpha: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = 24f
        val stepX = radius * 1.72f
        val stepY = radius * 1.5f
        for (row in 0..8) {
            for (col in 0..5) {
                val cx = col * stepX + if (row % 2 == 0) 18f else 42f
                val cy = row * stepY + 72f
                val path = hexagonPath(Offset(cx, cy), radius)
                drawPath(
                    path = path,
                    color = HoneyGold.copy(alpha = alpha * 0.35f),
                    style = Stroke(width = 2.2f),
                )
            }
        }
    }
}

@Composable
private fun MadhuSiriLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = HoneyGold,
            radius = size.minDimension * 0.44f,
            center = center,
        )
        drawCircle(
            color = LeafGreen.copy(alpha = 0.16f),
            radius = size.minDimension * 0.36f,
            center = center,
        )
        drawPath(
            path = hexagonPath(center, size.minDimension * 0.28f),
            color = Color.White.copy(alpha = 0.82f),
            style = Stroke(width = 5f),
        )

        val leaf = Path().apply {
            moveTo(center.x + 8f, center.y + 12f)
            cubicTo(center.x + 46f, center.y - 6f, center.x + 48f, center.y - 42f, center.x + 10f, center.y - 30f)
            cubicTo(center.x + 22f, center.y - 16f, center.x + 20f, center.y - 2f, center.x + 8f, center.y + 12f)
        }
        drawPath(leaf, LeafGreen)
        drawLine(
            color = Color.White.copy(alpha = 0.76f),
            start = Offset(center.x + 11f, center.y + 5f),
            end = Offset(center.x + 32f, center.y - 22f),
            strokeWidth = 3f,
        )

        drawCircle(Color(0xFF2B1D00), radius = 19f, center = Offset(center.x - 18f, center.y + 8f))
        drawCircle(HoneyGold, radius = 12f, center = Offset(center.x - 18f, center.y + 8f))
        drawCircle(Color(0xFF2B1D00), radius = 3f, center = Offset(center.x - 23f, center.y + 5f))
        drawCircle(Color.White.copy(alpha = 0.64f), radius = 9f, center = Offset(center.x - 36f, center.y - 11f))
        drawCircle(Color.White.copy(alpha = 0.64f), radius = 9f, center = Offset(center.x - 6f, center.y - 11f))
    }
}

@Composable
private fun FloatingBee(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(42.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(Color.White.copy(alpha = 0.7f), radius = 9f, center = Offset(center.x - 8f, center.y - 9f))
        drawCircle(Color.White.copy(alpha = 0.7f), radius = 9f, center = Offset(center.x + 8f, center.y - 9f))
        drawCircle(Color(0xFF2B1D00), radius = 11f, center = center)
        drawCircle(HoneyGold, radius = 7f, center = center)
        drawLine(Color(0xFF2B1D00), Offset(center.x - 2f, center.y - 7f), Offset(center.x - 2f, center.y + 7f), 3f)
        drawLine(Color(0xFF2B1D00), Offset(center.x + 5f, center.y - 6f), Offset(center.x + 5f, center.y + 6f), 3f)
    }
}

private fun hexagonPath(center: Offset, radius: Float): Path {
    return Path().apply {
        for (i in 0..5) {
            val angle = Math.toRadians((60.0 * i) - 30.0)
            val x = center.x + radius * kotlin.math.cos(angle).toFloat()
            val y = center.y + radius * kotlin.math.sin(angle).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

@Composable
private fun MadhuSiriApp(viewModel: MadhuSiriViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    MadhuSiriAppContent(
        uiState = uiState,
        onLogin = viewModel::login,
        onSignUp = viewModel::signUp,
        onLogout = viewModel::logout,
        onSelectHive = viewModel::selectHive,
        onSaveHive = viewModel::saveHive,
        onSendSprayAlert = viewModel::sendSprayAlert,
        onSendChatMessage = viewModel::sendChatMessage,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MadhuSiriAppContent(
    uiState: MadhuSiriUiState,
    onLogin: (String, String) -> Unit,
    onSignUp: (String, String, String, UserRole) -> Unit,
    onLogout: () -> Unit,
    onSelectHive: (Hive?) -> Unit,
    onSaveHive: (String, Int, Double, Double) -> Unit,
    onSendSprayAlert: (String, String, String, Double, Double) -> Unit,
    onSendChatMessage: (String) -> Unit,
) {
    var screen by rememberSaveable { mutableStateOf(Screen.Dashboard) }
    var showHiveDialog by rememberSaveable { mutableStateOf(false) }

    if (!uiState.isAuthenticated) {
        AuthScreen(
            uiState = uiState,
            onLogin = onLogin,
            onSignUp = onSignUp,
        )
        return
    }
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}
    LaunchedEffect(uiState.isAuthenticated) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            MadhuTopBar(
                weather = uiState.weather,
                notificationCount = uiState.alerts.size,
                role = uiState.role,
                userName = uiState.currentUser?.name ?: "Madhu-Siri User",
                onLogout = onLogout,
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Screen.entries.forEach { item ->
                    NavigationBarItem(
                        selected = screen == item,
                        onClick = { screen = item },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(visible = screen == Screen.HiveMap && uiState.role == UserRole.Beekeeper) {
                FloatingActionButton(
                    onClick = {
                        onSelectHive(null)
                        showHiveDialog = true
                    },
                    containerColor = HoneyGold,
                    contentColor = Color(0xFF2C2100),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add hive")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        AnimatedContent(
            targetState = screen,
            label = "screen transition",
            modifier = Modifier.padding(padding),
        ) { current ->
            when (current) {
                Screen.Dashboard -> DashboardScreen(uiState)
                Screen.HiveMap -> HiveMapScreen(
                    uiState = uiState,
                    onHiveSelected = {
                        if (uiState.role == UserRole.Beekeeper && it.ownerId == uiState.currentUser?.id) {
                            onSelectHive(it)
                            showHiveDialog = true
                        }
                    },
                )

                Screen.SprayAlert -> if (uiState.role == UserRole.Farmer) {
                    SprayAlertScreen(uiState.nearbyAlerts, onSendSprayAlert)
                } else {
                    RoleGateScreen()
                }
                Screen.Assistant -> AiAssistantScreen(uiState, onSendChatMessage)
                Screen.Health -> HealthScreen(uiState)
            }
        }
    }

    if (showHiveDialog) {
        HiveEditorDialog(
            hive = uiState.selectedHive,
            onDismiss = { showHiveDialog = false },
            onSave = { name, colonies, lat, lng ->
                onSaveHive(name, colonies, lat, lng)
                showHiveDialog = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MadhuTopBar(
    weather: String,
    notificationCount: Int,
    role: UserRole,
    userName: String,
    onLogout: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Madhu-Siri", fontWeight = FontWeight.ExtraBold)
                Text(
                    "$userName • ${role.name}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = {}) {
                Icon(Icons.Filled.Person, contentDescription = "Profile")
            }
        },
        actions = {
            AssistChip(
                onClick = {},
                leadingIcon = { Icon(Icons.Filled.Cloud, contentDescription = null, Modifier.size(16.dp)) },
                label = { Text(weather) },
            )
            Box {
                IconButton(onClick = {}) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                }
                if (notificationCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error),
                    )
                }
            }
            TextButton(onClick = onLogout) {
                Text("Logout")
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        ),
    )
}

@Composable
private fun AuthScreen(
    uiState: MadhuSiriUiState,
    onLogin: (String, String) -> Unit,
    onSignUp: (String, String, String, UserRole) -> Unit,
) {
    var isSignup by rememberSaveable { mutableStateOf(true) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf(UserRole.Beekeeper) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.86f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        AppCard(container = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                MadhuSiriLogo(modifier = Modifier.size(74.dp))
                Column {
                    Text("Madhu-Siri", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text("Bee Farmer Harmony", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                if (isSignup) "Create your farming network account" else "Welcome back",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (isSignup) {
                AppTextField("Full name", name, { name = it })
            }
            AppTextField("Email", email, { email = it }, keyboardType = KeyboardType.Email)
            AppTextField(
                label = "Password",
                value = password,
                onValueChange = { password = it },
                keyboardType = KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation(),
            )
            if (isSignup) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RoleButton("Beekeeper", role == UserRole.Beekeeper, Modifier.weight(1f)) {
                        role = UserRole.Beekeeper
                    }
                    RoleButton("Farmer", role == UserRole.Farmer, Modifier.weight(1f)) {
                        role = UserRole.Farmer
                    }
                }
            }
            uiState.authError?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !uiState.authLoading,
                colors = ButtonDefaults.buttonColors(containerColor = LeafGreen),
                onClick = {
                    if (isSignup) {
                        onSignUp(name, email, password, role)
                    } else {
                        onLogin(email, password)
                    }
                },
            ) {
                if (uiState.authLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (isSignup) "Create Account" else "Login")
                }
            }
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { isSignup = !isSignup },
            ) {
                Text(if (isSignup) "Already have an account? Login" else "New here? Create an account")
            }
        }
    }
}

@Composable
private fun DashboardScreen(uiState: MadhuSiriUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HeroPanel(uiState)
        }
        item {
            if (uiState.isSyncing) {
                ShimmerDashboard()
            } else {
                AnalyticsGrid(uiState)
            }
        }
        item {
            HoneyProductionChart(uiState.hives)
        }
        item {
            AiHiveInsight(uiState)
        }
    }
}

@Composable
private fun HeroPanel(uiState: MadhuSiriUiState) {
    AppCard(container = MaterialTheme.colorScheme.primaryContainer) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Protect pollinators before spraying begins",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    "Real-time hive pins, 2 km safety radius, spray alerts, and AI guidance for bee-friendly farming.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                )
            }
            Icon(
                Icons.Outlined.Hive,
                contentDescription = null,
                tint = HoneyGold,
                modifier = Modifier.size(54.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AssistChip(onClick = {}, label = { Text(uiState.role.name) })
            AssistChip(onClick = {}, label = { Text(uiState.currentUser?.email ?: "Firebase synced") })
        }
    }
}

@Composable
private fun RoleButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(
        modifier = modifier.height(48.dp),
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) HoneyGold else MaterialTheme.colorScheme.surface,
            contentColor = if (selected) Color(0xFF251A00) else MaterialTheme.colorScheme.primary,
        ),
    ) {
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AnalyticsGrid(uiState: MadhuSiriUiState) {
    val wide = LocalConfiguration.current.screenWidthDp > 520
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("Active hives", uiState.stats.activeHives.toString(), Icons.Outlined.Hive, Modifier.weight(1f))
            MetricCard("Alerts", uiState.stats.alertsToday.toString(), Icons.Filled.Radar, Modifier.weight(1f))
            if (wide) {
                MetricCard("Honey", "${uiState.stats.honeyKg.toInt()} kg", Icons.Filled.Analytics, Modifier.weight(1f))
                MetricCard("Healthy", "${uiState.stats.healthyPercent}%", Icons.Filled.HealthAndSafety, Modifier.weight(1f))
            }
        }
        if (!wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Honey", "${uiState.stats.honeyKg.toInt()} kg", Icons.Filled.Analytics, Modifier.weight(1f))
                MetricCard("Healthy", "${uiState.stats.healthyPercent}%", Icons.Filled.HealthAndSafety, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(targetValue = 1f, animationSpec = tween(450), label = "metric")
    AppCard(modifier = modifier, container = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Column {
                Text(value, fontSize = (24 * scale).sp, fontWeight = FontWeight.ExtraBold)
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ShimmerDashboard() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ShimmerCard(Modifier.weight(1f))
            ShimmerCard(Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ShimmerCard(Modifier.weight(1f))
            ShimmerCard(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ShimmerCard(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Restart),
        label = "shimmer offset",
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
        start = Offset(offset - 220f, 0f),
        end = Offset(offset, 220f),
    )
    Box(
        modifier = modifier
            .height(92.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(brush),
    )
}

@Composable
private fun HoneyProductionChart(hives: List<Hive>) {
    AppCard {
        Text("Honey production", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(top = 10.dp),
        ) {
            val values = hives.map { it.honeyKg.toFloat().coerceAtLeast(1f) }.ifEmpty { listOf(1f) }
            val max = values.maxOrNull() ?: 1f
            val barWidth = size.width / (values.size * 2f)
            values.forEachIndexed { index, value ->
                val barHeight = size.height * (value / max)
                val left = index * barWidth * 2 + barWidth * 0.45f
                drawRoundRect(
                    color = if (index % 2 == 0) HoneyGold else LeafGreen,
                    topLeft = Offset(left, size.height - barHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
                )
            }
        }
    }
}

@Composable
private fun AiHiveInsight(uiState: MadhuSiriUiState) {
    val weakHives = uiState.hives.filterNot { it.health.equals("healthy", ignoreCase = true) }
    AppCard(container = MaterialTheme.colorScheme.tertiaryContainer) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.HealthAndSafety, contentDescription = null)
            Text("AI hive health insight", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Text(
            if (weakHives.isEmpty()) {
                "All tracked hives look stable. Keep monitoring activity after nearby spray alerts."
            } else {
                "${weakHives.size} hive needs attention. Review recent spray alerts, bee activity, and honey drop patterns."
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun HiveMapScreen(uiState: MadhuSiriUiState, onHiveSelected: (Hive) -> Unit) {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasLocationPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val firstHive = uiState.hives.firstOrNull()
    val center = LatLng(firstHive?.latitude ?: 12.9716, firstHive?.longitude ?: 77.5946)
    val livePosition = LatLng(center.latitude + 0.002, center.longitude + 0.001)
    val pulse = rememberInfiniteTransition(label = "radius pulse").animateFloat(
        initialValue = 0.12f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "radius alpha",
    )
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, 13.5f)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionHeader("Hive Map", "Live location, editable hive pins, and visible 2 km safety radius.")
        }
        item {
            Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.height(380.dp)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                    uiSettings = MapUiSettings(myLocationButtonEnabled = true, mapToolbarEnabled = true),
                ) {
                    Circle(
                        center = center,
                        radius = 2000.0,
                        strokeColor = LeafGreen,
                        fillColor = HoneyGold.copy(alpha = pulse.value),
                        strokeWidth = 4f,
                    )
                    Marker(
                        state = MarkerState(livePosition),
                        title = "Current location",
                        snippet = "Use this for new hive placement",
                    )
                    uiState.hives.forEach { hive ->
                        Marker(
                            state = MarkerState(LatLng(hive.latitude, hive.longitude)),
                            title = hive.name,
                            snippet = "Tap to edit • ${hive.colonyCount} colonies",
                            onClick = {
                                onHiveSelected(hive)
                                true
                            },
                        )
                    }
                }
            }
        }
        items(uiState.hives, key = { it.id }) { hive ->
            HiveRow(
                hive = hive,
                canEdit = uiState.role == UserRole.Beekeeper && hive.ownerId == uiState.currentUser?.id,
                onClick = { onHiveSelected(hive) },
            )
        }
    }
}

@Composable
private fun HiveRow(hive: Hive, canEdit: Boolean, onClick: () -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Hive, contentDescription = null, tint = HoneyGold, modifier = Modifier.size(34.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(hive.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${hive.colonyCount} colonies • ${hive.health} • ${"%.4f".format(hive.latitude)}, ${"%.4f".format(hive.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (canEdit) {
                TextButton(onClick = onClick) {
                    Text("Edit")
                }
            }
        }
    }
}

@Composable
private fun RoleGateScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        AppCard(container = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = HoneyGold, modifier = Modifier.size(44.dp))
            Text("Role restricted", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Spray alerts are created from Farmer accounts.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HiveEditorDialog(
    hive: Hive?,
    onDismiss: () -> Unit,
    onSave: (String, Int, Double, Double) -> Unit,
) {
    var name by rememberSaveable(hive?.id) { mutableStateOf(hive?.name ?: "New Apiary Hive") }
    var colonies by rememberSaveable(hive?.id) { mutableStateOf((hive?.colonyCount ?: 4).toString()) }
    var latitude by rememberSaveable(hive?.id) { mutableStateOf((hive?.latitude ?: 12.9716).toString()) }
    var longitude by rememberSaveable(hive?.id) { mutableStateOf((hive?.longitude ?: 77.5946).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hive == null) "Add hive pin" else "Edit hive pin") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppTextField("Hive name", name, { name = it })
                AppTextField("Colonies", colonies, { colonies = it }, keyboardType = KeyboardType.Number)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppTextField("Latitude", latitude, { latitude = it }, Modifier.weight(1f), KeyboardType.Decimal)
                    AppTextField("Longitude", longitude, { longitude = it }, Modifier.weight(1f), KeyboardType.Decimal)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name,
                        colonies.toIntOrNull() ?: 1,
                        latitude.toDoubleOrNull() ?: 12.9716,
                        longitude.toDoubleOrNull() ?: 77.5946,
                    )
                },
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SprayAlertScreen(
    nearby: List<NearbyHiveAlert>,
    onSendAlert: (String, String, String, Double, Double) -> Unit,
) {
    var crop by rememberSaveable { mutableStateOf("Tomato") }
    var pesticide by rememberSaveable { mutableStateOf("Neem oil") }
    var time by rememberSaveable { mutableStateOf("5:30 PM") }
    var lat by rememberSaveable { mutableStateOf("12.9720") }
    var lng by rememberSaveable { mutableStateOf("77.5950") }
    var sent by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionHeader("Spray Alert", "Notify beekeepers within 2 km before spraying.")
        }
        item {
            AppCard {
                AppTextField("Crop", crop, { crop = it })
                AppTextField("Pesticide", pesticide, { pesticide = it })
                AppTextField("Spray time", time, { time = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppTextField("Latitude", lat, { lat = it }, Modifier.weight(1f), KeyboardType.Decimal)
                    AppTextField("Longitude", lng, { lng = it }, Modifier.weight(1f), KeyboardType.Decimal)
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = LeafGreen),
                    onClick = {
                        onSendAlert(crop, pesticide, time, lat.toDoubleOrNull() ?: 0.0, lng.toDoubleOrNull() ?: 0.0)
                        sent = true
                    },
                ) {
                    Icon(Icons.Filled.Radar, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Send Spray Alert")
                }
            }
        }
        if (sent) {
            item {
                Text("Nearby beekeepers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (nearby.isEmpty()) {
                item { EmptyState() }
            } else {
                items(nearby) {
                    AppCard {
                        Text("${it.hive.name} notified", fontWeight = FontWeight.Bold)
                        Text("${"%.2f".format(it.distanceKm)} km away • Owner ${it.hive.ownerName}")
                    }
                }
            }
        }
    }
}

@Composable
private fun AiAssistantScreen(uiState: MadhuSiriUiState, onSend: (String) -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader("AI Bee Advisor", "Chat with Gemini about pesticide safety and hive health.")
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(uiState.chatMessages, key = { it.id }) { message ->
                ChatBubble(
                    text = message.text,
                    fromUser = message.fromUser,
                    loading = message.isLoading,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AppTextField(
                label = "Ask about safe spraying",
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    onSend(input)
                    input = ""
                },
                enabled = input.isNotBlank(),
            ) {
                Text("Send")
            }
        }
    }
}

@Composable
private fun ChatBubble(text: String, fromUser: Boolean, loading: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .animateContentSize(),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (fromUser) 18.dp else 4.dp,
                bottomEnd = if (fromUser) 4.dp else 18.dp,
            ),
            color = if (fromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (fromUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        ) {
            if (loading) {
                ShimmerCard(Modifier.padding(12.dp))
            } else {
                Text(text, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun HealthScreen(uiState: MadhuSiriUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionHeader("Hive Health", "Production, colony status, and AI risk signals.")
        }
        item {
            AiHiveInsight(uiState)
        }
        items(uiState.hives) { hive ->
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (hive.health == "Healthy") LeafGreen else HoneyGold),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.HealthAndSafety, contentDescription = null, tint = FieldCream)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(hive.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("${hive.health} • ${hive.honeyKg} kg honey • ${hive.colonyCount} colonies")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyState() {
    AppCard(container = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Text("No hives were found inside the 2 km radius.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AppCard(
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun AppTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        modifier = modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(8.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun AppPreview() {
    MadhuSiriTheme(dynamicColor = false) {
        MadhuSiriAppContent(
            uiState = MadhuSiriUiState(isAuthenticated = true),
            onLogin = { _, _ -> },
            onSignUp = { _, _, _, _ -> },
            onLogout = {},
            onSelectHive = {},
            onSaveHive = { _, _, _, _ -> },
            onSendSprayAlert = { _, _, _, _, _ -> },
            onSendChatMessage = {},
        )
    }
}
