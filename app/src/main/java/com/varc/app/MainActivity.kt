package com.varc.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.varc.app.ui.screens.CameraScreen
import com.varc.app.ui.screens.ProfileScreen
import com.varc.app.ui.screens.ResultsScreen
import com.varc.app.ui.theme.VARCTheme

class MainActivity : ComponentActivity() {
    private var hasCameraPermission by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()
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
                                hasPermission = hasCameraPermission,
                                onRequestPermission = { checkAndRequestPermissions() },
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

    private fun checkAndRequestPermissions() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED -> hasCameraPermission = true
            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}
