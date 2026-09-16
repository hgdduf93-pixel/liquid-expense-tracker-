package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "scale")
    
    val glassBrush = Brush.linearGradient(
        colors = listOf(Color.White.copy(alpha = 0.05f), Color(0xFF14151A).copy(alpha = 0.85f))
    )

    Card(
        modifier = modifier
            .scale(scale)
            .shadow(30.dp, RoundedCornerShape(36.dp), spotColor = Color.Black.copy(alpha = 0.9f))
            .then(if (onClick != null) Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick) else Modifier)
            .border(
                1.dp, 
                Color.White.copy(alpha = 0.14f),
                RoundedCornerShape(36.dp)
            )
            .drawBehind {
                drawLine(
                    color = Color.White.copy(alpha = 0.28f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.5f.dp.toPx()
                )
            }
            .background(glassBrush, RoundedCornerShape(36.dp)),
        shape = RoundedCornerShape(36.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        androidx.compose.foundation.layout.Column(content = content)
    }
}
