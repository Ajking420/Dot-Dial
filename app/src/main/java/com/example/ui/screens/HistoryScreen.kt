package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CallDirection
import com.example.data.local.CallLogEntity
import com.example.data.local.ContactEntity
import com.example.data.system.T9Utils
import com.example.ui.components.ContactAvatarCircle
import com.example.ui.components.DotMatrixTagChip
import com.example.ui.components.NothingSurfaceCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.theme.LocalDotDialStyle
import com.example.viewmodel.HistoryFilter
import com.example.viewmodel.PersonCallGroup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    logs: List<CallLogEntity>,
    groupedHistory: List<PersonCallGroup>,
    selectedFilter: HistoryFilter,
    onSelectFilter: (HistoryFilter) -> Unit,
    onCallNumber: (String, ContactEntity?) -> Unit,
    onVideoCallNumber: (String, ContactEntity?) -> Unit = { _, _ -> },
    onStatusMessage: (String) -> Unit = {},
    onDeleteLog: (Long) -> Unit,
    onClearHistory: () -> Unit,
    onExportJsonBackup: (Uri) -> Unit,
    onExportContactsVcf: (Uri) -> Unit,
    onRestoreBackupJson: (Uri) -> Unit,
    onOpenDialpadOverlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showBackupBar by remember { mutableStateOf(false) }
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }
    val listState = rememberLazyListState()
    val nowMs = remember(logs.size) { System.currentTimeMillis() }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportJsonBackup(uri)
    }

    val exportVcfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/vcard")
    ) { uri ->
        if (uri != null) onExportContactsVcf(uri)
    }

    val restoreJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onRestoreBackupJson(uri)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        SectionHeaderRow(
            title = "CALL HISTORY",
            subtitle = "${groupedHistory.size} people • ${logs.size} calls",
            trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { showBackupBar = !showBackupBar },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("history_backup_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = "Export or restore backup",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (logs.isNotEmpty()) {
                        IconButton(
                            onClick = onClearHistory,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("clear_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear history",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        )

        AnimatedVisibility(
            visible = showBackupBar,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            NothingSurfaceCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                highlighted = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "EXPORT & RESTORE CONTACTS + CALL HISTORY",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { exportJsonLauncher.launch("dot_dial_backup.json") },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f).testTag("export_json_backup_button")
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("EXPORT JSON", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { restoreJsonLauncher.launch(arrayOf("application/json", "*/*")) },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f).testTag("restore_json_backup_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RESTORE", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    OutlinedButton(
                        onClick = { exportVcfLauncher.launch("dot_dial_contacts.vcf") },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth().testTag("export_vcf_button")
                    ) {
                        Text("EXPORT CONTACTS AS VCARD (.VCF)", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryFilter.entries.forEach { filter ->
                    DotMatrixTagChip(
                        label = filter.label,
                        active = selectedFilter == filter,
                        onClick = { onSelectFilter(filter) },
                        testTag = "history_filter_${filter.label}"
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("history_open_dialpad_button"),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, LocalDotDialStyle.current.hairlineBorder),
                onClick = onOpenDialpadOverlay
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Dialpad,
                        contentDescription = "Show dialpad",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "DIALPAD",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 84.dp)
        ) {
            if (groupedHistory.isEmpty()) {
                item {
                    NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "No history",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "NO CALLS IN THIS FILTER",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Place a call, sync Call Log, or switch filter tabs above.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(
                    items = groupedHistory,
                    key = { it.groupKey },
                    contentType = { "person_history_group" }
                ) { group ->
                    val expanded = expandedGroups[group.groupKey] == true
                    PersonGroupedCallCard(
                        group = group,
                        expanded = expanded,
                        nowMs = nowMs,
                        onToggleExpand = {
                            expandedGroups[group.groupKey] = !expanded
                        },
                        onCallPerson = { onCallNumber(group.phoneNumber, group.matchedContact) },
                        onVideoCallPerson = { onVideoCallNumber(group.phoneNumber, group.matchedContact) },
                        onMessagePerson = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${group.phoneNumber}"))
                                )
                            }.onFailure {
                                onStatusMessage("No SMS messaging app found.")
                            }
                        },
                        onWhatsAppPerson = {
                            val digits = T9Utils.normalizePhone(group.phoneNumber).filter { it.isDigit() }
                            if (digits.isBlank()) {
                                onStatusMessage("Valid phone number required for WhatsApp.")
                            } else {
                                val waIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://wa.me/$digits")
                                )
                                runCatching {
                                    context.startActivity(waIntent)
                                }.onFailure {
                                    onStatusMessage("WhatsApp could not be opened on this device.")
                                }
                            }
                        },
                        onCallSpecificLog = { log -> onCallNumber(log.phoneNumber, group.matchedContact) },
                        onDeleteLog = onDeleteLog
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonGroupedCallCard(
    group: PersonCallGroup,
    expanded: Boolean,
    nowMs: Long,
    onToggleExpand: () -> Unit,
    onCallPerson: () -> Unit,
    onVideoCallPerson: () -> Unit,
    onMessagePerson: () -> Unit,
    onWhatsAppPerson: () -> Unit,
    onCallSpecificLog: (CallLogEntity) -> Unit,
    onDeleteLog: (Long) -> Unit
) {
    val latest = group.latestCall
    val style = LocalDotDialStyle.current
    val dirIcon = directionIcon(latest.direction)
    val dirTint = directionTint(latest.direction)
    val relTime = remember(latest.id, latest.timestamp, nowMs) {
        T9Utils.formatRelativeTimestamp(latest.timestamp, nowMs)
    }

    NothingSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_group_card_${group.groupKey}"),
        highlighted = group.hasSpam || expanded,
        onClick = onToggleExpand
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    ContactAvatarCircle(
                        name = group.displayName,
                        photoUri = group.matchedContact?.photoUri,
                        size = 42.dp,
                        highlighted = group.hasSpam || group.callCount > 1
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = group.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (group.callCount > 1) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                                ) {
                                    Text(
                                        text = "${group.callCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (group.hasSpam) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Spam alert",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = dirIcon,
                                contentDescription = latest.direction.name,
                                tint = dirTint,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "$relTime • ${T9Utils.formatDuration(latest.durationSeconds)}" +
                                    if (group.callCount > 1) " • Tap for ${group.callCount} calls" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("history_group_expand_${group.groupKey}")
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (expanded) "Collapse call list" else "Expand call list",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onCallPerson,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("history_group_call_${group.groupKey}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call ${group.displayName}",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = style.hairlineBorder)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onVideoCallPerson,
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("history_action_video_${group.groupKey}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Video call ${group.displayName}",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "VIDEO",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                        OutlinedButton(
                            onClick = onMessagePerson,
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("history_action_sms_${group.groupKey}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Message,
                                contentDescription = "Message ${group.displayName}",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "MSG",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                        OutlinedButton(
                            onClick = onWhatsAppPerson,
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("history_action_whatsapp_${group.groupKey}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "WhatsApp ${group.displayName}",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "WHATSAPP",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    }
                    Text(
                        text = if (group.callCount == 1) "CALL DETAILS (${group.phoneNumber})"
                        else "ALL ${group.callCount} CALLS (${group.phoneNumber})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    group.calls.forEach { entry ->
                        PersonCallDetailEntryRow(
                            log = entry,
                            onRedial = { onCallSpecificLog(entry) },
                            onDelete = { onDeleteLog(entry.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonCallDetailEntryRow(
    log: CallLogEntity,
    onRedial: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault()) }
    val dirLabel = when (log.direction) {
        CallDirection.INCOMING -> "Incoming"
        CallDirection.OUTGOING -> "Outgoing"
        CallDirection.MISSED -> "Missed"
        CallDirection.BLOCKED_SPAM -> "Blocked Spam"
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, LocalDotDialStyle.current.hairlineBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_detail_entry_${log.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = directionIcon(log.direction),
                    contentDescription = dirLabel,
                    tint = directionTint(log.direction),
                    modifier = Modifier.size(16.dp)
                )
                Column {
                    Text(
                        text = "$dirLabel • ${T9Utils.formatDuration(log.durationSeconds)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${dateFmt.format(Date(log.timestamp))} • ${log.phoneNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("history_entry_delete_${log.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Delete call log entry",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onRedial,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("history_entry_call_${log.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Redial ${log.phoneNumber}",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun directionIcon(direction: CallDirection) = when (direction) {
    CallDirection.INCOMING -> Icons.AutoMirrored.Filled.CallReceived
    CallDirection.OUTGOING -> Icons.AutoMirrored.Filled.CallMade
    CallDirection.MISSED -> Icons.AutoMirrored.Filled.CallMissed
    CallDirection.BLOCKED_SPAM -> Icons.Default.Security
}

@Composable
private fun directionTint(direction: CallDirection) = when (direction) {
    CallDirection.MISSED, CallDirection.BLOCKED_SPAM -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
