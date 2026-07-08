package com.varc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.varc.app.ui.screens.CameraScreen
import com.varc.app.ui.screens.ProfileScreen
import com.varc.app.ui.screens.ResultsScreen
import com.varc.app.ui.theme.VARCTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VARCTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "camera") {
                        composable("camera") {
                            CameraScreen(
                                hasPermission = true,
                                onRequestPermission = {},
                                onAnalysisComplete = { videoPath ->
                                    navController.navigate("results/$videoPath")
                                },
                                onNavigateToProfile = {
                                    navController.navigate("profile")
                                }
                            )
                        }
                        composable("results/{videoPath}") { backStackEntry ->
                            val videoPath = backStackEntry.arguments?.getString("videoPath") ?: ""
                            ResultsScreen(
                                videoPath = videoPath,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("profile") {
                            ProfileScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
