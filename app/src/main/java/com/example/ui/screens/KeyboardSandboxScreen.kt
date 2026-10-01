package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.repository.SpecRepository
import com.example.ime.ImeKeyboardScreen
import com.example.ui.theme.PrimaryCyan

@Composable
fun KeyboardSandboxScreen(
    repository: SpecRepository
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var testFieldValue by remember { mutableStateOf(TextFieldValue("")) }

    // System IME status checks
    var isImeEnabled by remember { mutableStateOf(false) }
    var isImeSelected by remember { mutableStateOf(false) }

    fun checkImeStatus() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return
        val enabledMethods = imm.enabledInputMethodList
        val packageName = context.packageName

        isImeEnabled = enabledMethods.any { it.packageName == packageName }

        val currentIme = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        ) ?: ""
        isImeSelected = currentIme.contains(packageName)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkImeStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        checkImeStatus()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
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
                        Text(
                            text = "System IME Status",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        TextButton(
                            onClick = { checkImeStatus() },
                            modifier = Modifier.testTag("refresh_ime_status_button")
                        ) {
                            Text("Refresh", fontSize = 12.sp)
                        }
                    }

                    // Enabled indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isImeEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isImeEnabled) Color(0xFF10B981) else Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isImeEnabled) "Enabled in Android Settings" else "Not enabled in Android Settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isImeEnabled) Color(0xFF10B981) else Color(0xFFF59E0B),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Active indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isImeSelected) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isImeSelected) PrimaryCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isImeSelected) "Currently Active System Keyboard" else "Not currently active keyboard",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isImeSelected) PrimaryCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Action buttons to enable & switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_ime_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("1. Enable IME", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                imm?.showInputMethodPicker()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("switch_ime_picker_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("2. Switch IME", fontSize = 11.sp, color = Color(0xFF0F172A))
                        }
                    }
                }
            }

            // Interactive Test Text Field
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Interactive Test Sandbox",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (testFieldValue.text.isNotEmpty()) {
                    TextButton(
                        onClick = { testFieldValue = TextFieldValue("") },
                        modifier = Modifier.testTag("clear_sandbox_text_button")
                    ) {
                        Text("Clear", fontSize = 12.sp)
                    }
                }
            }

            OutlinedTextField(
                value = testFieldValue,
                onValueChange = { testFieldValue = it },
                placeholder = {
                    Text(
                        "Tap keys, brands, or specs in the keyboard below to test insertion...",
                        fontSize = 12.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .testTag("sandbox_test_field"),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Embedded IME Simulator
        Surface(
            color = Color(0xFF0F172A),
            modifier = Modifier.fillMaxWidth()
        ) {
            ImeKeyboardScreen(
                repository = repository,
                onInsertText = { textToInsert ->
                    val text = testFieldValue.text
                    val selection = testFieldValue.selection
                    val newText = text.replaceRange(selection.start, selection.end, textToInsert)
                    val newCursor = selection.start + textToInsert.length
                    testFieldValue = TextFieldValue(newText, TextRange(newCursor))
                },
                onBackspace = {
                    val text = testFieldValue.text
                    val selection = testFieldValue.selection
                    if (selection.start != selection.end) {
                        val newText = text.removeRange(selection.start, selection.end)
                        testFieldValue = TextFieldValue(newText, TextRange(selection.start))
                    } else if (selection.start > 0) {
                        val newText = text.removeRange(selection.start - 1, selection.start)
                        testFieldValue = TextFieldValue(newText, TextRange(selection.start - 1))
                    }
                },
                onEnter = {
                    val text = testFieldValue.text
                    val selection = testFieldValue.selection
                    val newText = text.replaceRange(selection.start, selection.end, "\n")
                    testFieldValue = TextFieldValue(newText, TextRange(selection.start + 1))
                },
                onSpace = {
                    val text = testFieldValue.text
                    val selection = testFieldValue.selection
                    val newText = text.replaceRange(selection.start, selection.end, " ")
                    testFieldValue = TextFieldValue(newText, TextRange(selection.start + 1))
                },
                onSwitchIme = {
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.showInputMethodPicker()
                },
                onHideKeyboard = {
                    // In sandbox, no-op or clear field
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
