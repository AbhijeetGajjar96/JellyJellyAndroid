package com.example.jellyjelly1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.jellyjelly1.ui.theme.JellyJellyTheme
import com.example.jellyjelly1.ui.navigation.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JellyJellyTheme {
                MainScreen()
            }
        }
    }
}