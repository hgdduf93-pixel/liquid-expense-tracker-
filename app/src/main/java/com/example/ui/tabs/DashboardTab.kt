package com.example.ui.tabs

import com.example.data.Transaction
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import java.text.SimpleDateFormat
import java.util.Date
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

private fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "food" -> Icons.Filled.Restaurant
        "travel" -> Icons.Filled.DirectionsCar
        "bills" -> Icons.Filled.ReceiptLong
        "shopping" -> Icons.Filled.ShoppingBag
        "entertainment" -> Icons.Filled.Movie
        "health" -> Icons.Filled.MedicalServices
        "income" -> Icons.Filled.ArrowDownward
        else -> Icons.Filled.MoreHoriz
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffMillis = now - timestamp
    val diffHours = diffMillis / (1000 * 60 * 60)
    return when {
        diffHours < 1 -> "Just now"
        diffHours < 24 -> "Today"
        diffHours < 48 -> "Yesterday"
        else -> {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

@Composable
fun DashboardTab(
    viewModel: ExpenseViewModel = viewModel(),
    onNavigateToTransactions: () -> Unit = {},
    onOpenPrivacySheet: () -> Unit = {}
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
    val netBalance by viewModel.netBalance.collectAsStateWithLifecycle()
    val totalCredits by viewModel.totalCredits.collectAsStateWithLifecycle()
    val totalDebits by viewModel.totalDebits.collectAsStateWithLifecycle()
    val todayNetSpent by viewModel.todayNetSpent.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val recentTransactions = remember(allTransactions) {
        allTransactions.sortedByDescending { it.timestamp }.take(5)
    }

    val monthlyBudget by viewModel.monthlyBudget.collectAsStateWithLifecycle()
    val totalSpentThisMonth by viewModel.totalSpentThisMonth.collectAsStateWithLifecycle()
    val remainingBudget = if (monthlyBudget > 0) monthlyBudget - totalSpentThisMonth else 0.0
    val isOverBudget = monthlyBudget > 0 && totalSpentThisMonth > monthlyBudget

    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetInputText by remember { mutableStateOf("") }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }

    val netBalanceAnim = remember { Animatable(0f) }
    val todayNetAnim = remember { Animatable(0f) }
    val budgetAmountAnim = remember { Animatable(0f) }
    val arcSweepAnim = remember { Animatable(0f) }
    var isBalanceVisible by remember { mutableStateOf(true) }

    val targetArc = if (monthlyBudget > 0) {
        ((totalSpentThisMonth / monthlyBudget) * 360f).toFloat().coerceIn(0f, 360f)
    } else {
        ((totalDebits / 50000.0) * 360f).toFloat().coerceIn(0f, 360f)
    }
    val arcColor = if (isOverBudget) Color(0xFFEF4444) else Color(0xFF34D399)

    // Smooth count-up easing whenever database updates
    LaunchedEffect(netBalance, todayNetSpent, remainingBudget, monthlyBudget, totalDebits) {
        launch { netBalanceAnim.animateTo(netBalance.toFloat(), tween(1000, easing = FastOutSlowInEasing)) }
        launch { todayNetAnim.animateTo(kotlin.math.abs(todayNetSpent).toFloat(), tween(1000, easing = FastOutSlowInEasing)) }
        launch { budgetAmountAnim.animateTo(remainingBudget.toFloat(), tween(1000, easing = FastOutSlowInEasing)) }
        launch { arcSweepAnim.animateTo(if (targetArc > 0f) targetArc else 45f, tween(1000, easing = FastOutSlowInEasing)) }
    }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 120.dp)
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
                        onOpenPrivacySheet()
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
                                text = "Tap here for Privacy model & 3-step setup guide (or continue manually)",
                                color = Color(0xFFA1A1AA),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open Setup Guide",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }


        // Luxury Branded Lockup Header with Animated Glowing Radial Gradient Background
        val infiniteTransition = rememberInfiniteTransition(label = "headerGlow")
        val glowOffset by infiniteTransition.animateFloat(
            initialValue = -20f,
            targetValue = 25f,
            animationSpec = infiniteRepeatable(
                animation = tween(5000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowOffset"
        )
        val glowRadiusFactor by infiniteTransition.animateFloat(
            initialValue = 0.65f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowRadius"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.22f),
                                Color(0xFF34D399).copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = center.copy(x = center.x + glowOffset, y = center.y + glowOffset / 2),
                            radius = size.width * glowRadiusFactor
                        )
                    )
                }
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "OFFLINE OBSIDIAN VAULT • DESI CORE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF71717A),
                    letterSpacing = 2.2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MUDRIX",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        color = Color.White.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                    ) {
                        Text(
                            text = "GSD",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Total Balance / Cash Flow Card
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
                            "TOTAL BALANCE",
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
                    Column(modifier = Modifier.weight(1f)) {
                        val netVal = netBalanceAnim.value.toDouble()
                        val netColor = if (netVal >= 0) Color.White else Color(0xFFF87171)
                        val formattedNetVal = if (isBalanceVisible) {
                            val absV = kotlin.math.abs(netVal)
                            val formatted = currencyFormatter.format(absV)
                            when {
                                netVal > 0.001 -> "+ $formatted"
                                netVal < -0.001 -> "- $formatted"
                                else -> formatted
                            }
                        } else "₹ ••••••••"

                        Text(
                            text = formattedNetVal,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = netColor
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Inflow / Outflow Micro Bar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "↑ ${currencyFormatter.format(totalCredits)}",
                                color = Color(0xFF34D399),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("•", color = Color.White.copy(alpha = 0.2f), fontSize = 12.sp)
                            Text(
                                text = "↓ ${currencyFormatter.format(totalDebits)}",
                                color = Color(0xFFE4E4E7),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Canvas(modifier = Modifier.size(68.dp)) {
                        drawArc(
                            color = Color.White.copy(alpha = 0.08f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 12f)
                        )
                        drawArc(
                            color = arcColor,
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
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = onNavigateToTransactions
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "TODAY",
                        color = Color(0xFF71717A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isBalanceVisible) currencyFormatter.format(todayNetAnim.value.toDouble()) else "₹ ••••",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val todaySubtitle = when {
                        todayNetSpent > 0.001 -> "Daily spent"
                        todayNetSpent < -0.001 -> "Net income"
                        else -> "Break-even"
                    }
                    Text(
                        todaySubtitle,
                        color = Color(0xFF52525B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = {
                    budgetInputText = if (monthlyBudget > 0) monthlyBudget.toInt().toString() else ""
                    showBudgetDialog = true
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "REMAINING",
                        color = Color(0xFF71717A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (!isBalanceVisible) {
                        Text(
                            text = "₹ ••••",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (monthlyBudget <= 0.0) {
                        Text(
                            text = "Tap to Set",
                            color = Color(0xFFF59E0B),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (remainingBudget >= 0) {
                        Text(
                            text = currencyFormatter.format(budgetAmountAnim.value.toDouble()),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "-${currencyFormatter.format(kotlin.math.abs(budgetAmountAnim.value.toDouble()))}",
                            color = Color(0xFFEF4444),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (monthlyBudget > 0) "of ${currencyFormatter.format(monthlyBudget)} target" else "No budget set.",
                        color = Color(0xFF52525B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Navigation to Transactions Header
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

        Spacer(modifier = Modifier.height(10.dp))

        // Recent Transactions Container or Elegant Frosted Empty State
        if (recentTransactions.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.03f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = CircleShape,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = Color(0xFF71717A),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No Transactions Yet",
                        color = Color(0xFFE4E4E7),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Auto-tracks via UPI notifications, or tap '+ Add Expense' to log one manually.",
                        color = Color(0xFF71717A),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                recentTransactions.forEach { tx ->
                    val isCredit = tx.type.equals("CREDIT", ignoreCase = true)
                    val categoryIcon = getCategoryIcon(tx.category)
                    val formattedTime = formatRelativeTime(tx.timestamp)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent),
                        color = Color.White.copy(alpha = 0.03f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        onClick = { selectedTransaction = tx }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left side: Icon + Merchant + Timestamp
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.06f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = categoryIcon,
                                            contentDescription = tx.category,
                                            tint = if (isCredit) Color(0xFF34D399) else Color(0xFF00E5FF),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tx.merchantName,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = formattedTime,
                                        color = Color(0xFF71717A),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Right side: Amount + Status
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = (if (isCredit) "+ " else "- ") + currencyFormatter.format(tx.amount),
                                    color = if (isCredit) Color(0xFF34D399) else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isCredit) "Credited" else "Debited",
                                    color = Color(0xFF71717A),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "POWERED BY GSD",
                color = Color(0xFF52525B),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.8.sp
            )
        }
    }

    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Set Monthly Budget", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter your monthly spending target (₹).", color = Color(0xFFA1A1AA), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = budgetInputText,
                        onValueChange = { budgetInputText = it },
                        placeholder = { Text("e.g. 50000", color = Color(0xFF71717A)) },
                        prefix = { Text("₹ ", color = Color.White, fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            cursorColor = Color(0xFF00E5FF)
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Quick Chips:", color = Color(0xFFA1A1AA), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10000.0, 25000.0, 50000.0, 100000.0).forEach { chipVal ->
                            Surface(
                                color = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                                modifier = Modifier
                                    .clickable { budgetInputText = chipVal.toInt().toString() }
                            ) {
                                Text(
                                    text = "₹${chipVal.toInt() / 1000}k",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = budgetInputText.toDoubleOrNull() ?: 0.0
                        viewModel.setMonthlyBudget(parsed)
                        showBudgetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("Save Target", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text("Cancel", color = Color(0xFFA1A1AA))
                }
            },
            containerColor = Color(0xFF14151C)
        )
    }

    if (selectedTransaction != null) {
        val tx = selectedTransaction!!
        com.example.ui.components.TransactionDetailDialog(
            transaction = tx,
            onDismiss = { selectedTransaction = null },
            categoryIcon = getCategoryIcon(tx.category)
        )
    }
}
