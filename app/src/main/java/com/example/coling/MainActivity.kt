package com.example.coling

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.coling.ui.theme.ColingTheme
import com.example.coling.ui.screens.MediaScreen
import com.example.coling.ui.screens.EditScreen
import com.example.coling.ui.screens.ColorScreen
import com.example.coling.ui.screens.ExportSheetContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            ColingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppScreen()
                }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Media : Screen("media", "Media", Icons.Default.List)
    object Edit : Screen("edit", "Edit", Icons.Default.PlayArrow)
    object Color : Screen("color", "Color", Icons.Default.Build)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    val navController = rememberNavController()
    val items = listOf(Screen.Media, Screen.Edit, Screen.Color)

    var showExportSheet by remember { mutableStateOf(false) }
    var isExportRunning by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Coling",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                    // Export progress chip when running in background
                    if (isExportRunning && !showExportSheet) {
                        AssistChip(
                            onClick = { showExportSheet = true },
                            label = { Text("Exporting\u2026", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    // Export action button
                    IconButton(onClick = { showExportSheet = true }) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Export project",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Media.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Media.route) { MediaScreen() }
            composable(Screen.Edit.route) { EditScreen() }
            composable(Screen.Color.route) { ColorScreen() }
        }
    }

    // Export modal bottom sheet
    if (showExportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExportSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            ExportSheetContent(
                onDismiss = { showExportSheet = false },
                onExportStarted = { running -> isExportRunning = running }
            )
        }
    }
}
