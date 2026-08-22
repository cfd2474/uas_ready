package com.uasready.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
        AssessmentStatus.GO -> "All flight criteria satisfied"
        AssessmentStatus.CAUTION -> assessmentResult.primaryReasons.firstOrNull() ?: "Advisory conditions detected"
        AssessmentStatus.NO_GO -> assessmentResult.primaryReasons.firstOrNull() ?: "Flight not recommended"
        AssessmentStatus.DATA_UNAVAILABLE -> "Mandatory live telemetry required"
    }

    val icon = when (status) {
        AssessmentStatus.GO -> Icons.Default.CheckCircle
        AssessmentStatus.CAUTION -> Icons.Default.Warning
        AssessmentStatus.NO_GO -> Icons.Default.Error
        AssessmentStatus.DATA_UNAVAILABLE -> Icons.AutoMirrored.Filled.HelpOutline
    }

    // Compact horizontal banner for landscape optimization (saving vertical screen space)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(statusBg)
            .border(1.5.dp, statusColor.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
            .clickable { onTap() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = statusText,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = statusColor,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• TAP TO AUDIT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Text(
                        text = subText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
