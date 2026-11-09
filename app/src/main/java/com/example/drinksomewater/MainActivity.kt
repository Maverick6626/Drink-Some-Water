package com.example.drinksomewater

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.example.drinksomewater.ui.theme.DrinkSomeWaterTheme

class MainActivity : ComponentActivity() {

    private lateinit var repo: WaterRepository
    private var refreshKey by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = WaterRepository(this)

        setContent {
            var themeMode by remember { mutableIntStateOf(repo.getThemeMode()) }
            val dark = when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            // Keep status/navigation bar icons readable in the chosen theme
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT
                    ) { dark },
                    navigationBarStyle = SystemBarStyle.auto(
                        AndroidColor.argb(0xE6, 0xFF, 0xFF, 0xFF),
                        AndroidColor.argb(0x80, 0x1B, 0x1B, 0x1B)
                    ) { dark }
                )
                onDispose { }
            }

            DrinkSomeWaterTheme(darkTheme = dark) {
                AppRoot(
                    repo = repo,
                    refreshKey = refreshKey,
                    themeMode = themeMode,
                    onThemeMode = {
                        themeMode = it
                        repo.setThemeMode(it)
                    },
                    onDataChanged = { WaterWidgetProvider.updateAllWidgets(this) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshKey++ // re-read values after e.g. tapping the widget
    }
}

@Composable
fun AppRoot(
    repo: WaterRepository,
    refreshKey: Int,
    themeMode: Int,
    onThemeMode: (Int) -> Unit,
    onDataChanged: () -> Unit
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("💧" to "Today", "📊" to "History", "⚙️" to "Settings")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { index, (icon, label) ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Text(icon, fontSize = 20.sp) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> TodayScreen(repo, refreshKey, onDataChanged)
                1 -> HistoryScreen(repo, refreshKey)
                else -> SettingsScreen(repo, themeMode, onThemeMode, onDataChanged)
            }
        }
    }
}
