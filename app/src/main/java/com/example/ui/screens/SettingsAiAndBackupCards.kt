package com.example.ui.screens

import android.net.Uri
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ai.AssistantReplyLanguage
import com.example.data.preferences.AiAssistantLanguageMode
import com.example.data.preferences.AiAssistantTriggerMode
import com.example.data.preferences.DialerSettings
import com.example.data.system.GoogleDriveBackupManager
import com.example.ui.components.DotMatrixTagChip
import com.example.ui.components.NothingSurfaceCard
import com.example.ui.theme.LocalDotDialStyle
import com.example.viewmodel.GoogleDriveUiState

private val RingDelayPresets = listOf(2, 5, 8, 12, 15)

@Composable
fun AiCallAssistantSettingsCard(
    settings: DialerSettings,
    onSetAiAssistantEnabled: (Boolean) -> Unit,
    onSetAiBusyMode: (Boolean) -> Unit,
    onSetAiOwnerName: (String) -> Unit,
    onSetAiTriggerMode: (AiAssistantTriggerMode) -> Unit,
    onSetAiDelaySeconds: (Int) -> Unit,
    onSetAiLanguageMode: (AiAssistantLanguageMode) -> Unit,
    onSetAiAutoRecord: (Boolean) -> Unit,
    onPreviewAiVoice: (AssistantReplyLanguage) -> Unit
) {
    val style = LocalDotDialStyle.current
    var ownerDraft by remember(settings.aiAssistantOwnerName) {
        mutableStateOf(settings.aiAssistantOwnerName)
    }

    NothingSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_assistant_settings_card"),
        highlighted = settings.aiAssistantEnabled
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "Gemini AI Call Assistant",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GEMINI AI CALL ASSISTANT",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            SettingsSwitchRow(
                title = "Enable AI Call Assistant",
                subtitle = "Answers incoming calls with Gemini, converses with caller, records & saves summary to Vault",
                checked = settings.aiAssistantEnabled,
                onCheckedChange = onSetAiAssistantEnabled,
                testTag = "ai_assistant_switch"
            )

            SettingsSwitchRow(
                title = "Busy Mode (Quick Toggle)",
                subtitle = "Marks you busy right now for 'When Busy Mode On' auto-answer",
                checked = settings.aiBusyModeEnabled,
                onCheckedChange = onSetAiBusyMode,
                testTag = "ai_busy_mode_switch"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = ownerDraft,
                    onValueChange = { ownerDraft = it },
                    label = {
                        Text(
                            "Owner Name (blank = Google / Device name)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_owner_name_input")
                )
                Button(
                    onClick = { onSetAiOwnerName(ownerDraft.trim()) },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("ai_owner_name_save_button")
                ) {
                    Text("SAVE", style = MaterialTheme.typography.labelMedium)
                }
            }

            Text(
                text = "AUTO-ANSWER TRIGGER",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiAssistantTriggerMode.entries.forEach { mode ->
                    DotMatrixTagChip(
                        label = mode.label,
                        active = settings.aiAssistantTriggerMode == mode,
                        onClick = { onSetAiTriggerMode(mode) },
                        testTag = "ai_trigger_${mode.id}"
                    )
                }
            }

            AnimatedVisibility(
                visible = settings.aiAssistantTriggerMode == AiAssistantTriggerMode.AFTER_DELAY ||
                    settings.aiAssistantTriggerMode == AiAssistantTriggerMode.UNKNOWN_ONLY
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "RING DELAY BEFORE AI ANSWERS (${settings.aiAssistantDelaySeconds}s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RingDelayPresets.forEach { sec ->
                            DotMatrixTagChip(
                                label = "${sec}s",
                                active = settings.aiAssistantDelaySeconds == sec,
                                onClick = { onSetAiDelaySeconds(sec) },
                                testTag = "ai_delay_${sec}s"
                            )
                        }
                    }
                }
            }

            Text(
                text = "SPEAKING VOICE & LANGUAGE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiAssistantLanguageMode.entries.forEach { langMode ->
                    DotMatrixTagChip(
                        label = langMode.label,
                        active = settings.aiAssistantLanguageMode == langMode,
                        onClick = { onSetAiLanguageMode(langMode) },
                        testTag = "ai_lang_${langMode.id}"
                    )
                }
            }

            Text(
                text = "PREVIEW AI VOICE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DotMatrixTagChip(
                    label = "Test EN",
                    active = false,
                    onClick = { onPreviewAiVoice(AssistantReplyLanguage.EN) },
                    testTag = "preview_voice_en"
                )
                DotMatrixTagChip(
                    label = "Test HI (हिंदी)",
                    active = false,
                    onClick = { onPreviewAiVoice(AssistantReplyLanguage.HI) },
                    testTag = "preview_voice_hi"
                )
                DotMatrixTagChip(
                    label = "Test BN (বাংলা)",
                    active = false,
                    onClick = { onPreviewAiVoice(AssistantReplyLanguage.BN) },
                    testTag = "preview_voice_bn"
                )
            }

            HorizontalDivider(color = style.hairlineBorder)

            SettingsSwitchRow(
                title = "Record AI Calls & Save Summary",
                subtitle = "Automatically records AI-handled calls and writes transcript + Gemini summary to Vault",
                checked = settings.aiAssistantAutoRecord,
                onCheckedChange = onSetAiAutoRecord,
                testTag = "ai_auto_record_switch"
            )
        }
    }
}

@Composable
fun GoogleDriveAndBackupSettingsCard(
    settings: DialerSettings,
    driveUiState: GoogleDriveUiState,
    onConnectDrive: () -> Unit,
    onDisconnectDrive: () -> Unit,
    onBackupToDrive: (Boolean, Boolean) -> Unit,
    onRefreshDriveBackups: () -> Unit,
    onRestoreFromDrive: (String, Boolean, Boolean) -> Unit,
    onExportFullBackupJson: (Uri) -> Unit,
    onRestoreFullBackupJson: (Uri) -> Unit,
    onExportSettingsJson: (Uri) -> Unit,
    onRestoreSettingsJson: (Uri) -> Unit,
    onExportContactsVcf: (Uri) -> Unit
) {
    val context = LocalContext.current
    val style = LocalDotDialStyle.current

    val exportFullJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) onExportFullBackupJson(uri) }

    val restoreFullJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) onRestoreFullBackupJson(uri) }

    val exportSettingsJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) onExportSettingsJson(uri) }

    val restoreSettingsJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) onRestoreSettingsJson(uri) }

    val exportVcfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/vcard")
    ) { uri -> if (uri != null) onExportContactsVcf(uri) }

    val activeEmail = driveUiState.connectedEmail.ifBlank { settings.driveConnectedEmail }
    val isConnected = activeEmail.isNotBlank()

    NothingSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("backup_restore_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Google Drive Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                        contentDescription = "Google Drive Backup",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "GOOGLE DRIVE BACKUP & RESTORE",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isConnected) {
                                "$activeEmail • Folder: ${GoogleDriveBackupManager.DRIVE_FOLDER_DISPLAY_NAME}"
                            } else {
                                "Visible Drive folder: ${GoogleDriveBackupManager.DRIVE_FOLDER_DISPLAY_NAME}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (isConnected) {
                    TextButton(
                        onClick = onDisconnectDrive,
                        modifier = Modifier.testTag("drive_disconnect_button")
                    ) {
                        Text("SIGN OUT", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (driveUiState.isAuthorizing || driveUiState.isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (settings.lastDriveBackupAtMs > 0L) {
                val rel = DateUtils.getRelativeTimeSpanString(
                    settings.lastDriveBackupAtMs,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                )
                Text(
                    text = "Last Drive backup: $rel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (driveUiState.statusDetail.isNotBlank()) {
                Text(
                    text = driveUiState.statusDetail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!driveUiState.errorMessage.isNullOrBlank()) {
                Text(
                    text = driveUiState.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onBackupToDrive(true, true) },
                    enabled = !driveUiState.isAuthorizing && !driveUiState.isBusy,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("drive_full_backup_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isConnected) "DRIVE: FULL BACKUP (ALL)" else "CONNECT & BACKUP DRIVE",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = if (isConnected) onRefreshDriveBackups else onConnectDrive,
                    enabled = !driveUiState.isAuthorizing && !driveUiState.isBusy,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("drive_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Google Drive backups",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onBackupToDrive(true, false) },
                    enabled = !driveUiState.isAuthorizing && !driveUiState.isBusy,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("drive_backup_data_button")
                ) {
                    Text(
                        "DRIVE: LOGS & CONTACTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = { onBackupToDrive(false, true) },
                    enabled = !driveUiState.isAuthorizing && !driveUiState.isBusy,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("drive_backup_settings_button")
                ) {
                    Text(
                        "DRIVE: ALL SETTINGS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (driveUiState.availableBackups.isNotEmpty()) {
                Text(
                    text = "RESTORE FROM '${GoogleDriveBackupManager.DRIVE_FOLDER_DISPLAY_NAME}'",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    driveUiState.availableBackups.forEach { file ->
                        val includesData = file.name != GoogleDriveBackupManager.FILE_SETTINGS_ONLY
                        val includesSettings = file.name != GoogleDriveBackupManager.FILE_CONTACTS_AND_HISTORY
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, style.hairlineBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val sizeLabel = if (file.sizeBytes > 0L) {
                                        Formatter.formatShortFileSize(context, file.sizeBytes)
                                    } else {
                                        "JSON"
                                    }
                                    val timeLabel = file.modifiedTime.take(16).replace("T", " ")
                                    Text(
                                        text = if (timeLabel.isNotBlank()) "$sizeLabel • $timeLabel" else sizeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        onRestoreFromDrive(file.id, includesData, includesSettings)
                                    },
                                    enabled = !driveUiState.isBusy,
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.testTag("drive_restore_${file.name}")
                                ) {
                                    Text("RESTORE", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = style.hairlineBorder)

            // Local File Backup & App Settings Export/Restore
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Backup,
                    contentDescription = "Local device backup",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LOCAL FILE BACKUP & APP SETTINGS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { exportFullJsonLauncher.launch("dot_dial_full_backup.json") },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("local_export_full_button")
                ) {
                    Text("EXPORT FULL (.JSON)", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = { restoreFullJsonLauncher.launch(arrayOf("application/json", "*/*")) },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("local_restore_full_button")
                ) {
                    Text(
                        "RESTORE FULL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { exportSettingsJsonLauncher.launch("dot_dial_settings.json") },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("local_export_settings_button")
                ) {
                    Text(
                        "EXPORT SETTINGS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                OutlinedButton(
                    onClick = { restoreSettingsJsonLauncher.launch(arrayOf("application/json", "*/*")) },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("local_restore_settings_button")
                ) {
                    Text(
                        "RESTORE SETTINGS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            OutlinedButton(
                onClick = { exportVcfLauncher.launch("dot_dial_contacts.vcf") },
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "EXPORT CONTACTS (.VCF VCARD)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
