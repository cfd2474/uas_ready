package com.uasready.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uasready.domain.model.AssessmentStatus
import com.uasready.ui.theme.*

@Composable
fun MetricSummaryCard(
    title: String,
    primaryValue: String,
    secondaryValue: String? = null,
    status: AssessmentStatus? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val statusColor = status?.toColor() ?: AviationAccent

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AviationDarkCard)
            .border(1.dp, AviationDarkBorder, RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = AviationAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                if (status != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = status.name,
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = statusColor,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = primaryValue,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            if (secondaryValue != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = secondaryValue,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary
                    )
                )
            }
        }
    }
}
