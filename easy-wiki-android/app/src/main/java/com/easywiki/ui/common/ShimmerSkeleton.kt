package com.easywiki.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Creates a shimmer brush that animates horizontally across the target.
 */
@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val highlightColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    return Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f)
    )
}

/**
 * A single shimmer placeholder row, mimicking a card with title + subtitle lines.
 */
@Composable
fun ShimmerRow(
    modifier: Modifier = Modifier,
    lineCount: Int = 2,
    rowHeight: Dp = 14.dp,
    cornerRadius: Dp = 6.dp
) {
    val brush = shimmerBrush()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Title line (wider)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(rowHeight)
                .clip(RoundedCornerShape(cornerRadius))
                .background(brush)
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Subtitle lines
        repeat(lineCount - 1) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (index == 0) 0.9f else 0.5f)
                    .height(rowHeight - 2.dp)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(brush)
            )
            if (index < lineCount - 2) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

/**
 * Full-screen list skeleton with shimmer rows.
 */
@Composable
fun ShimmerListSkeleton(
    itemCount: Int = 5,
    lineCount: Int = 2,
    itemSpacing: Dp = 0.dp
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        repeat(itemCount) { index ->
            ShimmerRow(lineCount = lineCount)
            if (index < itemCount - 1 && itemSpacing > 0.dp) {
                Spacer(modifier = Modifier.height(itemSpacing))
            }
        }
    }
}

/**
 * Skeleton with a circular avatar placeholder on the left + text lines on the right.
 */
@Composable
fun ShimmerAvatarRow(
    modifier: Modifier = Modifier,
    avatarSize: Dp = 40.dp
) {
    val brush = shimmerBrush()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .width(avatarSize)
                .height(avatarSize)
                .clip(RoundedCornerShape(avatarSize / 2))
                .background(brush)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush)
            )
        }
    }
}
