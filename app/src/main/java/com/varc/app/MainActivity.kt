package com.varc.app

import android.net.Uri
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
                                    val encoded = Uri.encode(videoPath)
                                    navController.navigate("results/$encoded")
                                },
                                onNavigateToProfile = {
                                    navController.navigate("profile")
                                }
                            )
                        }
                        composable("results/{videoPath}") { backStackEntry ->
                            val rawPath = backStackEntry.arguments?.getString("videoPath") ?: ""
                            val videoPath = Uri.decode(rawPath)
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
