package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.system.T9Utils
import com.example.telecom.CallLifecycleState
import com.example.telecom.CallSessionState
import com.example.ui.components.ContactAvatarCircle
import com.example.ui.components.PulsingDotIndicator
import com.example.ui.theme.MatrixEmerald
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingRed
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun IncomingFloatingTopBanner(
    session: CallSessionState,
    onAnswerToStatusBar: () -> Unit,
    onReject: () -> Unit,
    onSwipeUpMute: () -> Unit,
    onExpandFullScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    val dismissThresholdPx = with(LocalDensity.current) { 52.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .pointerInput(session.phoneNumber) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetY.value < -dismissThresholdPx) {
                                    offsetY.animateTo(-360f, tween(180))
                                    onSwipeUpMute()
                                    offsetY.snapTo(0f)
                                } else {
                                    offsetY.animateTo(0f, spring(dampingRatio = 0.72f, stiffness = 420f))
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { offsetY.animateTo(0f, spring()) }
                        },
                        onVerticalDrag = { _, dragAmount ->
                            scope.launch {
                                val next = (offsetY.value + dragAmount).coerceAtMost(18f)
                                offsetY.snapTo(next)
                            }
                        }
                    )
                }
                .clickable(onClick = onExpandFullScreen)
                .testTag("incoming_floating_popup"),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.97f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(
                1.2.dp,
                if (session.isSpamWarning) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            ),
            tonalElevation = 10.dp,
            shadowElevation = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ContactAvatarCircle(
                        name = session.displayName.ifBlank { session.phoneNumber },
                        photoUri = session.contactPhotoUri,
                        size = 44.dp,
                        highlighted = true
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PulsingDotIndicator(
                                color = if (session.isSpamWarning) NothingRed else MatrixEmerald,
                                size = 7.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = session.displayName.ifBlank { session.phoneNumber },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (session.isSpamWarning) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Spam risk",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = "${session.phoneNumber} • Swipe up to mute",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onReject,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NothingRed,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("floating_popup_reject")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Reject incoming call",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Button(
                        onClick = onAnswerToStatusBar,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MatrixEmerald,
                            contentColor = NothingBlack
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("floating_popup_accept")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Accept incoming call",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBarCallPill(
    session: CallSessionState,
    onExpandFullScreen: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMutedRing = session.lifecycle == CallLifecycleState.INCOMING_RINGING && session.isRingMuted
    val pulseTransition = rememberInfiniteTransition(label = "status_bar_ring_pulse")
    val ringAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(680, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "muted_ring_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onExpandFullScreen)
                .testTag(if (isMutedRing) "status_bar_muted_ring_pill" else "status_bar_active_call_pill"),
            shape = RoundedCornerShape(50),
            color = if (isMutedRing) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f)
            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.96f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isMutedRing) {
                    Icon(
                        imageVector = Icons.Default.PhoneInTalk,
                        contentDescription = "Muted ringing call",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(16.dp)
                            .alpha(ringAlpha)
                    )
                    Icon(
                        imageVector = Icons.Default.NotificationsOff,
                        contentDescription = "Ringtone silenced",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${session.displayName.ifBlank { session.phoneNumber }} • MUTED RING",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                } else {
                    PulsingDotIndicator(
                        color = if (session.isRecording) NothingRed else MatrixEmerald,
                        size = 8.dp
                    )
                    Text(
                        text = "${session.displayName.ifBlank { session.phoneNumber }} • ${T9Utils.formatDuration(session.elapsedSeconds)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    IconButton(
                        onClick = onToggleSpeaker,
                        modifier = Modifier
                            .size(32.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("status_pill_speaker")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Toggle speaker",
                            tint = if (session.isSpeakerOn) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onEndCall)
                            .testTag("status_pill_end"),
                        shape = CircleShape,
                        color = NothingRed
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "End call",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
