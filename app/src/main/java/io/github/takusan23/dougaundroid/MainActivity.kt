package io.github.takusan23.dougaundroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.takusan23.dougaundroid.ui.screen.HomeScreen
import io.github.takusan23.dougaundroid.ui.theme.DougaUnDroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DougaUnDroidTheme {
                HomeScreen()
            }
        }
    }
}
