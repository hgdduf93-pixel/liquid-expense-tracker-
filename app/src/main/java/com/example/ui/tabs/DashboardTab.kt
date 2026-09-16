package com.example.ui.tabs

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.GlassCard
import com.example.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

fun checkNotificationAccess(context: Context): Boolean {
    return try {
        val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
        val packageName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        enabledListeners.contains(packageName) || (flat != null && flat.contains(packageName))
    } catch (e: Throwable) {
        false
    }
}

@Composable
fun DashboardTab(
    viewModel: ExpenseViewModel = viewModel(),
    onNavigateToTransactions: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasNotificationAccess by remember {
        mutableStateOf(try { checkNotificationAccess(context) } catch (e: Throwable) { false })
    }

    // Re-check permission automatically whenever the app comes back to foreground (ON_RESUME)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                try {
                    hasNotificationAccess = checkNotificationAccess(context)
                } catch (ignored: Throwable) {}
            }
        }
        try {
            lifecycleOwner.lifecycle.addObserver(observer)
        } catch (ignored: Throwable) {}
        onDispose {
            try {
                lifecycleOwner.lifecycle.removeObserver(observer)
            } catch (ignored: Throwable) {}
        }
    }

    // Real reactive values from Room Database & fallback state
    val totalSpent by viewModel.totalSpent.collectAsStateWithLifecycle()
    val todaySpent by viewModel.todaySpent.collectAsStateWithLifecycle()
    val monthlyBudget = viewModel.monthlyBudget
    val remainingBudget = (monthlyBudget - totalSpent).coerceAtLeast(0.0)

    val totalAmountAnim = remember { Animatable(0f) }
    val todayAmountAnim = remember { Animatable(0f) }
    val budgetAmountAnim = remember { Animatable(0f) }
    val arcSweepAnim = remember { Animatable(0f) }
    var isBalanceVisible by remember { mutableStateOf(true) }

    val targetArc = if (monthlyBudget > 0) {
        ((totalSpent / monthlyBudget) * 360f).toFloat().coerceIn(0f, 360f)
    } else 0f

    // Smooth count-up easing whenever database updates
    LaunchedEffect(totalSpent, todaySpent, remainingBudget) {
        launch { totalAmountAnim.animateTo(totalSpent.toFloat(), tween(1000, easing = FastOutSlowInEasing)) }
        launch { todayAmountAnim.animateTo(todaySpent.toFloat(), tween(1000, easing = FastOutSlowInEasing)) }
        launch { budgetAmountAnim.animateTo(remainingBudget.toFloat(), tween(1000, easing = FastOutSlowInEasing)) }
        launch { arcSweepAnim.animateTo(if (targetArc > 0f) targetArc else 45f, tween(1000, easing = FastOutSlowInEasing)) }
    }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Notification Access Alert Card (If disabled)
        AnimatedVisibility(
            visible = !hasNotificationAccess,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        // In preview sandbox, toggle state immediately for seamless preview interaction
                        hasNotificationAccess = true

                        // Try native Android Settings intent safely
                        try {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Throwable) {
                            try {
                                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(fallbackIntent)
                            } catch (ignored: Throwable) {
                                // Graceful fallback in preview sandbox
                            }
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = "Warning",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Tracking Disabled",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tap here to grant Notification Access for PhonePe, GPay, Paytm & SMS",
                                color = Color(0xFFA1A1AA),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Grant Access",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Active Status indicator if permission is enabled
        if (hasNotificationAccess) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Allow toggling back for preview testing
                        hasNotificationAccess = false
                    }
            ) {
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                    shape = CircleShape,
                    modifier = Modifier.size(8.dp)
                ) {
                    Box(modifier = Modifier.background(Color(0xFF10B981), CircleShape))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LIVE AUTO-TRACKING ACTIVE",
                    color = Color(0xFF10B981),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }
        }

        Text(
            text = "EXPENSE TRACKER",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF71717A),
            letterSpacing = 2.sp
        )
        Text(
            text = "FUNDS",
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Spent Amount Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onNavigateToTransactions
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "SPENT AMOUNT",
                            color = Color(0xFFA1A1AA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { isBalanceVisible = !isBalanceVisible },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isBalanceVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (isBalanceVisible) "Hide balance" else "Show balance",
                                tint = Color(0xFF71717A),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View Transactions",
                        tint = Color(0xFF00E5FF).copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBalanceVisible) currencyFormatter.format(totalAmountAnim.value.toDouble()) else "₹ ••••••••",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Canvas(modifier = Modifier.size(68.dp)) {
                        // Background track
                        drawArc(
                            color = Color.White.copy(alpha = 0.08f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 12f)
                        )
                        // Progress arc
                        drawArc(
                            color = Color(0xFF00E5FF),
                            startAngle = -90f,
                            sweepAngle = arcSweepAnim.value,
                            useCenter = false,
                            style = Stroke(width = 12f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Today & Budget Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlassCard(
                modifier = Modifier.weight(1f),
                onClick = onNavigateToTransactions
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "TODAY",
                        color = Color(0xFFA1A1AA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isBalanceVisible) currencyFormatter.format(todayAmountAnim.value.toDouble()) else "₹ ••••",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            GlassCard(
                modifier = Modifier.weight(1f),
                onClick = onNavigateToTransactions
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "BUDGET",
                        color = Color(0xFFA1A1AA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isBalanceVisible) currencyFormatter.format(budgetAmountAnim.value.toDouble()) else "₹ ••••",
                        color = Color(0xFF34D399),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Navigation to Transactions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = Color(0xFF00E5FF))
                ) {
                    onNavigateToTransactions()
                }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "TRANSACTION HISTORY",
                color = Color(0xFF71717A),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp
            )
            Text(
                "View all →",
                color = Color(0xFF00E5FF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
