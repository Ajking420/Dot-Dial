package com.example.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CallRecordingEntity
import com.example.data.system.T9Utils
import com.example.ui.components.AudioGlyphVisualizer
import com.example.ui.components.DotMatrixTagChip
import com.example.ui.components.NothingSurfaceCard
import com.example.ui.components.SectionHeaderRow
import com.example.viewmodel.RecordingSummaryUiState
import org.json.JSONArray

@Composable
fun VaultScreen(
    recordings: List<CallRecordingEntity>,
    vaultPinSet: Boolean,
    isUnlocked: Boolean,
    playingRecordingId: Long?,
    summaryState: RecordingSummaryUiState,
    onUnlockWithPin: (String) -> Boolean,
    onLockVault: () -> Unit,
    onTogglePlay: (CallRecordingEntity) -> Unit,
    onSummarizeRecording: (Long, String?) -> Unit,
    onDeleteRecording: (CallRecordingEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (vaultPinSet && !isUnlocked) {
        VaultPinGate(
            onUnlockAttempt = onUnlockWithPin,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        SectionHeaderRow(
            title = "PRIVACY RECORDING VAULT",
            subtitle = "On-device encrypted-scope storage • Gemini auto-summaries",
            trailing = {
                if (vaultPinSet) {
                    IconButton(
                        onClick = onLockVault,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("lock_vault_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "Lock Vault",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Local privacy indicator",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Audio recordings never auto-sync to cloud. Only transcripts or notes you choose to summarize are sent to Gemini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            if (recordings.isEmpty()) {
                item {
                    NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Empty vault",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "VAULT EMPTY",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap Record during any call (or Test Ring) to save private audio & generate automatic Gemini conversation summaries.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(recordings, key = { it.id }) { rec ->
                    RecordingVaultCard(
                        recording = rec,
                        isPlaying = playingRecordingId == rec.id,
                        summaryState = summaryState,
                        onTogglePlay = { onTogglePlay(rec) },
                        onSummarize = { notes -> onSummarizeRecording(rec.id, notes) },
                        onDelete = { onDeleteRecording(rec) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VaultPinGate(
    onUnlockAttempt: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    var pinInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Vault locked",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.vault_locked_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.vault_locked_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))
        OutlinedTextField(
            value = pinInput,
            onValueChange = {
                pinInput = it.filter { c -> c.isDigit() }.take(4)
                errorText = null
            },
            label = { Text("4-Digit PIN") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .testTag("vault_pin_input")
        )
        if (errorText != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = errorText!!,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = {
                if (!onUnlockAttempt(pinInput)) {
                    errorText = "Incorrect PIN. Try again."
                }
            },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.testTag("unlock_vault_button")
        ) {
            Text("UNLOCK VAULT", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun RecordingVaultCard(
    recording: CallRecordingEntity,
    isPlaying: Boolean,
    summaryState: RecordingSummaryUiState,
    onTogglePlay: () -> Unit,
    onSummarize: (String) -> Unit,
    onDelete: () -> Unit
) {
    var notesDraft by remember(recording.id, recording.transcriptText) {
        mutableStateOf(recording.transcriptText)
    }
    var showNotesEditor by remember { mutableStateOf(false) }

    val isLoadingThis = summaryState is RecordingSummaryUiState.Loading &&
        summaryState.recordingId == recording.id
    val errorThis = (summaryState as? RecordingSummaryUiState.Error)
        ?.takeIf { it.recordingId == recording.id }
        ?.message

    val bullets = remember(recording.aiSummaryBulletsJson) {
        runCatching {
            val arr = JSONArray(recording.aiSummaryBulletsJson)
            (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }
    val actions = remember(recording.aiActionItemsJson) {
        runCatching {
            val arr = JSONArray(recording.aiActionItemsJson)
            (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    NothingSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recording_card_${recording.id}"),
        highlighted = isPlaying || recording.aiHeadline.isNotBlank()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.contactName.ifBlank { recording.phoneNumber },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val relTime = DateUtils.getRelativeTimeSpanString(
                        recording.createdAt,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    )
                    Text(
                        text = "$relTime  •  ${T9Utils.formatDuration(recording.durationSeconds)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (recording.filePath.isNotBlank()) {
                        IconButton(
                            onClick = onTogglePlay,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("play_recording_${recording.id}")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause recording" else "Play recording",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("delete_recording_${recording.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Shred recording",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (isPlaying) {
                AudioGlyphVisualizer(isActive = true)
            }

            if (recording.aiHeadline.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
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
                                text = "GEMINI SUMMARY",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            DotMatrixTagChip(label = recording.aiSentiment, active = false)
                        }
                        Text(
                            text = recording.aiHeadline,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        bullets.forEach { bullet ->
                            Text(
                                text = "• $bullet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (actions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "ACTION ITEMS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            actions.forEach { act ->
                                Text(
                                    text = "→ $act",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            if (showNotesEditor) {
                OutlinedTextField(
                    value = notesDraft,
                    onValueChange = { notesDraft = it },
                    label = { Text("Conversation Transcript / Notes") },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (errorThis != null) {
                Text(
                    text = errorThis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showNotesEditor = !showNotesEditor },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (showNotesEditor) "HIDE NOTES" else "EDIT NOTES",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Button(
                    onClick = { onSummarize(notesDraft) },
                    enabled = !isLoadingThis,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("summarize_recording_${recording.id}")
                ) {
                    if (isLoadingThis) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (recording.aiHeadline.isBlank()) "SUMMARIZE" else "RE-SUMMARIZE",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
