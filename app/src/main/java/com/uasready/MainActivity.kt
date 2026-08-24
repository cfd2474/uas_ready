package com.uasready

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.uasready.BuildConfig
import com.uasready.domain.model.Aircraft
import com.uasready.domain.model.PilotAuthorityType
import com.uasready.ui.navigation.Screen
import com.uasready.ui.screens.*
import com.uasready.ui.theme.*
import com.uasready.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val uiState by viewModel.uiState.collectAsState()

            // Global Theme applied with Light, Dark, or System Auto mode
            UASReadyTheme(themeMode = uiState.themeMode) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
                    val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
                    if (fineLocationGranted || coarseLocationGranted) {
                        viewModel.refreshGpsLocation()
                    }
                }

                LaunchedEffect(Unit) {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }

                // 0. App Splash Screen (Dark background, fits any orientation without cutoff, lasts 3 seconds)
                var showSplash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(3000L)
                    showSplash = false
                }

                if (showSplash) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AviationDarkBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.app_logo),
                            contentDescription = "UASReady Splash Logo",
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    }
                } else {
                    // 1. First-Time Setup: Fleet Picker Setup Dialog
                    if (uiState.showFirstTimeFleetSetup) {
                        FirstTimeFleetSetupDialog(
                            allAircraft = uiState.allAircraft,
                            currentSelectedAircraft = uiState.selectedAircraft,
                            onConfirmAircraft = { aircraftId ->
                                viewModel.selectAircraft(aircraftId)
                                viewModel.completeFirstTimeFleetSetup()
                            }
                        )
                    } else if (uiState.isPilotSelectionPending) {
                        // 2. Session Pilot Onboarding Popup (No cutoffs, auto-fitting, returns to Flight Readiness)
                        AlertDialog(
                            onDismissRequest = { /* Modal: require explicit pilot selection */ },
                            containerColor = AviationDarkCard,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .wrapContentHeight(),
                            title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = AviationAccent, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "SELECT PILOT CERTIFICATION",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = TextPrimary
                                    )
                                )
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = "Select operational certification status to initiate safety evaluation:",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontSize = 12.sp)
                                )

                                // Side-by-side selection buttons (Dynamic wrap, no text cutoff)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Button 1: Licensed Pilot
                                    Button(
                                        onClick = {
                                            viewModel.setPilotAuthority(PilotAuthorityType.PART_107)
                                            navController.navigate(Screen.Home.route) {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 84.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AviationDarkSurface),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, AviationAccent)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = AviationAccent, modifier = Modifier.size(22.dp))
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Licensed Pilot",
                                                textAlign = TextAlign.Center,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            )
                                            Text(
                                                text = "14 CFR Part 107",
                                                textAlign = TextAlign.Center,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = AviationAccent,
                                                    fontSize = 9.sp
                                                )
                                            )
                                        }
                                    }

                                    // Button 2: Non-licensed Pilot
                                    Button(
                                        onClick = {
                                            viewModel.setPilotAuthority(PilotAuthorityType.PUBLIC_COA)
                                            navController.navigate(Screen.Home.route) {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 84.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AviationDarkSurface),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, AviationDarkBorder)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Non-licensed/Not permitted for night flight",
                                                textAlign = TextAlign.Center,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            )
                                            Text(
                                                text = "Daylight Window Only",
                                                textAlign = TextAlign.Center,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = TextMuted,
                                                    fontSize = 9.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {}
                    )
                }

                // 2. Main Navigation Drawer with Gestures Disabled (Prevents Map Scrolling Conflicts)
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = false, // Prevents map swiping from accidentally opening the drawer
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = AviationDarkSurface,
                            drawerContentColor = TextPrimary,
                            modifier = Modifier.width(290.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Side Drawer Header (Compact)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text(
                                            text = "UAS READY",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 1.2.sp,
                                                color = TextPrimary
                                            )
                                        )
                                        Text(
                                            text = "FLIGHT READINESS // MENU",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = AviationAccent,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                    IconButton(
                                        onClick = { scope.launch { drawerState.close() } },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close Menu", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                    }
                                }

                                // Active Craft & Pilot Chip (Compact)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AviationDarkCard,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AviationDarkBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 5.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = uiState.selectedAircraft.displayName,
                                            style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = if (uiState.isPilotSelectionPending) "Pending" else uiState.currentPilot.activeAuthority.displayName,
                                            style = MaterialTheme.typography.labelSmall.copy(color = AviationAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            maxLines = 1
                                        )
                                    }
                                }

                                HorizontalDivider(color = AviationDarkBorder, modifier = Modifier.padding(vertical = 2.dp))

                                // Navigation Items (5 Items: Home, Detailed Report, Map, Reference, Settings)
                                val navItems = listOf(
                                    Triple(Screen.Home.route, "Flight Readiness", Icons.Default.Dashboard),
                                    Triple(Screen.Assessment.route, "Detailed Report", Icons.Default.Assessment),
                                    Triple(Screen.Map.route, "Aviation Map (FAA NASR)", Icons.Default.Map),
                                    Triple(Screen.Reference.route, "Checklists & Emergency", Icons.AutoMirrored.Filled.MenuBook),
                                    Triple(Screen.Settings.route, "Settings & Fleet", Icons.Default.Settings)
                                )

                                navItems.forEach { (route, label, icon) ->
                                    val isSelected = currentRoute == route
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) AviationDarkCard else Color.Transparent,
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, AviationAccent.copy(alpha = 0.5f)) else null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                scope.launch { drawerState.close() }
                                                navController.navigate(route) {
                                                    popUpTo(Screen.Home.route) { inclusive = (route == Screen.Home.route) }
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                icon,
                                                contentDescription = label,
                                                tint = if (isSelected) AviationAccent else TextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) AviationAccent else TextPrimary,
                                                    fontSize = 12.sp
                                                )
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = AviationDarkBorder, modifier = Modifier.padding(vertical = 2.dp))

                                // Side Drawer Footer
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "UAS READY // v${BuildConfig.VERSION_NAME}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "RC PRO OPTIMIZED",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AviationAccent,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            // Persistent Standardized Top Status Bar Across All Pages
                            AviationTopStatusBar(
                                currentAircraftName = uiState.selectedAircraft.displayName,
                                currentPilotName = if (uiState.isPilotSelectionPending) "Pending" else uiState.currentPilot.activeAuthority.displayName,
                                isLiveLoading = uiState.isLiveLoading,
                                lastTelemetryUpdateEpochMs = uiState.lastTelemetryUpdateEpochMs,
                                onRefresh = { viewModel.fetchLiveData() },
                                onOpenDrawer = { scope.launch { drawerState.open() } }
                            )
                        },
                        containerColor = AviationDarkBackground
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Home.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Home.route) {
                                HomeScreen(
                                    uiState = uiState,
                                    onNavigateToAssessment = { category ->
                                        viewModel.setCategoryFilter(category)
                                        navController.navigate(Screen.Assessment.route)
                                    },
                                    onNavigateToForecast = {
                                        viewModel.navigateToForecastDetail()
                                        navController.navigate(Screen.Assessment.route)
                                    },
                                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                                    onNavigateToMap = { navController.navigate(Screen.Map.route) }
                                )
                            }

                            composable(Screen.Assessment.route) {
                                AssessmentDetailScreen(
                                    uiState = uiState,
                                    onCategoryFilterSelected = { viewModel.setCategoryFilter(it) },
                                    onClearScrollToForecast = { viewModel.clearScrollToForecast() },
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.Map.route) {
                                MapScreen(
                                    uiState = uiState,
                                    onRefreshGpsLocation = {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                        viewModel.refreshGpsLocation()
                                    },
                                    onDismissAiracWarning = { viewModel.dismissAiracWarning() },
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.Timeline.route) {
                                FlightTimelineScreen(
                                    uiState = uiState,
                                    onUpdateFlightWindow = { start, end -> viewModel.updateFlightWindow(start, end) },
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.Aircraft.route) {
                                AircraftScreen(
                                    uiState = uiState,
                                    onSelectAircraft = { viewModel.selectAircraft(it) },
                                    onSaveCustomAircraft = { viewModel.saveCustomAircraft(it) },
                                    onDeleteCustomAircraft = { viewModel.deleteCustomAircraft(it) },
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.Reference.route) {
                                ReferenceScreen(
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.Settings.route) {
                                SettingsScreen(
                                    uiState = uiState,
                                    onSetAuthority = { viewModel.setPilotAuthority(it) },
                                    onSelectAircraft = { viewModel.selectAircraft(it) },
                                    onSetThemeMode = { viewModel.setThemeMode(it) },
                                    onNavigateToAircraft = { navController.navigate(Screen.Aircraft.route) },
                                    onCheckAiracUpdate = { viewModel.checkForAiracUpdates() },
                                    onPerformAiracUpdate = { viewModel.performAiracUpdate() },
                                    onRebuildNasrDatabase = { viewModel.rebuildNasrDatabase() },
                                    onResetAiracUpdateStatus = { viewModel.resetAiracUpdateStatus() },
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AviationTopStatusBar(
    currentAircraftName: String,
    currentPilotName: String,
    isLiveLoading: Boolean,
    lastTelemetryUpdateEpochMs: Long,
    onRefresh: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    var currentTickerMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lastTelemetryUpdateEpochMs) {
        while (true) {
            currentTickerMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(5000L) // tick every 5s
        }
    }

    val elapsedMs = (currentTickerMs - lastTelemetryUpdateEpochMs).coerceAtLeast(0L)
    val elapsedMinutes = (elapsedMs / (60 * 1000L)).toInt()

    // Status styling: Turns Yellow at >= 5 minutes, Green when < 5 minutes
    val isStale = elapsedMinutes >= 5
    val chipTextColor = if (isStale) SafetyCautionLight else SafetyGoLight
    val chipBgColor = if (isStale) SafetyCautionBg else AviationDarkCard
    val chipBorderColor = if (isStale) SafetyCautionLight.copy(alpha = 0.8f) else AviationDarkBorder

    val timeSinceText = when {
        elapsedMinutes == 0 -> "Last update • 0m"
        elapsedMinutes < 60 -> "Last update • ${elapsedMinutes}m"
        else -> "Last update • ${elapsedMinutes / 60}h"
    }

    // Dynamic Live Status Chip Composable
    val liveStatusChip = @Composable {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = chipBgColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, chipBorderColor),
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable(enabled = !isLiveLoading) { onRefresh() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(chipTextColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = timeSinceText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = chipTextColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }

    TopAppBar(
        title = {
            if (isLandscape) {
                // Landscape Single-Line Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "UASREADY",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            color = TextPrimary
                        )
                    )

                    // Live status chip with elapsed time
                    liveStatusChip()

                    // Active Aircraft Chip
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AviationDarkCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AviationDarkBorder)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.FlightTakeoff,
                                contentDescription = null,
                                tint = AviationAccent,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = currentAircraftName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextPrimary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // Pilot Status Chip
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AviationDarkCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AviationDarkBorder)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Badge,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = currentPilotName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AviationAccent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            } else {
                // Portrait Multi-Line Layout (Prevents crushing)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "UASREADY",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                                color = TextPrimary
                            )
                        )

                        liveStatusChip()

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AviationDarkCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AviationDarkBorder)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.FlightTakeoff,
                                    contentDescription = null,
                                    tint = AviationAccent,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = currentAircraftName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextPrimary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    // Line 2: Pilot Status Chip
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AviationDarkCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AviationDarkBorder)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Badge,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PILOT: $currentPilotName",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AviationAccent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        },
        actions = {
            // Refresh Live Telemetry Button
            IconButton(onClick = onRefresh) {
                if (isLiveLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = AviationAccent, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Live Telemetry", tint = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

            // Upper-Right Menu FAB Button
            IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AviationDarkCard)
                    .border(1.dp, AviationDarkBorder, RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Open Menu", tint = AviationAccent, modifier = Modifier.size(20.dp))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AviationDarkBackground
        )
    )
}

@Composable
fun FirstTimeFleetSetupDialog(
    allAircraft: List<Aircraft>,
    currentSelectedAircraft: Aircraft,
    onConfirmAircraft: (String) -> Unit
) {
    var selectedId by remember { mutableStateOf(currentSelectedAircraft.id) }
    var selectedManufacturer by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val manufacturerList = listOf(
        "All Manufacturers",
        "DJI",
        "Autel Robotics",
        "Skydio",
        "Parrot",
        "Custom Profiles"
    )

    val filteredList = allAircraft.filter { craft ->
        val matchesManufacturer = when (selectedManufacturer) {
            null, "All Manufacturers" -> true
            "Custom Profiles" -> craft.isCustom
            else -> craft.manufacturer.contains(selectedManufacturer ?: "", ignoreCase = true)
        }
        val matchesSearch = searchQuery.isBlank() ||
                craft.displayName.contains(searchQuery, ignoreCase = true) ||
                craft.model.contains(searchQuery, ignoreCase = true)

        matchesManufacturer && matchesSearch
    }

    AlertDialog(
        onDismissRequest = { /* Modal: require explicit aircraft confirmation */ },
        containerColor = AviationDarkCard,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .wrapContentHeight(),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = AviationCyan, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "FIRST-TIME SETUP: SELECT AIRCRAFT",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                    )
                    Text(
                        text = "Step 1 of 2: Configure primary fleet airframe",
                        style = MaterialTheme.typography.bodySmall.copy(color = AviationAccent, fontSize = 10.sp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 210.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Filters Row: Dropdown & Search
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Manufacturer Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = BorderStroke(1.dp, AviationDarkBorder)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedManufacturer ?: "All Manufacturers",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    ),
                                    maxLines = 1
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = AviationAccent, modifier = Modifier.size(16.dp))
                            }
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(AviationDarkSurface)
                        ) {
                            manufacturerList.forEach { mfg ->
                                DropdownMenuItem(
                                    text = { Text(mfg, style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 11.sp)) },
                                    onClick = {
                                        selectedManufacturer = if (mfg == "All Manufacturers") null else mfg
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Search text field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search model...", fontSize = 11.sp, color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TextPrimary),
                        shape = RoundedCornerShape(6.dp),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                        }
                    )
                }

                // Scrollable Aircraft Cards List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredList) { craft ->
                        val isCraftSelected = craft.id == selectedId
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCraftSelected) AviationDarkSurface else AviationDarkCard,
                            border = BorderStroke(
                                if (isCraftSelected) 1.5.dp else 1.dp,
                                if (isCraftSelected) AviationCyan else AviationDarkBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedId = craft.id }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    RadioButton(
                                        selected = isCraftSelected,
                                        onClick = { selectedId = craft.id },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = AviationCyan,
                                            unselectedColor = TextSecondary
                                        ),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = craft.displayName,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = if (isCraftSelected) AviationCyan else TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        )
                                        Text(
                                            text = "${craft.manufacturer} • Wind: ${craft.limitations.maxGustSpeedMph.toInt()} MPH • Temp: ${craft.limitations.minOperatingTempF.toInt()}°F to ${craft.limitations.maxOperatingTempF.toInt()}°F",
                                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmAircraft(selectedId) },
                colors = ButtonDefaults.buttonColors(containerColor = AviationCyan, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACCEPT SELECTION & PROCEED",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    )
}
