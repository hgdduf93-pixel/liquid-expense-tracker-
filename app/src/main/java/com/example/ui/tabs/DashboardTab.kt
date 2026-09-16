package com.example.ui.tabs

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DashboardTab() {
    val totalAmount = remember { Animatable(0f) }
    val todayAmount = remember { Animatable(0f) }
    val budgetAmount = remember { Animatable(0f) }
    val arcSweep = remember { Animatable(0f) }
    var isBalanceVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        launch { totalAmount.animateTo(45230f, tween(1500, easing = LinearOutSlowInEasing)) }
        launch { todayAmount.animateTo(1250f, tween(1500, easing = LinearOutSlowInEasing)) }
        launch { budgetAmount.animateTo(4770f, tween(1500, easing = LinearOutSlowInEasing)) }
        launch { arcSweep.animateTo(270f, tween(1500, easing = LinearOutSlowInEasing)) }
    }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    Column(modifier = Modifier.padding(24.dp).fillMaxSize()) {
        Text("EXPENSE TRACKER", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Color.Gray, letterSpacing = 2.sp)
        Text("FUNDS", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color.White)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Hero Card
        GlassCard(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("SPENT AMOUNT", color = Color.Gray, fontSize = 12.sp)
                    Text(currencyFormatter.format(totalAmount.value), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Canvas(modifier = Modifier.size(80.dp)) {
                    drawArc(Color(0xFF00E5FF), 0f, arcSweep.value, false, style = Stroke(width = 15f))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GlassCard(modifier = Modifier.weight(1f).height(100.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("TODAY", color = Color.Gray, fontSize = 12.sp)
                    Text(currencyFormatter.format(todayAmount.value), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            GlassCard(modifier = Modifier.weight(1f).height(100.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("BUDGET", color = Color.Gray, fontSize = 12.sp)
                    Text(currencyFormatter.format(budgetAmount.value), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
