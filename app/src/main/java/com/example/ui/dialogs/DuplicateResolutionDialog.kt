package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.DuplicateAction
import com.example.data.repository.DuplicateConflict

@Composable
fun DuplicateResolutionDialog(
    conflict: DuplicateConflict,
    totalRemaining: Int,
    onAction: (DuplicateAction) -> Unit,
    onApplyAllRemaining: (DuplicateAction) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B)
                )
                Text(
                    text = "Duplicate Phone ($totalRemaining left)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "An existing phone with a matching name or brand/model was found in your database.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "EXISTING IN DATABASE:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = conflict.existing.fullName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${conflict.existing.brand} • ${conflict.existing.model}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "INCOMING FROM TSV:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = conflict.incoming.record.fullName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${conflict.incoming.record.brand} • ${conflict.incoming.record.model} (${conflict.incoming.fields.size} fields)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onAction(DuplicateAction.UPDATE) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("action_update_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("UPDATE (Merge non-blank fields)")
                    }

                    Button(
                        onClick = { onAction(DuplicateAction.REPLACE) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("action_replace_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text("REPLACE (Overwrite entire phone)")
                    }

                    OutlinedButton(
                        onClick = { onAction(DuplicateAction.ADD_AS_NEW) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("action_add_new_button")
                    ) {
                        Text("ADD AS NEW (Keep both)")
                    }

                    OutlinedButton(
                        onClick = { onAction(DuplicateAction.SKIP) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("action_skip_button")
                    ) {
                        Text("SKIP (Ignore incoming row)")
                    }

                    if (totalRemaining > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(
                                onClick = { onApplyAllRemaining(DuplicateAction.UPDATE) },
                                modifier = Modifier.testTag("apply_all_update_button")
                            ) {
                                Text("Update All ($totalRemaining)", fontSize = 12.sp)
                            }
                            TextButton(
                                onClick = { onApplyAllRemaining(DuplicateAction.SKIP) },
                                modifier = Modifier.testTag("apply_all_skip_button")
                            ) {
                                Text("Skip All ($totalRemaining)", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}
