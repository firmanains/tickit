package io.github.firmanains.tickit.presentation

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.firmanains.tickit.container
import io.github.firmanains.tickit.presentation.habit.HabitDetailScreen
import io.github.firmanains.tickit.presentation.home.HomeScreen
import io.github.firmanains.tickit.presentation.shame.ShameScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        val c = container
        setContent {
            TickitTheme {
                // Navigation as state: two tabs plus one detail screen on top. A nav library would be more than this needs.
                var tab by rememberSaveable { mutableIntStateOf(0) }
                var openHabit by rememberSaveable { mutableStateOf<Long?>(null) }
                BackHandler(enabled = openHabit != null) { openHabit = null }

                val habitId = openHabit
                if (habitId != null) {
                    HabitDetailScreen(viewModel(key = "habit-$habitId") { c.habitDetailViewModel(habitId) }, onBack = { openHabit = null })
                } else Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = tab == 0, onClick = { tab = 0 },
                                icon = { Icon(Icons.Default.DateRange, null) }, label = { Text("Hari ini") },
                            )
                            NavigationBarItem(
                                selected = tab == 1, onClick = { tab = 1 },
                                icon = { Icon(Icons.Default.Warning, null) }, label = { Text("Wall of Shame") },
                            )
                        }
                    },
                ) { padding ->
                    Box(Modifier.padding(padding)) {
                        if (tab == 0) HomeScreen(viewModel { c.homeViewModel() }, onOpenHabit = { openHabit = it })
                        else ShameScreen(viewModel { c.shameViewModel() })
                    }
                }
            }
        }
    }
}
