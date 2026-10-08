package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.screens.CounterScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SimulatorScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent

enum class NavTab(val title: String, val testTag: String) {
    COUNTER("Counter", "nav_tab_counter"),
    HISTORY("History", "nav_tab_history"),
    SIMULATOR("Simulator", "nav_tab_simulator"),
    SETTINGS("Settings", "nav_tab_settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayVoiceApp(
    viewModel: MainViewModel,
    hasSmsPermission: Boolean,
    onRequestSmsPermission: () -> Unit
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = NavTab.values()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "PayVoice",
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = androidx.compose.ui.unit.Dp(6f)
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = selectedTabIndex == index
                    val icon = when (tab) {
                        NavTab.COUNTER -> Icons.Default.Store
                        NavTab.HISTORY -> Icons.Default.History
                        NavTab.SIMULATOR -> Icons.Default.PhoneAndroid
                        NavTab.SETTINGS -> Icons.Default.Settings
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldPrimary.copy(alpha = 0.2f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (tabs[selectedTabIndex]) {
                NavTab.COUNTER -> CounterScreen(
                    viewModel = viewModel,
                    hasSmsPermission = hasSmsPermission,
                    onRequestSmsPermission = onRequestSmsPermission
                )
                NavTab.HISTORY -> HistoryScreen(
                    viewModel = viewModel
                )
                NavTab.SIMULATOR -> SimulatorScreen(
                    viewModel = viewModel
                )
                NavTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
