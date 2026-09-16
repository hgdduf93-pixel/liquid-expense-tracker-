package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
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
    val tabs = listOf(Tab.Home, Tab.Analytics, Tab.Transactions, Tab.Settings)

    val infiniteTransition = rememberInfiniteTransition(label = "blob")
    val scale by infiniteTransition.animateFloat(1f, 1.08f, infiniteRepeatable(tween(4000), RepeatMode.Reverse), label = "scale")

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF060813))) {
        // Glowing Orbs
        Box(modifier = Modifier.size(300.dp).offset(x = 100.dp, y = (-50).dp).scale(scale).background(Color(0xFF8A2BE2).copy(alpha = 0.45f), CircleShape).blur(120.dp))
        Box(modifier = Modifier.size(300.dp).offset(x = (-100).dp, y = 300.dp).scale(scale).background(Color(0xFF00E5FF).copy(alpha = 0.45f), CircleShape).blur(120.dp))

        Column(modifier = Modifier.fillMaxSize().padding(top = 50.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        (scaleIn(initialScale = 0.94f, animationSpec = tween(260, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f))) + fadeIn(tween(260))) togetherWith
                        (scaleOut(targetScale = 0.96f, animationSpec = tween(180)) + fadeOut(tween(180)))
                    }, label = "tabTransition"
                ) { target ->
                    when (target) {
                        Tab.Home -> DashboardTab()
                        Tab.Analytics -> AnalyticsTab()
                        Tab.Transactions -> TransactionsTab()
                        Tab.Settings -> SettingsTab()
                    }
                }
            }

            // Bottom Nav
            Column(modifier = Modifier.padding(16.dp)) {
                GlassCard(modifier = Modifier.fillMaxWidth().height(70.dp)) {
                    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        tabs.forEach { tab ->
                            val isSelected = activeTab == tab
                            IconButton(onClick = { activeTab = tab }) {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) Color(0xFF00E5FF) else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackgroundShapes() {
    val density = LocalDensity.current
    Canvas(modifier = Modifier.fillMaxSize()) {
        with(density) {
            val baseSize = size.width * 1.2f
            val cornerRadius = 80.dp.toPx()

            for (i in 0 until 5) {
                val scale = 0.8f + (i * 0.15f)
                val rectSize = baseSize * scale
                
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.03f + (i * 0.01f)),
                    topLeft = Offset(-rectSize / 4, size.height - rectSize / 4),
                    size = Size(rectSize, rectSize),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}
