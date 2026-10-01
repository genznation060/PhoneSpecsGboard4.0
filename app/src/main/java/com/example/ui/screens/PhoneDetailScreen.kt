package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PhoneField
import com.example.data.model.PhoneWithFields
import com.example.data.model.SpecCategory
import com.example.data.queue.PasteQueueManager
import com.example.ui.theme.PrimaryCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneDetailScreen(
    phoneWithFields: PhoneWithFields?,
    onBack: () -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit,
    onTogglePin: (Long, Boolean) -> Unit,
    onFieldUsed: (PhoneField) -> Unit,
    onUpdateField: (PhoneField) -> Unit
) {
    BackHandler { onBack() }

    if (phoneWithFields == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Phone record not found")
        }
        return
    }

    val phone = phoneWithFields.phone
    val fields = phoneWithFields.fields
    val context = LocalContext.current

    var selectedCategory by remember { mutableStateOf(SpecCategory.ALL) }
    var rawTsvDialogVisible by remember { mutableStateOf(false) }
    var editingField by remember { mutableStateOf<PhoneField?>(null) }

    val filteredFields = remember(fields, selectedCategory) {
        if (selectedCategory == SpecCategory.ALL) {
            fields.sortedBy { it.columnIndex }
        } else {
            fields.filter {
                it.category.equals(selectedCategory.name, ignoreCase = true) ||
                        it.category.equals(selectedCategory.title, ignoreCase = true)
            }.sortedBy { it.columnIndex }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = phone.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${phone.brand} • ${phone.model}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { rawTsvDialogVisible = true },
                        modifier = Modifier.testTag("view_raw_tsv_button")
                    ) {
                        Icon(imageVector = Icons.Default.Code, contentDescription = "View Raw TSV")
                    }
                    IconButton(
                        onClick = {
                            val allSpecs = fields.joinToString("\n") { "${it.fieldName}: ${it.fieldValue}" }
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("All Specs", allSpecs))
                            Toast.makeText(context, "Copied all specs!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("copy_all_specs_button")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy all specs")
                    }
                    IconButton(
                        onClick = { onTogglePin(phone.id, phone.isPinned) },
                        modifier = Modifier.testTag("detail_pin_button")
                    ) {
                        Icon(
                            imageVector = if (phone.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin phone",
                            tint = if (phone.isPinned) PrimaryCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onToggleFavorite(phone.id, phone.isFavorite) },
                        modifier = Modifier.testTag("detail_fav_button")
                    ) {
                        Icon(
                            imageVector = if (phone.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite phone",
                            tint = if (phone.isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("category_filter_row"),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SpecCategory.entries.toTypedArray()) { cat ->
                    val isSelected = selectedCategory == cat
                    val count = if (cat == SpecCategory.ALL) fields.size else {
                        fields.count {
                            it.category.equals(cat.name, ignoreCase = true) ||
                                    it.category.equals(cat.title, ignoreCase = true)
                        }
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text("${cat.title} ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = cat.color.copy(alpha = 0.25f),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("cat_chip_${cat.name}")
                    )
                }
            }

            // Specs List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("fields_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredFields, key = { it.id }) { field ->
                    FieldSpecCard(
                        field = field,
                        onCopy = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText(field.fieldName, field.fieldValue))
                            Toast.makeText(context, "Copied: ${field.fieldName}", Toast.LENGTH_SHORT).show()
                            onFieldUsed(field)
                        },
                        onAddToQueue = {
                            PasteQueueManager.add(
                                label = field.fieldName,
                                value = field.fieldValue,
                                sourcePhone = phone.fullName
                            )
                            Toast.makeText(context, "Added to queue: ${field.fieldName}", Toast.LENGTH_SHORT).show()
                            onFieldUsed(field)
                        },
                        onEdit = {
                            editingField = field
                        }
                    )
                }
            }
        }
    }

    // Raw TSV Dialog
    if (rawTsvDialogVisible) {
        AlertDialog(
            onDismissRequest = { rawTsvDialogVisible = false },
            title = { Text("Original TSV Row", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Raw tab-separated values stored for this smartphone:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = phone.originalTsv.ifBlank { "No raw TSV row saved." },
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("Raw TSV", phone.originalTsv))
                        Toast.makeText(context, "Raw TSV copied to clipboard", Toast.LENGTH_SHORT).show()
                        rawTsvDialogVisible = false
                    },
                    modifier = Modifier.testTag("copy_raw_tsv_button")
                ) {
                    Text("Copy TSV")
                }
            },
            dismissButton = {
                TextButton(onClick = { rawTsvDialogVisible = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Edit Field Value Dialog
    editingField?.let { fieldToEdit ->
        var editVal by remember { mutableStateOf(fieldToEdit.fieldValue) }
        AlertDialog(
            onDismissRequest = { editingField = null },
            title = { Text("Edit: ${fieldToEdit.fieldName}", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editVal,
                    onValueChange = { editVal = it },
                    label = { Text("Field Value") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateField(fieldToEdit.copy(fieldValue = editVal))
                        editingField = null
                    },
                    modifier = Modifier.testTag("save_field_edit_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingField = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FieldSpecCard(
    field: PhoneField,
    onCopy: () -> Unit,
    onAddToQueue: () -> Unit,
    onEdit: () -> Unit
) {
    val category = SpecCategory.fromString(field.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("field_card_${field.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category color badge + Field Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = category.color,
                        modifier = Modifier
                            .width(4.dp)
                            .height(16.dp)
                    ) {}
                    Text(
                        text = field.fieldName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Category pill tag
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = category.color.copy(alpha = 0.2f),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = category.color,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Field Value
            Text(
                text = field.fieldValue.ifBlank { "—" },
                style = MaterialTheme.typography.bodyMedium,
                color = if (field.fieldValue.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            // Bottom action row: Copy, Add-to-queue, Edit, Usage badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (field.usageCount > 0) {
                    Text(
                        text = "Used ${field.usageCount}x",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp).testTag("edit_field_${field.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit value",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onAddToQueue,
                        modifier = Modifier.size(32.dp).testTag("queue_field_${field.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add to paste queue",
                            modifier = Modifier.size(18.dp),
                            tint = PrimaryCyan
                        )
                    }

                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(32.dp).testTag("copy_field_${field.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy field value",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
