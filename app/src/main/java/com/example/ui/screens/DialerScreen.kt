package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.CallDirection
import com.example.data.local.CallLogEntity
import com.example.data.local.ContactEntity
import com.example.data.system.DeviceNameHelper
import com.example.data.system.T9Utils
import com.example.ui.components.ContactAvatarCircle
import com.example.ui.components.DotKeypadButton
import com.example.ui.components.DotMatrixTagChip
import com.example.ui.components.NothingSurfaceCard
import com.example.ui.components.PulsingDotIndicator
import com.example.ui.theme.LocalDotDialStyle

private val DialpadRows = listOf(
    listOf("1" to "∞", "2" to "ABC", "3" to "DEF"),
    listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
    listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
    listOf("*" to "", "0" to "+", "#" to "")
)

@Composable
fun DialerScreen(
    dialerQuery: String,
    isDialpadVisible: Boolean,
    predictiveMatches: List<ContactEntity>,
    recentLogs: List<CallLogEntity>,
    contacts: List<ContactEntity>,
    isDefaultDialer: Boolean,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    onRequestDefaultDialer: () -> Unit,
    onToggleDialpad: () -> Unit,
    onDigitPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onClearQuery: () -> Unit,
    onPlaceCall: (String, ContactEntity?) -> Unit,
    onSimulateIncoming: (Boolean) -> Unit,
    onOpenAiCommand: () -> Unit,
    aiAssistantEnabled: Boolean = false,
    aiBusyModeEnabled: Boolean = false,
    onToggleBusyMode: (Boolean) -> Unit = {},
    signedInName: String = "",
    signedInEmail: String = "",
    onGoogleAccountClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val style = LocalDotDialStyle.current
    val brandHeaderText = remember(context) {
        DeviceNameHelper.resolveDialerBrandHeader(context)
    }
    val contactByNumber = remember(contacts) {
        T9Utils.buildNumberLookup(contacts)
    }

    val recentListState = rememberLazyListState()
    val predictiveListState = rememberLazyListState()
    val nowMs = remember(recentLogs.size) { System.currentTimeMillis() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Utility Bar
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PulsingDotIndicator(size = 8.dp, active = false)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = brandHeaderText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("dialer_brand_header")
                    )
                }
                Row(
                    modifier = Modifier
                        .weight(1.25f, fill = false)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val userBadgeLabel = when {
                        signedInName.isNotBlank() -> signedInName.substringBefore(" ").uppercase().take(10)
                        signedInEmail.isNotBlank() -> signedInEmail.substringBefore("@").uppercase().take(10)
                        else -> "SIGN IN"
                    }
                    DotMatrixTagChip(
                        label = userBadgeLabel,
                        active = signedInEmail.isNotBlank(),
                        onClick = onGoogleAccountClick,
                        testTag = "dialer_google_account_chip"
                    )
                    if (aiAssistantEnabled) {
                        DotMatrixTagChip(
                            label = if (aiBusyModeEnabled) "BUSY AI" else "AI AUTO",
                            active = aiBusyModeEnabled,
                            onClick = { onToggleBusyMode(!aiBusyModeEnabled) },
                            testTag = "dialer_ai_busy_chip"
                        )
                    }
                }
            }

            if (!hasPermissions || !isDefaultDialer) {
                Spacer(modifier = Modifier.height(8.dp))
                NothingSurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    highlighted = !hasPermissions
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (!hasPermissions) stringResource(R.string.permission_banner_title)
                                else "DEFAULT DIALER ROLE",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (!hasPermissions) "Enable Contacts, Call Log, Mic & Bluetooth"
                                else "Set Dot Dial as default for hardware call intercept",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = if (!hasPermissions) onRequestPermissions else onRequestDefaultDialer,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("grant_system_access_button")
                        ) {
                            Text(
                                text = if (!hasPermissions) "GRANT" else "ENABLE",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }

        // Predictive T9 Matches or Call History Strip + Hide/Unhide Dialpad Toggle
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (dialerQuery.isNotBlank()) "PREDICTIVE T9 (${predictiveMatches.size})"
                    else "CALL HISTORY (${recentLogs.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onToggleDialpad)
                        .testTag("dialpad_hide_unhide_button"),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, style.hairlineBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isDialpadVisible) Icons.Default.KeyboardArrowDown
                            else Icons.Default.KeyboardArrowUp,
                            contentDescription = if (isDialpadVisible) "Hide dialpad" else "Unhide dialpad",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isDialpadVisible) "HIDE PAD" else "SHOW PAD",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (dialerQuery.isNotBlank()) {
                LazyColumn(
                    state = predictiveListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (predictiveMatches.isEmpty()) {
                        item {
                            NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Direct Dial: $dialerQuery",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Tap the Call key to place call",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Direct dial",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = predictiveMatches,
                            key = { it.id },
                            contentType = { "predictive_contact" }
                        ) { contact ->
                            PredictiveContactRow(
                                contact = contact,
                                onCall = { onPlaceCall(contact.phoneNumber, contact) }
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = recentListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    if (recentLogs.isEmpty()) {
                        item {
                            NothingSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneCallback,
                                        contentDescription = "No recent calls",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "MATRIX LOG CLEAR",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Dial below, sync Contacts, or tap POPUP RING to preview the top floating caller popup.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = recentLogs.take(30),
                            key = { it.id },
                            contentType = { "recent_call" }
                        ) { log ->
                            val matchedContact = contactByNumber[log.normalizedNumber]
                            RecentCallRow(
                                log = log,
                                photoUri = matchedContact?.photoUri,
                                nowMs = nowMs,
                                onCall = { onPlaceCall(log.phoneNumber, matchedContact) }
                            )
                        }
                    }
                }
            }
        }

        // Animated Collapsible Dot-Matrix Dialpad
        AnimatedVisibility(
            visible = isDialpadVisible,
            enter = fadeIn() + expandVertically(animationSpec = spring(dampingRatio = 0.78f, stiffness = 360f)),
            exit = fadeOut() + shrinkVertically(animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.dp, style.hairlineBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onOpenAiCommand,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("dialer_ai_voice_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Open Gemini AI Control",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = dialerQuery.ifBlank { stringResource(R.string.dialer_placeholder) },
                            style = if (dialerQuery.isBlank()) {
                                MaterialTheme.typography.labelMedium
                            } else {
                                MaterialTheme.typography.displayMedium.copy(fontSize = 26.sp)
                            },
                            color = if (dialerQuery.isBlank()) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )

                        AnimatedVisibility(
                            visible = dialerQuery.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            IconButton(
                                onClick = onBackspace,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("keypad_backspace_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Backspace digit",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DialpadRows.forEach { row ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                row.forEach { (digit, letters) ->
                                    DotKeypadButton(
                                        digit = digit,
                                        letters = letters,
                                        onClick = { onDigitPress(digit) },
                                        onLongClick = if (digit == "0") {
                                            { onDigitPress("+") }
                                        } else if (digit == "*") {
                                            onClearQuery
                                        } else null
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onOpenAiCommand)
                                .testTag("dialer_voice_command_pill"),
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, style.hairlineBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice command",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "VOICE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val target = dialerQuery.ifBlank {
                                    recentLogs.firstOrNull()?.phoneNumber.orEmpty()
                                }
                                onPlaceCall(target, predictiveMatches.firstOrNull())
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .size(60.dp)
                                .testTag("dialer_call_fab"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = stringResource(R.string.action_call),
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onToggleDialpad)
                                .testTag("dialer_collapse_pad_pill"),
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, style.hairlineBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dialpad,
                                    contentDescription = "Hide dialpad",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "HIDE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PredictiveContactRow(
    contact: ContactEntity,
    onCall: () -> Unit
) {
    NothingSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("predictive_match_${contact.id}"),
        highlighted = contact.isFavorite,
        onClick = onCall
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                ContactAvatarCircle(
                    name = contact.name,
                    photoUri = contact.photoUri,
                    size = 42.dp,
                    highlighted = contact.isFavorite
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${contact.phoneNumber}  •  T9:${contact.t9Key}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "Call ${contact.name}",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun RecentCallRow(
    log: CallLogEntity,
    photoUri: String?,
    nowMs: Long = remember { System.currentTimeMillis() },
    onCall: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val dirIcon = when (log.direction) {
        CallDirection.INCOMING -> Icons.AutoMirrored.Filled.CallReceived
        CallDirection.OUTGOING -> Icons.AutoMirrored.Filled.CallMade
        CallDirection.MISSED -> Icons.AutoMirrored.Filled.CallMissed
        CallDirection.BLOCKED_SPAM -> Icons.Default.Security
    }
    val dirTint = when (log.direction) {
        CallDirection.MISSED, CallDirection.BLOCKED_SPAM -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    NothingSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recent_call_card_${log.id}"),
        highlighted = log.isSpam,
        onClick = onCall
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                ContactAvatarCircle(
                    name = log.displayName,
                    photoUri = photoUri,
                    size = 40.dp,
                    highlighted = log.isSpam
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = dirIcon,
                            contentDescription = log.direction.name,
                            tint = dirTint,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = log.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (log.isSpam) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Spam risk",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    val relTime = remember(log.id, log.timestamp, nowMs) {
                        T9Utils.formatRelativeTimestamp(log.timestamp, nowMs)
                    }
                    Text(
                        text = "$relTime  •  ${T9Utils.formatDuration(log.durationSeconds)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Delete log entry",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onCall,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Redial ${log.displayName}",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
