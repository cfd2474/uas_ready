package com.uasready.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uasready.domain.model.AssessmentCategory
import com.uasready.domain.model.AssessmentResult
import com.uasready.ui.components.RuleAuditCard
import com.uasready.ui.theme.*
import com.uasready.ui.viewmodel.MainUiState

@Composable
fun AssessmentDetailScreen(
    uiState: MainUiState,
    onCategoryFilterSelected: (AssessmentCategory?) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val assessment = uiState.assessmentResult

    Scaffold(
        topBar = {
            // Ultra-compact top bar header designed for 360dp landscape
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(AviationDarkBackground)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "DETAILED REPORT",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                )
            }
        },
        containerColor = AviationDarkBackground
    ) { paddingValues ->
        if (assessment == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AviationAccent)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Compact Overall Status Summary Card
            item {
                val overallColor = assessment.overallStatus.toColor()
                val overallBg = assessment.overallStatus.toBgColor()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(overallBg)
                        .border(1.dp, overallColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(overallColor)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = assessment.overallStatus.name,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = assessment.primaryHeadline,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )
                                )
                            }
                        }

                        if (assessment.primaryReasons.isNotEmpty()) {
                            assessment.primaryReasons.forEach { reason ->
                                Text(
                                    text = "• $reason",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedCategoryFilter == null,
                            onClick = { onCategoryFilterSelected(null) },
                            label = { Text("ALL (${assessment.allRuleResults.size})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AviationCyan,
                                selectedLabelColor = Color.White,
                                containerColor = AviationDarkCard,
                                labelColor = TextSecondary
                            )
                        )
                    }

                    items(AssessmentCategory.values()) { category ->
                        val count = assessment.categoryAssessments.firstOrNull { it.category == category }?.ruleResults?.size ?: 0
                        FilterChip(
                            selected = uiState.selectedCategoryFilter == category,
                            onClick = { onCategoryFilterSelected(category) },
                            label = { Text("${category.displayName.substringBefore(" ")} ($count)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AviationCyan,
                                selectedLabelColor = Color.White,
                                containerColor = AviationDarkCard,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }

            // List of Evaluated Categories and Rules
            val categoriesToDisplay = if (uiState.selectedCategoryFilter != null) {
                assessment.categoryAssessments.filter { it.category == uiState.selectedCategoryFilter }
            } else {
                assessment.categoryAssessments
            }

            items(categoriesToDisplay) { catAssessment ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = catAssessment.category.displayName.uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = AviationAccent,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                fontSize = 11.sp
                            )
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(catAssessment.status.toBgColor())
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = catAssessment.status.name,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = catAssessment.status.toColor(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    catAssessment.ruleResults.forEach { rule ->
                        RuleAuditCard(rule = rule, modifier = Modifier.padding(bottom = 6.dp))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
