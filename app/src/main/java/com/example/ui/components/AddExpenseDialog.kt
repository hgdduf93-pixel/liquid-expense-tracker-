package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Transaction

@Composable
fun AddExpenseFloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(46.dp)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                spotColor = Color.White.copy(alpha = 0.3f),
                ambientColor = Color.White.copy(alpha = 0.15f)
            ),
        shape = CircleShape,
        color = Color(0xFFF4F4F6), // Porcelain White Capsule
        contentColor = Color(0xFF09090B),
        border = BorderStroke(1.dp, Color.White)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Expense",
                tint = Color(0xFF09090B),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Add Expense",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF09090B),
                letterSpacing = 0.2.sp
            )
        }
    }
}

@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var merchantText by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("DEBIT") } // "DEBIT" or "CREDIT"
    var selectedCategory by remember { mutableStateOf("Food") }

    val categories = remember {
        listOf(
            Triple("Food", Icons.Filled.Restaurant, "Food"),
            Triple("Travel", Icons.Filled.DirectionsCar, "Travel"),
            Triple("Bills", Icons.Filled.ReceiptLong, "Bills"),
            Triple("Shopping", Icons.Filled.ShoppingBag, "Shopping"),
            Triple("Entertainment", Icons.Filled.Movie, "Entertainment"),
            Triple("Health", Icons.Filled.MedicalServices, "Health"),
            Triple("Others", Icons.Filled.MoreHoriz, "Others")
        )
    }

    val parsedAmount = amountText.toDoubleOrNull()
    val isAmountValid = parsedAmount != null && parsedAmount > 0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(26.dp)
                ),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF0D0F17),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MANUAL ENTRY",
                            color = Color(0xFF71717A),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "New Transaction",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF71717A),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Type Selector: Debit (Expense) vs Credit (Income)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(Color(0xFF151824), RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isDebit = transactionType == "DEBIT"
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDebit) Color(0xFFF43F5E).copy(alpha = 0.2f) else Color.Transparent,
                        border = if (isDebit) BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.6f)) else null,
                        onClick = { transactionType = "DEBIT" }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Debit (Expense)",
                                color = if (isDebit) Color(0xFFF43F5E) else Color(0xFF71717A),
                                fontSize = 12.sp,
                                fontWeight = if (isDebit) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    val isCredit = transactionType == "CREDIT"
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCredit) Color(0xFF10B981).copy(alpha = 0.2f) else Color.Transparent,
                        border = if (isCredit) BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f)) else null,
                        onClick = { transactionType = "CREDIT" }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Credit (Income)",
                                color = if (isCredit) Color(0xFF10B981) else Color(0xFF71717A),
                                fontSize = 12.sp,
                                fontWeight = if (isCredit) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Amount Field
                Text(
                    text = "AMOUNT",
                    color = Color(0xFFA1A1AA),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' } && input.count { it == '.' } <= 1) {
                            amountText = input
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Text(
                            text = "₹",
                            color = Color(0xFF00E5FF),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    placeholder = {
                        Text(
                            text = "0.00",
                            color = Color(0xFF3F3F46),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedContainerColor = Color(0xFF131520),
                        unfocusedContainerColor = Color(0xFF131520)
                    )
                )

                // Quick Increment Chips
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(50, 100, 250, 500, 1000)
                    presets.forEach { preset ->
                        Surface(
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF181B28),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            onClick = {
                                val current = amountText.toDoubleOrNull() ?: 0.0
                                amountText = String.format("%.0f", current + preset)
                            }
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+₹$preset",
                                    color = Color(0xFFD4D4D8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Merchant / Title Field
                Text(
                    text = "TITLE / MERCHANT",
                    color = Color(0xFFA1A1AA),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = merchantText,
                    onValueChange = { merchantText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "e.g., Chai, Swiggy, Uber, Rent",
                            color = Color(0xFF52525B),
                            fontSize = 13.sp
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedContainerColor = Color(0xFF131520),
                        unfocusedContainerColor = Color(0xFF131520)
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Category Selector
                Text(
                    text = "CATEGORY",
                    color = Color(0xFFA1A1AA),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { (catName, catIcon, _) ->
                        val isSelected = selectedCategory == catName
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.18f) else Color(0xFF151824),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.08f)
                            ),
                            onClick = { selectedCategory = catName }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = catIcon,
                                    contentDescription = catName,
                                    tint = if (isSelected) Color(0xFF00E5FF) else Color(0xFF71717A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = catName,
                                    color = if (isSelected) Color.White else Color(0xFFA1A1AA),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons: Cancel and Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA1A1AA))
                    ) {
                        Text(text = "Cancel", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (isAmountValid && parsedAmount != null) {
                                val title = merchantText.trim().ifBlank {
                                    if (transactionType == "CREDIT") "Manual Income" else "$selectedCategory Expense"
                                }
                                val transaction = Transaction(
                                    amount = parsedAmount,
                                    type = transactionType,
                                    merchantName = title,
                                    category = if (transactionType == "CREDIT") "Income" else selectedCategory,
                                    timestamp = System.currentTimeMillis(),
                                    rawMessage = "Manual transaction entry"
                                )
                                onSave(transaction)
                                onDismiss()
                            }
                        },
                        enabled = isAmountValid,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF4F4F6), // Porcelain white high-trust accent
                            contentColor = Color(0xFF09090B),
                            disabledContainerColor = Color(0xFF1E2130),
                            disabledContentColor = Color(0xFF52525B)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save Expense",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
