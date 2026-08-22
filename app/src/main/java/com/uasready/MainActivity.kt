package com.uasready

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.uasready.BuildConfig
import com.uasready.ui.navigation.Screen
import com.uasready.ui.screens.*
import com.uasready.ui.theme.*
import com.uasready.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UASReadyTheme {
                val uiState by viewModel.uiState.collectAsState()
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

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

                Scaffold(
                    bottomBar = {
                        Column {
                            NavigationBar(
                                containerColor = AviationDarkSurface,
                                contentColor = TextPrimary
                            ) {
                                NavigationBarItem(
                                    selected = currentRoute == Screen.Home.route,
                                    onClick = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } },
                                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Ready") },
                                    label = { Text("Ready") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AviationAccent,
                                        selectedTextColor = AviationAccent,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary,
                                        indicatorColor = AviationDarkCard
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentRoute == Screen.Assessment.route,
                                    onClick = { navController.navigate(Screen.Assessment.route) },
                                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Audit") },
                                    label = { Text("Audit") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AviationAccent,
                                        selectedTextColor = AviationAccent,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary,
                                        indicatorColor = AviationDarkCard
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentRoute == Screen.Map.route,
                                    onClick = { navController.navigate(Screen.Map.route) },
                                    icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                                    label = { Text("Map") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AviationAccent,
                                        selectedTextColor = AviationAccent,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary,
                                        indicatorColor = AviationDarkCard
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentRoute == Screen.Reference.route,
                                    onClick = { navController.navigate(Screen.Reference.route) },
                                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Checklists") },
                                    label = { Text("Checklists") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AviationAccent,
                                        selectedTextColor = AviationAccent,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary,
                                        indicatorColor = AviationDarkCard
                                    )
                                )
                            }
                            // Persistent High-Contrast App Footer with Version
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = AviationDarkBackground,
                                tonalElevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "UAS READY // FLIGHT READINESS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AviationAccent,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
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
                                onNavigateToAssessment = { navController.navigate(Screen.Assessment.route) },
                                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                                onNavigateToMap = { navController.navigate(Screen.Map.route) },
                                onRefreshLiveData = { viewModel.fetchLiveData() }
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
                                onLocationChanged = { viewModel.updateLocation(it) },
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
