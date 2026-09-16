package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Transaction
import com.example.viewmodel.ExpenseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GlassmorphicDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: ExpenseViewModel = viewModel()
) {
    var showHistory by remember { mutableStateOf(false) }
    val transactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    
    Box(modifier = modifier.fillMaxSize().background(Color(0xFF070913))) {
        Box(modifier = Modifier.size(300.dp).offset(x = 100.dp, y = (-50).dp).background(Color(0xFF3A0CA3).copy(alpha = 0.4f), CircleShape).blur(80.dp))
        Box(modifier = Modifier.size(300.dp).offset(x = (-100).dp, y = 300.dp).background(Color(0xFF4CC9F0).copy(alpha = 0.4f), CircleShape).blur(80.dp))

        Column(modifier = Modifier.padding(24.dp).fillMaxSize()) {
            Text("EXPENSE TRACKER", style = MaterialTheme.typography.labelSmall, color = Color.Gray, letterSpacing = 2.sp)
            Text("FUNDS", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color.White)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            GlassCard(modifier = Modifier.fillMaxWidth().height(220.dp).clickable { showHistory = true }) {
                Row(modifier = Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SPENT AMOUNT", color = Color.Gray, fontSize = 12.sp)
                        Text("₹45,230", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("TOTAL SPEND", color = Color.Gray, fontSize = 12.sp)
                    }
                    Canvas(modifier = Modifier.size(100.dp)) {
                        drawArc(Color(0xFF4CC9F0), 0f, 270f, false, style = Stroke(width = 20f))
                        drawArc(Color(0xFFF72585), 270f, 90f, false, style = Stroke(width = 20f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GlassCard(modifier = Modifier.weight(1f).height(120.dp)) {
                    val animatedToday = animateIntAsState(targetValue = 1250, animationSpec = tween(durationMillis = 1000), label = "today")
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("TODAY", color = Color.Gray, fontSize = 12.sp)
                        Text("₹${animatedToday.value}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                GlassCard(modifier = Modifier.weight(1f).height(120.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("REMAINING", color = Color.Gray, fontSize = 12.sp)
                        Text("₹4,770", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            GlassCard(modifier = Modifier.fillMaxWidth().height(70.dp)) {
                Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Home, "Home", tint = Color.White)
                    Icon(Icons.Filled.BarChart, "Analytics", tint = Color.Gray)
                    Icon(Icons.Filled.SwapHoriz, "Swap", tint = Color.Gray)
                    Icon(Icons.Filled.GridView, "Grid", tint = Color.Gray)
                    Icon(Icons.Filled.Settings, "Settings", tint = Color.Gray)
                }
            }

        }
        AnimatedVisibility(
            visible = showHistory,
            enter = scaleIn(initialScale = 0.8f) + fadeIn(),
            exit = scaleOut(targetScale = 0.8f) + fadeOut()
        ) {
            TransactionHistoryPopup(transactions) { showHistory = false }
        }
    }
}

@Composable
fun TransactionHistoryPopup(transactions: List<Transaction>, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF070913).copy(alpha = 0.9f)).padding(16.dp)) {
            Column {
                Text("TRANSACTION HISTORY", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(16.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    itemsIndexed(transactions, key = { index, tx -> if (tx.id != 0) tx.id else index }) { index, tx ->
                        val animProgress = remember { Animatable(0f) }
                        LaunchedEffect(tx.id, index) {
                            delay(minOf(index * 60L, 400L))
                            animProgress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = 400,
                                    easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                                )
                            )
                        }
                        val density = LocalDensity.current
                        val slideDistance = with(density) { 36.dp.toPx() }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .graphicsLayer {
                                    alpha = animProgress.value
                                    translationY = (1f - animProgress.value) * slideDistance
                                }
                        ) {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(tx.merchantName, color = Color.White)
                                    Text("₹${tx.amount}", color = if (tx.type == "DEBIT") Color.Red else Color.Green, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
        }
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.border(BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)), RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(content = content)
    }
}

