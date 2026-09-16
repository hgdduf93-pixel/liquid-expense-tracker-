package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.tabs.*

sealed class Tab(val icon: ImageVector, val title: String) {
    object Home : Tab(Icons.Filled.Home, "Home")
    object Analytics : Tab(Icons.Filled.BarChart, "Analytics")
    object Transactions : Tab(Icons.Filled.SwapHoriz, "Transactions")
    object Settings : Tab(Icons.Filled.Settings, "Settings")
}

@Composable
fun MainScreen() {
    var activeTab by remember { mutableStateOf<Tab>(Tab.Home) }
    val tabs = remember { listOf(Tab.Home, Tab.Analytics, Tab.Transactions, Tab.Settings) }

    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0C0E17),
                Color(0xFF05060A),
                Color(0xFF000000)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Main content area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        (scaleIn(initialScale = 0.95f, animationSpec = tween(200, easing = FastOutSlowInEasing)) + fadeIn(tween(200))) togetherWith
                        (scaleOut(targetScale = 0.98f, animationSpec = tween(150)) + fadeOut(tween(150)))
                    },
                    label = "tabTransition"
                ) { target ->
                    when (target) {
                        Tab.Home -> DashboardTab(onNavigateToTransactions = { activeTab = Tab.Transactions })
                        Tab.Analytics -> AnalyticsTab()
                        Tab.Transactions -> TransactionsTab()
                        Tab.Settings -> SettingsTab()
                    }
                }
            }

            // Bottom Navigation Bar with proper system navigation bars padding
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEach { tab ->
                            val isSelected = activeTab == tab
                            val interactionSource = remember { MutableInteractionSource() }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = ripple(bounded = true, color = Color(0xFF00E5FF))
                                    ) {
                                        activeTab = tab
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        tint = if (isSelected) Color(0xFF00E5FF) else Color(0xFF71717A),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .background(Color(0xFF00E5FF), CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
