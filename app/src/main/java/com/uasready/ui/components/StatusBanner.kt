package com.uasready.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uasready.domain.model.AssessmentResult
import com.uasready.domain.model.AssessmentStatus
import com.uasready.ui.theme.*

@Composable
fun StatusBanner(
    assessmentResult: AssessmentResult,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = assessmentResult.overallStatus
    val statusColor = status.toColor()
    val statusBg = status.toBgColor()

    val statusText = when (status) {
        AssessmentStatus.GO -> "GO"
        AssessmentStatus.CAUTION -> "CAUTION"
        AssessmentStatus.NO_GO -> "NO-GO"
        AssessmentStatus.DATA_UNAVAILABLE -> "DATA UNAVAILABLE"
    }

    val subText = when (status) {
        AssessmentStatus.GO -> "ALL SAFETY & REGULATORY CRITERIA SATISFIED"
        AssessmentStatus.CAUTION -> "ADVISORY CONDITIONS DETECTED — INSPECT DETAILS"
        AssessmentStatus.NO_GO -> "HARD CRITERIA VIOLATED — FLIGHT NOT RECOMMENDED"
        AssessmentStatus.DATA_UNAVAILABLE -> "MANDATORY LIVE TELEMETRY REQUIRED"
    }

    val icon = when (status) {
        AssessmentStatus.GO -> Icons.Default.CheckCircle
        AssessmentStatus.CAUTION -> Icons.Default.Warning
        AssessmentStatus.NO_GO -> Icons.Default.Error
        AssessmentStatus.DATA_UNAVAILABLE -> Icons.Default.HelpOutline
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(statusBg)
            .border(2.dp, statusColor.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
            .clickable { onTap() }
            .padding(vertical = 24.dp, horizontal = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Status Icon and Dominant Label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = statusText,
                    tint = statusColor,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.displayLarge.copy(
                        color = statusColor,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-headline
            Text(
                text = subText,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )

            // Primary reason if non-GO
            if (status != AssessmentStatus.GO && assessmentResult.primaryReasons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AviationDarkBackground.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "• ${assessmentResult.primaryReasons.first()}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
