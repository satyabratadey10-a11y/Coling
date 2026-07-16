package com.example.coling

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.coling.ui.theme.ColingTheme
import com.example.coling.ui.screens.MediaScreen
import com.example.coling.ui.screens.EditScreen
import com.example.coling.ui.screens.ColorScreen
import com.example.coling.ui.screens.DeliverScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    object Deliver : Screen("deliver", "Deliver", Icons.Default.Share)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    val navController = rememberNavController()
    val items = listOf(Screen.Media, Screen.Edit, Screen.Color, Screen.Deliver)
    
    val nativeVersionString = try {
        NativeBridge.getNativeVersion()
    } catch (e: Exception) {
        "JNI Not Loaded"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Coling Video Editor") },
                actions = {
                    Text(
                        text = "Native v$nativeVersionString",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(end = 16.dp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
            composable(Screen.Deliver.route) { DeliverScreen() }
        }
    }
}
