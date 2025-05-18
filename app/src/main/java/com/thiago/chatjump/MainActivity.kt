package com.thiago.chatjump

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.thiago.chatjump.navigation.ChatNavigation
import com.thiago.chatjump.ui.theme.ChatJumpTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatJumpTheme {
                val backgroundColor = MaterialTheme.colorScheme.background
                // Update system bar colors to match theme
                SideEffect {
                    window.statusBarColor = backgroundColor.toArgb()
                    val isLight = backgroundColor.luminance() > 0.5f
                    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isLight
                }

                Scaffold( modifier = Modifier.fillMaxSize() ) { innerPadding ->
                    Surface(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        ChatNavigation(navController = navController)
                    }
                }
            }
        }
    }
}