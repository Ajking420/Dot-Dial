package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.system.ActiveAudioRoute
import com.example.data.system.T9Utils
import com.example.telecom.CallLifecycleState
import com.example.telecom.CallSessionState
import com.example.ui.components.AudioGlyphVisualizer
import com.example.ui.components.CallerVideoBackdrop
import com.example.ui.components.ContactAvatarCircle
import com.example.ui.components.DotKeypadButton
import com.example.ui.components.DotMatrixTagChip
import com.example.ui.components.GlyphCallVisualizer
import com.example.ui.components.PulsingDotIndicator
import com.example.ui.theme.MatrixEmerald
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingRed
import com.example.voice.VoiceUiState

private val InCallDialpadRows = listOf(
    listOf("1" to "∞", "2" to "ABC", "3" to "DEF"),
    listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
    listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
    listOf("*" to "", "0" to "+", "#" to "")
)

@Composable
fun ActiveCallOverlay(
    session: CallSessionState,
    voiceState: VoiceUiState,
    redTintStrength: Float,
    voiceControlEnabled: Boolean,
    silentRecording: Boolean,
    onAnswerCall: () -> Unit,
    onEndCall: () -> Unit,
    onCollapseToStatusBar: () -> Unit,
    onToggleMute: () -> Unit,
    onCycleAudioRoute: () -> Unit,
    onToggleRecord: () -> Unit,
    onSimulateVoicePhrase: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onSendDtmfDigit: (String) -> Unit,
    onClearDtmfDigits: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMiniNotepad by remember { mutableStateOf(false) }
    var showInCallDialpad by remember { mutableStateOf(false) }

    val routeIcon = when (session.activeAudioRoute) {
        ActiveAudioRoute.BLUETOOTH -> Icons.Default.BluetoothAudio
        ActiveAudioRoute.EAR_RECEIVER -> Icons.Default.PhoneInTalk
        ActiveAudioRoute.SPEAKER -> Icons.Default.VolumeUp
    }
    val routeLabel = when (session.activeAudioRoute) {
        ActiveAudioRoute.BLUETOOTH -> "BLUETOOTH"
        ActiveAudioRoute.EAR_RECEIVER -> "EARPIECE"
        ActiveAudioRoute.SPEAKER -> "SPEAKER"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CallerVideoBackdrop(
            videoUriString = session.videoBackdropUri,
            callingCardUriString = session.callingCardUri,
            redTintStrength = redTintStrength,
            isRingingOrActive = session.lifecycle == CallLifecycleState.INCOMING_RINGING ||
                session.lifecycle == CallLifecycleState.ACTIVE
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Status Header + Minimize to Status Bar + Spam Alert
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (session.lifecycle == CallLifecycleState.ACTIVE) {
                        IconButton(
                            onClick = onCollapseToStatusBar,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("collapse_call_to_status_bar")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Minimize call to status bar",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PulsingDotIndicator(
                                color = if (session.isRecording && !silentRecording) NothingRed else MatrixEmerald,
                                active = true
                            )
                            Text(
                                text = when (session.lifecycle) {
                                    CallLifecycleState.INCOMING_RINGING -> "INCOMING GLYPH CALL"
                                    CallLifecycleState.DIALING -> "DIALING..."
                                    CallLifecycleState.ACTIVE ->
                                        if (session.isRecording && !silentRecording) {
                                            "REC • ${T9Utils.formatDuration(session.elapsedSeconds)}"
                                        } else if (session.isRecording) {
                                            "SILENT REC • ${T9Utils.formatDuration(session.elapsedSeconds)}"
                                        } else {
                                            "CONNECTED • ${T9Utils.formatDuration(session.elapsedSeconds)}"
                                        }
                                    CallLifecycleState.IDLE -> ""
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (session.lifecycle == CallLifecycleState.ACTIVE) {
                        IconButton(
                            onClick = { showMiniNotepad = !showMiniNotepad },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("toggle_mini_notepad_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "Toggle mini notepad",
                                tint = if (showMiniNotepad) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }

                AnimatedVisibility(
                    visible = session.isSpamWarning,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = NothingRed.copy(alpha = 0.90f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("incoming_spam_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Spam warning",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Column {
                                Text(
                                    text = "${stringResource(R.string.spam_warning_title)} (${session.spamScore}%)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = session.spamReason,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f)
                                )
                            }
                        }
                    }
                }
            }

            // Center Caller Glyph + Small Rounded Contact Photo + Theme-Matched Name + Mini Notepad
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    GlyphCallVisualizer(
                        isRingingOrActive = session.lifecycle == CallLifecycleState.INCOMING_RINGING ||
                            session.lifecycle == CallLifecycleState.ACTIVE,
                        isRecording = session.isRecording,
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                    ContactAvatarCircle(
                        name = session.displayName.ifBlank { session.phoneNumber },
                        photoUri = session.contactPhotoUri,
                        size = 68.dp,
                        highlighted = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = session.displayName.ifBlank { session.phoneNumber },
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 32.sp, lineHeight = 38.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${session.phoneNumber} • $routeLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Hands-Free Always-On Voice Control Panel (auto-hides when In-Call Dialpad is open)
                AnimatedVisibility(
                    visible = !showInCallDialpad && voiceControlEnabled &&
                        (session.lifecycle == CallLifecycleState.INCOMING_RINGING ||
                            session.lifecycle == CallLifecycleState.ACTIVE),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
                        border = BorderStroke(
                            1.dp,
                            if (voiceState.isListening) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (session.lifecycle == CallLifecycleState.INCOMING_RINGING) {
                                        stringResource(R.string.voice_hint_incoming)
                                    } else {
                                        stringResource(R.string.voice_hint_active)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                PulsingDotIndicator(
                                    color = if (voiceState.isListening) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    size = 9.dp
                                )
                            }

                            AudioGlyphVisualizer(
                                isActive = voiceState.isListening || session.isRecording,
                                barColor = MaterialTheme.colorScheme.primary
                            )

                            if (voiceState.partialTranscript.isNotBlank()) {
                                Text(
                                    text = "\"${voiceState.partialTranscript}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (session.lifecycle == CallLifecycleState.INCOMING_RINGING) {
                                    DotMatrixTagChip(
                                        label = "\"Answer\"",
                                        active = false,
                                        onClick = { onSimulateVoicePhrase("Answer") },
                                        testTag = "voice_chip_receive"
                                    )
                                    DotMatrixTagChip(
                                        label = "\"Mute\"",
                                        active = false,
                                        onClick = { onSimulateVoicePhrase("Mute") },
                                        testTag = "voice_chip_mute"
                                    )
                                }
                                DotMatrixTagChip(
                                    label = "\"End call\"",
                                    active = true,
                                    onClick = { onSimulateVoicePhrase("End call") },
                                    testTag = "voice_chip_end"
                                )
                            }
                        }
                    }
                }

                // Expandable In-Call DTMF Dialpad
                AnimatedVisibility(
                    visible = showInCallDialpad && (session.lifecycle == CallLifecycleState.ACTIVE ||
                        session.lifecycle == CallLifecycleState.DIALING),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("in_call_dialpad_panel")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (session.dtmfDigits.isBlank()) "IN-CALL KEYPAD"
                                    else session.dtmfDigits,
                                    style = if (session.dtmfDigits.isBlank()) {
                                        MaterialTheme.typography.labelMedium
                                    } else {
                                        MaterialTheme.typography.titleLarge.copy(letterSpacing = 2.sp)
                                    },
                                    color = if (session.dtmfDigits.isBlank()) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    maxLines = 1,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 6.dp)
                                        .testTag("in_call_dtmf_display")
                                )
                                if (session.dtmfDigits.isNotEmpty()) {
                                    IconButton(
                                        onClick = onClearDtmfDigits,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("in_call_dtmf_clear_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear entered digits",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            InCallDialpadRows.forEach { row ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    row.forEach { (digit, letters) ->
                                        DotKeypadButton(
                                            digit = digit,
                                            letters = letters,
                                            size = 54.dp,
                                            onClick = { onSendDtmfDigit(digit) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Expandable Mini Notepad inside In-Call UI
                AnimatedVisibility(
                    visible = showMiniNotepad && !showInCallDialpad &&
                        session.lifecycle == CallLifecycleState.ACTIVE,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "IN-CALL MINI NOTEPAD",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = session.miniNotepadText,
                                onValueChange = onUpdateNotes,
                                placeholder = {
                                    Text(
                                        "Jot quick call notes, names, or tasks...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("active_call_notes_input")
                            )
                        }
                    }
                }
            }

            // Bottom Action Controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (session.lifecycle == CallLifecycleState.ACTIVE ||
                    session.lifecycle == CallLifecycleState.DIALING
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CallControlRoundButton(
                            icon = if (session.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            label = if (session.isMuted) "UNMUTE" else "MUTE",
                            active = session.isMuted,
                            onClick = onToggleMute,
                            testTag = "call_mute_button"
                        )
                        CallControlRoundButton(
                            icon = Icons.Default.Dialpad,
                            label = "KEYPAD",
                            active = showInCallDialpad,
                            onClick = {
                                val next = !showInCallDialpad
                                showInCallDialpad = next
                                if (next) showMiniNotepad = false
                            },
                            testTag = "call_keypad_button"
                        )
                        CallControlRoundButton(
                            icon = Icons.Default.FiberManualRecord,
                            label = if (session.isRecording) "REC ON" else "RECORD",
                            active = session.isRecording,
                            activeColor = NothingRed,
                            onClick = onToggleRecord,
                            testTag = "call_record_button"
                        )
                        CallControlRoundButton(
                            icon = Icons.Default.EditNote,
                            label = "NOTES",
                            active = showMiniNotepad,
                            onClick = {
                                val next = !showMiniNotepad
                                showMiniNotepad = next
                                if (next) showInCallDialpad = false
                            },
                            testTag = "call_notepad_button"
                        )
                        CallControlRoundButton(
                            icon = routeIcon,
                            label = routeLabel,
                            active = session.activeAudioRoute != ActiveAudioRoute.EAR_RECEIVER,
                            onClick = onCycleAudioRoute,
                            testTag = "call_audio_route_button"
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (session.lifecycle == CallLifecycleState.INCOMING_RINGING) {
                        Button(
                            onClick = onAnswerCall,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MatrixEmerald,
                                contentColor = NothingBlack
                            ),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .size(72.dp)
                                .testTag("incoming_answer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = stringResource(R.string.action_answer),
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Button(
                        onClick = onEndCall,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NothingRed,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("call_end_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = stringResource(R.string.action_decline),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CallControlRoundButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(50.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .testTag(testTag),
            shape = CircleShape,
            color = if (active) activeColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (active) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
