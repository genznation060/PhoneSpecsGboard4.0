package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryCyan
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    brandColIdx: Int,
    modelColIdx: Int,
    fullNameColIdx: Int,
    onUpdateColumnMapping: (Int, Int, Int) -> Unit,
    onLoadSamples: () -> Unit,
    onClearAllData: () -> Unit,
    onExportJson: suspend () -> String,
    onExportTsv: suspend () -> String,
    onExportCsv: suspend () -> String,
    onRestoreJson: (String, (Int) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showClearDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonText by remember { mutableStateOf("") }

    var editBrandIdx by remember(brandColIdx) { mutableIntStateOf(brandColIdx) }
    var editModelIdx by remember(modelColIdx) { mutableIntStateOf(modelColIdx) }
    var editFullNameIdx by remember(fullNameColIdx) { mutableIntStateOf(fullNameColIdx) }

    fun shareText(title: String, text: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title)
        context.startActivity(shareIntent)
    }

    fun copyToClipboard(label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Column Mapping
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewColumn,
                        contentDescription = null,
                        tint = PrimaryCyan
                    )
                    Text(
                        text = "TSV Column Mapping",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Configure zero-based column indexes used to resolve Brand, Model, and Full Name from TSV rows.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editBrandIdx.toString(),
                        onValueChange = { editBrandIdx = it.toIntOrNull() ?: 0 },
                        label = { Text("Brand", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).testTag("brand_col_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editModelIdx.toString(),
                        onValueChange = { editModelIdx = it.toIntOrNull() ?: 1 },
                        label = { Text("Model", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).testTag("model_col_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editFullNameIdx.toString(),
                        onValueChange = { editFullNameIdx = it.toIntOrNull() ?: 2 },
                        label = { Text("Full Name", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).testTag("fullname_col_input"),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            onUpdateColumnMapping(editBrandIdx, editModelIdx, editFullNameIdx)
                            Toast.makeText(context, "Column mapping saved", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("save_col_mapping_button")
                    ) {
                        Text("Save Mapping")
                    }
                }
            }
        }

        // Section: Sample Data & Database Management
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Data Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onLoadSamples,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_load_samples_button")
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Load 5 Sample Flagships (S26 Ultra, X300 Pro, iPhone 18...)")
                }

                OutlinedButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_clear_all_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Entire Database", color = Color(0xFFEF4444))
                }
            }
        }

        // Section: Export & Backup
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = null,
                        tint = Color(0xFF10B981)
                    )
                    Text(
                        text = "Export & Backup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Export all smartphone specifications to share or back up.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Export TSV
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val tsv = onExportTsv()
                                copyToClipboard("TSV Export", tsv)
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("export_tsv_copy_button")
                    ) {
                        Text("Copy TSV")
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val tsv = onExportTsv()
                                shareText("SpecBoard TSV Export", tsv)
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("export_tsv_share_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share TSV")
                    }
                }

                // Export CSV
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val csv = onExportCsv()
                                copyToClipboard("CSV Export", csv)
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("export_csv_copy_button")
                    ) {
                        Text("Copy CSV")
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val csv = onExportCsv()
                                shareText("SpecBoard CSV Export", csv)
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("export_csv_share_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share CSV")
                    }
                }

                // Export JSON & Restore JSON
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val json = onExportJson()
                                copyToClipboard("JSON Export", json)
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("export_json_copy_button")
                    ) {
                        Text("Copy JSON")
                    }

                    Button(
                        onClick = { showRestoreDialog = true },
                        modifier = Modifier.weight(1f).testTag("restore_json_button")
                    ) {
                        Text("Restore JSON")
                    }
                }
            }
        }

        // Section: System Keyboard Instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = null,
                        tint = PrimaryCyan
                    )
                    Text(
                        text = "System Keyboard Guide",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "SpecBoard includes a real system InputMethodService keyboard that works in any app (WhatsApp, ChatGPT, Gmail, Docs, etc.):\n\n" +
                            "1. Open Android Settings › System › Languages & Input › On-screen keyboard › Manage Keyboards.\n" +
                            "2. Toggle ON 'SpecBoard Keyboard'.\n" +
                            "3. When typing anywhere, tap the keyboard switcher icon in your navigation bar or space bar to pick SpecBoard Keyboard.\n" +
                            "4. Tap brands or phone cards to insert individual specs or use the Queue to paste multiple specs formatted!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Button(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                    },
                    modifier = Modifier.testTag("open_ime_settings_from_settings")
                ) {
                    Text("Open Android Keyboard Settings")
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }

    // Confirm Clear All Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444))
                    Text("Clear All Data?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("This will permanently remove all smartphones, spec fields, and clipboard history from the local database.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.testTag("confirm_clear_all_button")
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore JSON Dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore From JSON Backup", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paste previously exported SpecBoard JSON array below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = restoreJsonText,
                        onValueChange = { restoreJsonText = it },
                        placeholder = { Text("[{\"brand\":\"Samsung\", ...}]", fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("restore_json_input"),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreJson(restoreJsonText) {
                            showRestoreDialog = false
                            restoreJsonText = ""
                        }
                    },
                    enabled = restoreJsonText.isNotBlank(),
                    modifier = Modifier.testTag("confirm_restore_json_button")
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
