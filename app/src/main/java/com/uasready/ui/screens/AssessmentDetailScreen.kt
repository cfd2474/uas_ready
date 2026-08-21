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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uasready.domain.model.AssessmentCategory
import com.uasready.domain.model.AssessmentResult
import com.uasready.ui.components.RuleAuditCard
import com.uasready.ui.theme.*
import com.uasready.ui.viewmodel.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
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
            TopAppBar(
                title = {
                    Text(
                        text = "DETAILED ASSESSMENT",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AviationDarkBackground
                )
            )
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Overall Status Summary Card
            item {
                val overallColor = assessment.overallStatus.toColor()
                val overallBg = assessment.overallStatus.toBgColor()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(overallBg)
                        .border(1.5.dp, overallColor, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "OVERALL STATUS",
                                style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = assessment.overallStatus.name,
                                style = MaterialTheme.typography.titleLarge.copy(color = overallColor, fontWeight = FontWeight.Black)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = assessment.primaryHeadline,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        assessment.primaryReasons.forEach { reason ->
                            Text(
                                text = "• $reason",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, lineHeight = 18.sp)
                            )
                        }
                    }
                }
            }

            // Category Filter Chips
            item {
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedCategoryFilter == null,
                            onClick = { onCategoryFilterSelected(null) },
                            label = { Text("ALL (${assessment.allRuleResults.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AviationCyan,
                                selectedLabelColor = TextPrimary,
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
                            label = { Text("${category.displayName.substringBefore(" ")} ($count)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AviationCyan,
                                selectedLabelColor = TextPrimary,
                                containerColor = AviationDarkCard,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
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
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = catAssessment.category.displayName.uppercase(),
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = AviationAccent,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
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
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    catAssessment.ruleResults.forEach { rule ->
                        RuleAuditCard(rule = rule, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
