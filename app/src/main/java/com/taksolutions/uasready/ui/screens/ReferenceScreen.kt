package com.taksolutions.uasready.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taksolutions.uasready.domain.model.ChecklistCategory
import com.taksolutions.uasready.domain.model.ChecklistGroup
import com.taksolutions.uasready.domain.model.ChecklistItem
import com.taksolutions.uasready.domain.model.EmergencyProcedure
import com.taksolutions.uasready.ui.theme.*

enum class ReferenceTab(val title: String) {
    CHECKLISTS("Checklists"),
    EMERGENCY("Emergency Procedures")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var checklists by remember { mutableStateOf(ChecklistGroup.DEFAULT_CHECKLISTS) }
    var selectedTab by remember { mutableStateOf(ReferenceTab.CHECKLISTS) }
    var showAddItemDialog by remember { mutableStateOf(false) }

    // Dialog state
    var selectedCategory by remember { mutableStateOf(ChecklistCategory.PREFLIGHT) }
    var itemTitle by remember { mutableStateOf("") }
    var itemDescription by remember { mutableStateOf("") }
    var isCriticalItem by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "REFERENCE & PROCEDURES",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    if (selectedTab == ReferenceTab.CHECKLISTS) {
                        Button(
                            onClick = { showAddItemDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AviationCyan,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Add Checklist Item", color = Color.White, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AviationDarkBackground)
            )
        },
        containerColor = AviationDarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))
                // Tab Row Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AviationDarkCard)
                        .border(1.dp, AviationDarkBorder, RoundedCornerShape(10.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReferenceTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) AviationAccent else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTab = tab }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (tab == ReferenceTab.CHECKLISTS) Icons.Default.Checklist else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ==================== TAB 1: OPERATIONAL CHECKLISTS ====================
            if (selectedTab == ReferenceTab.CHECKLISTS) {
                item {
                    // Info banner explaining read-only nature
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AviationDarkCard)
                            .border(1.dp, AviationDarkBorder, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = AviationAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Standard public safety reference material. These checklists are read-only operational guides and are not required click-through workflows.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontSize = 12.sp)
                            )
                        }
                    }
                }

                items(checklists) { group ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AviationDarkCard),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AviationDarkBorder)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = group.title.uppercase(),
                                    style = MaterialTheme.typography.titleMedium.copy(color = AviationAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AviationDarkSurface)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("${group.items.size} ITEMS", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontSize = 10.sp))
                                }
                            }

                            HorizontalDivider(color = AviationDarkBorder)

                            group.items.forEach { item ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.isCritical) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (item.isCritical) SafetyGoLight else TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextPrimary,
                                                    fontSize = 12.sp
                                                )
                                            )
                                            if (item.isCritical) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(SafetyCautionBg)
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text("CRITICAL", style = MaterialTheme.typography.labelMedium.copy(color = SafetyCautionLight, fontSize = 8.sp))
                                                }
                                            }
                                        }
                                        if (item.description.isNotBlank()) {
                                            Text(
                                                text = item.description,
                                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==================== TAB 2: EMERGENCY PROCEDURES ====================
            if (selectedTab == ReferenceTab.EMERGENCY) {
                item {
                    // Emergency Header Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SafetyNoGoBg)
                            .border(1.dp, SafetyNoGoLight, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = SafetyNoGoLight, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "EMERGENCY PROCEDURES (SOP)",
                                    style = MaterialTheme.typography.titleMedium.copy(color = SafetyNoGoLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                )
                                Text(
                                    text = "Standard response protocols for in-flight anomalies, critical battery levels, and unexpected environmental hazards.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 11.sp)
                                )
                            }
                        }
                    }
                }

                items(EmergencyProcedure.DEFAULT_PROCEDURES) { proc ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AviationDarkCard),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (proc.isCriticalWarning) SafetyCautionLight.copy(alpha = 0.6f) else AviationDarkBorder
                            )
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Step Number Badge
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (proc.isCriticalWarning) SafetyCautionBg else AviationDarkSurface)
                                    .border(1.dp, if (proc.isCriticalWarning) SafetyCautionLight else AviationAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${proc.stepNumber}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (proc.isCriticalWarning) SafetyCautionLight else AviationAccent,
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = proc.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 13.sp
                                    )
                                )
                                Text(
                                    text = proc.description,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // Safety Principle Training Excerpt Footer
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AviationDarkCard)
                            .border(1.5.dp, AviationAccent, RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = AviationAccent, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "SAFETY OVER AIRCRAFT PRINCIPLE",
                                    style = MaterialTheme.typography.labelMedium.copy(color = AviationAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                )
                                Text(
                                    text = "Always have an emergency plan in place and stay familiar with your drone's capabilities and limitations. In all emergency situations, prioritizing safety over the drone itself is essential.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 11.sp, lineHeight = 15.sp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Add Checklist Item Modal Dialog
    if (showAddItemDialog) {
        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, tint = AviationAccent, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Checklist Item", style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Add a custom verification item to an existing checklist category:",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                    )

                    // Category Dropdown
                    Box {
                        OutlinedButton(
                            onClick = { categoryDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Category: ${selectedCategory.title}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = AviationAccent)
                            }
                        }

                        DropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            ChecklistCategory.values().forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.title, style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        selectedCategory = cat
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Item Title
                    OutlinedTextField(
                        value = itemTitle,
                        onValueChange = { itemTitle = it },
                        label = { Text("Item Title") },
                        placeholder = { Text("e.g. FLIR Thermal NUC Calibration") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Item Description
                    OutlinedTextField(
                        value = itemDescription,
                        onValueChange = { itemDescription = it },
                        label = { Text("Description / Action (Optional)") },
                        placeholder = { Text("e.g. Execute flat-field calibration before flight") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Critical Item Toggle (Structured Card with Aligned Checkbox & High Contrast)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AviationDarkSurface,
                        border = BorderStroke(1.dp, if (isCriticalItem) SafetyCautionLight else AviationDarkBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isCriticalItem = !isCriticalItem }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isCriticalItem,
                                onCheckedChange = { isCriticalItem = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = SafetyCautionLight,
                                    uncheckedColor = TextSecondary,
                                    checkmarkColor = AviationDarkBackground
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "CRITICAL CHECKLIST ITEM",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isCriticalItem) SafetyCautionLight else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                                Text(
                                    text = "Mandatory safety verification before launch",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (itemTitle.isNotBlank()) {
                            val newItem = ChecklistItem(
                                id = "custom_item_${System.currentTimeMillis()}",
                                title = itemTitle.trim(),
                                description = itemDescription.trim(),
                                isCritical = isCriticalItem
                            )

                            // Find target group or create new custom group
                            val existingGroupIndex = checklists.indexOfFirst { it.category == selectedCategory }
                            if (existingGroupIndex >= 0) {
                                val existingGroup = checklists[existingGroupIndex]
                                val updatedGroup = existingGroup.copy(items = existingGroup.items + newItem)
                                checklists = checklists.toMutableList().apply {
                                    set(existingGroupIndex, updatedGroup)
                                }
                            } else {
                                val newGroup = ChecklistGroup(
                                    id = "custom_group_${System.currentTimeMillis()}",
                                    category = selectedCategory,
                                    title = selectedCategory.title,
                                    items = listOf(newItem)
                                )
                                checklists = checklists + newGroup
                            }

                            showAddItemDialog = false
                            itemTitle = ""
                            itemDescription = ""
                            isCriticalItem = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AviationCyan, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ADD ITEM", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = AviationDarkCard
        )
    }
}
