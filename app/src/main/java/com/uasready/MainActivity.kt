package com.uasready

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
                    // 1. Session Pilot Onboarding Popup (No cutoffs, auto-fitting, returns to Flight Readiness)
                    if (uiState.isPilotSelectionPending) {
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
                                                text = "Non-licensed Pilot",
                                                textAlign = TextAlign.Center,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
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
                                    Triple(Screen.Map.route, "Aviation Map (openAIP)", Icons.Default.Map),
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
                                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                                    onNavigateToMap = { navController.navigate(Screen.Map.route) }
                                )
                            }

                            composable(Screen.Assessment.route) {
                                AssessmentDetailScreen(
                                    uiState = uiState,
                                    onCategoryFilterSelected = { viewModel.setCategoryFilter(it) },
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
    onRefresh: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

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

                    // Live status chip
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AviationDarkCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AviationDarkBorder)
                    ) {
                        Text(
                            text = "LIVE",
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SafetyGoLight,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

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

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AviationDarkCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AviationDarkBorder)
                        ) {
                            Text(
                                text = "LIVE",
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SafetyGoLight,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

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
