package com.example.ui.tabs

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Transaction
import com.example.ui.components.GlassCard
import com.example.viewmodel.ExpenseViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsTab() {
    Column(modifier = Modifier.padding(24.dp).fillMaxSize().padding(bottom = 90.dp)) {
        Text("SPENDING ANALYTICS", color = Color(0xFF71717A), fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.12.sp)
        Spacer(modifier = Modifier.height(16.dp))
        GlassCard(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            Column(modifier = Modifier.padding(16.dp)) { Text("Weekly Spending Trend", color = Color.White, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
fun TransactionsTab(viewModel: ExpenseViewModel = viewModel()) {
    val dbTransactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    
    // Sample fallback transactions for immediate rich display when DB has no items yet
    val sampleTransactions = remember {
        val now = System.currentTimeMillis()
        listOf(
            Transaction(
                id = 101,
                amount = 4299.0,
                type = "DEBIT",
                merchantName = "Apple Store",
                category = "Shopping",
                timestamp = now - 1000 * 60 * 35,
                rawMessage = "Paid to Apple Store"
            ),
            Transaction(
                id = 102,
                amount = 340.0,
                type = "DEBIT",
                merchantName = "Uber Technologies",
                category = "Travel",
                timestamp = now - 1000 * 60 * 60 * 2,
                rawMessage = "Debited for Uber Ride"
            ),
            Transaction(
                id = 103,
                amount = 480.0,
                type = "DEBIT",
                merchantName = "Starbucks Coffee",
                category = "Food",
                timestamp = now - 1000 * 60 * 60 * 4,
                rawMessage = "Paid at Starbucks"
            ),
            Transaction(
                id = 104,
                amount = 25000.0,
                type = "CREDIT",
                merchantName = "Client Wire Transfer",
                category = "Income",
                timestamp = now - 1000 * 60 * 60 * 18,
                rawMessage = "Salary credited"
            ),
            Transaction(
                id = 105,
                amount = 649.0,
                type = "DEBIT",
                merchantName = "Netflix Premium",
                category = "Entertainment",
                timestamp = now - 1000 * 60 * 60 * 26,
                rawMessage = "Auto debited for Netflix"
            ),
            Transaction(
                id = 106,
                amount = 1250.0,
                type = "DEBIT",
                merchantName = "Swiggy Food Delivery",
                category = "Food",
                timestamp = now - 1000 * 60 * 60 * 32,
                rawMessage = "Paid to Swiggy"
            ),
            Transaction(
                id = 107,
                amount = 1890.0,
                type = "DEBIT",
                merchantName = "Amazon Marketplace",
                category = "Shopping",
                timestamp = now - 1000 * 60 * 60 * 46,
                rawMessage = "Purchased on Amazon"
            ),
            Transaction(
                id = 108,
                amount = 2100.0,
                type = "DEBIT",
                merchantName = "Electricity Bill",
                category = "Bills",
                timestamp = now - 1000 * 60 * 60 * 70,
                rawMessage = "Electricity bill paid"
            )
        )
    }

    val allList = remember(dbTransactions) {
        if (dbTransactions.isNotEmpty()) dbTransactions else sampleTransactions
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    val filterOptions = listOf("All", "Debits", "Credits")

    val filteredList = remember(allList, searchQuery, selectedFilter) {
        allList.filter { tx ->
            val matchesSearch = searchQuery.isBlank() ||
                tx.merchantName.contains(searchQuery, ignoreCase = true) ||
                tx.category.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "Debits" -> tx.type == "DEBIT"
                "Credits" -> tx.type == "CREDIT"
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxSize()
            .padding(bottom = 90.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TRANSACTIONS",
                    color = Color(0xFF71717A),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.12.sp
                )
                Text(
                    text = "History",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            // Count badge
            Surface(
                color = Color.White.copy(alpha = 0.07f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Text(
                    text = "${filteredList.size} items",
                    color = Color(0xFFA1A1AA),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar GlassCard
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF71717A),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(Color(0xFF00E5FF)),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search transactions or merchants...",
                                color = Color(0xFF71717A),
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear search",
                            tint = Color(0xFFA1A1AA),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filterOptions) { option ->
                val isSelected = selectedFilter == option
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.clickable { selectedFilter = option }
                ) {
                    Text(
                        text = option,
                        color = if (isSelected) Color(0xFF00E5FF) else Color(0xFFA1A1AA),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scrollable List with Fade-in and Slide-up animation
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            itemsIndexed(
                items = filteredList,
                key = { index, item -> if (item.id != 0) item.id else "$index-${item.merchantName}" }
            ) { index, item ->
                AnimatedTransactionItem(
                    transaction = item,
                    index = index
                )
            }
        }
    }
}

@Composable
fun AnimatedTransactionItem(
    transaction: Transaction,
    index: Int,
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(transaction.id, index) {
        // Staggered entrance animation: smooth fade-in and slide-up
        delay(minOf(index * 60L, 420L))
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 420,
                easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
            )
        )
    }

    val density = LocalDensity.current
    val slideDistancePx = with(density) { 36.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .graphicsLayer {
                alpha = animProgress.value
                translationY = (1f - animProgress.value) * slideDistancePx
            }
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Category Icon with subtle glossy circular badge
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.07f),
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getTransactionCategoryIcon(transaction.category),
                            contentDescription = transaction.category,
                            tint = if (transaction.type == "CREDIT") Color(0xFF00E5FF) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = transaction.merchantName,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${transaction.category} • ${formatTransactionTimestamp(transaction.timestamp)}",
                            color = Color(0xFFA1A1AA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    val isDebit = transaction.type == "DEBIT"
                    val prefix = if (isDebit) "- " else "+ "
                    val amountColor = if (isDebit) Color.White else Color(0xFF00E5FF)

                    Text(
                        text = "$prefix₹${formatTransactionAmount(transaction.amount)}",
                        color = amountColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isDebit) "Debited" else "Credited",
                        color = Color(0xFF71717A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun getTransactionCategoryIcon(category: String): ImageVector {
    return when (category.lowercase(Locale.ROOT)) {
        "food", "dining" -> Icons.Filled.Restaurant
        "travel", "transport" -> Icons.Filled.DirectionsCar
        "shopping" -> Icons.Filled.ShoppingBag
        "entertainment", "streaming" -> Icons.Filled.Movie
        "bills", "utilities" -> Icons.Filled.Receipt
        "income", "salary" -> Icons.Filled.ArrowDownward
        else -> Icons.Filled.CreditCard
    }
}

private fun formatTransactionAmount(amount: Double): String {
    return if (amount % 1.0 == 0.0) {
        String.format(Locale.getDefault(), "%,d", amount.toLong())
    } else {
        String.format(Locale.getDefault(), "%,.2f", amount)
    }
}

private fun formatTransactionTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffMillis = now - timestamp
    val diffHours = diffMillis / (1000 * 60 * 60)
    return when {
        diffHours < 1 -> "Just now"
        diffHours < 24 -> "Today"
        diffHours < 48 -> "Yesterday"
        else -> {
            val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

@Composable
fun CategoriesTab() {
    Column(modifier = Modifier.padding(24.dp).fillMaxSize().padding(bottom = 90.dp)) {
        Text("CATEGORIES", color = Color(0xFF71717A), fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.12.sp)
        Spacer(modifier = Modifier.height(16.dp))
        GlassCard(modifier = Modifier.fillMaxWidth().height(100.dp)) { /* Budget */ }
        Spacer(modifier = Modifier.height(16.dp))
        // Simplified Grid
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GlassCard(modifier = Modifier.weight(1f).height(120.dp)) { Text("Food", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(16.dp)) }
            GlassCard(modifier = Modifier.weight(1f).height(120.dp)) { Text("Travel", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(16.dp)) }
        }
    }
}

@Composable
fun SettingsTab() {
    val context = LocalContext.current as? Activity
    var showTerms by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(24.dp).fillMaxSize().padding(bottom = 90.dp)) {
        Text("SETTINGS", color = Color(0xFF71717A), fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.12.sp)
        Spacer(modifier = Modifier.height(16.dp))

        GlassCard(modifier = Modifier.fillMaxWidth().clickable { showTerms = true }) {
            Text("Terms & Conditions", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(16.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        GlassCard(modifier = Modifier.fillMaxWidth().clickable { context?.finish() }) {
            Text("Exit Application", color = Color.Red, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(16.dp))
        }
    }

    if (showTerms) {
        AlertDialog(
            onDismissRequest = { showTerms = false },
            title = { Text("Terms & Conditions") },
            text = { Text("This is the terms and conditions for the app. By continuing to use this app, you agree to these terms.") },
            confirmButton = {
                TextButton(onClick = { showTerms = false }) { Text("Close") }
            }
        )
    }
}
