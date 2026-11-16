package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.preferences.DialerSettings
import com.example.data.preferences.NewContactStorageMode
import com.example.data.preferences.VoiceChangerPreset
import com.example.data.preferences.VoiceChangerSettingsMode
import com.example.ui.components.ContactAvatarCircle
import com.example.ui.components.DotMatrixTagChip
import com.example.ui.components.NothingSurfaceCard
import com.example.ui.theme.LocalDotDialStyle
import com.example.viewmodel.GoogleAuthUiState

@Composable
fun FirstLaunchGoogleSignInDialog(
    authState: GoogleAuthUiState,
    onSignInClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!authState.isSigningIn) onSkipClick()
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(26.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Google account sign-in",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "SIGN IN WITH GOOGLE",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Sign in so your personal Google account powers your own Google Drive backups, cloud contact saves, and Gemini AI Call Assistant identity.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, LocalDotDialStyle.current.hairlineBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Private Google Drive call logs & settings backup",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Gemini AI greets callers with your Google name",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (!authState.errorMessage.isNullOrBlank()) {
                    Text(
                        text = authState.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSignInClick,
                enabled = !authState.isSigningIn,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("welcome_google_signin_button")
            ) {
                if (authState.isSigningIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SIGNING IN...", style = MaterialTheme.typography.labelMedium)
                } else {
                    Text("CONTINUE WITH GOOGLE", style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onSkipClick,
                enabled = !authState.isSigningIn,
                modifier = Modifier.testTag("welcome_skip_signin_button")
            ) {
                Text(
                    text = "CONTINUE OFFLINE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
fun GoogleAccountIdentityCard(
    settings: DialerSettings,
    authState: GoogleAuthUiState,
    onSignInOrSwitch: () -> Unit,
    onSignOut: () -> Unit
) {
    val isSignedIn = settings.signedInGoogleEmail.isNotBlank()
    val displayTitle = settings.signedInGoogleName.ifBlank {
        settings.signedInGoogleEmail.substringBefore("@").ifBlank { "No Google Account" }
    }

    NothingSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("google_account_identity_card"),
        highlighted = isSignedIn
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Google account profile",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GOOGLE ACCOUNT & MULTI-USER PROFILE",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                DotMatrixTagChip(
                    label = if (isSignedIn) "ACTIVE" else "GUEST",
                    active = isSignedIn,
                    onClick = onSignInOrSwitch
                )
            }

            if (isSignedIn) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ContactAvatarCircle(
                        name = displayTitle,
                        photoUri = settings.signedInGooglePhotoUri.ifBlank { null },
                        size = 48.dp,
                        highlighted = true
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = settings.signedInGoogleEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Linked to Gemini AI greeting & Drive backups",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Text(
                    text = "Sign in with Google to use your own Drive backups, Google contact sync, and personalized Gemini AI Call Assistant identity on shared devices.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!authState.errorMessage.isNullOrBlank()) {
                Text(
                    text = authState.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSignInOrSwitch,
                    enabled = !authState.isSigningIn,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_google_signin_switch_button")
                ) {
                    if (authState.isSigningIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CONNECTING...", style = MaterialTheme.typography.labelMedium)
                    } else {
                        Icon(
                            imageVector = if (isSignedIn) Icons.Default.SwapHoriz else Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSignedIn) "SWITCH ACCOUNT" else "SIGN IN WITH GOOGLE",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                if (isSignedIn) {
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("settings_google_signout_button")
                    ) {
                        Text("SIGN OUT", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun NewContactStorageSettingsCard(
    settings: DialerSettings,
    availableGoogleAccounts: List<String>,
    resolvedTargetAccount: String,
    onSetStorageMode: (NewContactStorageMode) -> Unit,
    onSelectPreferredGoogleAccount: (String) -> Unit
) {
    val style = LocalDotDialStyle.current
    val mergedAccounts = (listOf(settings.signedInGoogleEmail) + availableGoogleAccounts)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    NothingSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("new_contact_storage_settings_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Contacts,
                    contentDescription = "New contact save location",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "NEW CONTACT SAVE DESTINATION",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Choose where newly created contacts are saved by default.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NewContactStorageMode.entries.forEach { mode ->
                    DotMatrixTagChip(
                        label = if (mode == NewContactStorageMode.GOOGLE_ACCOUNT) {
                            "GOOGLE ACCOUNT (DEFAULT)"
                        } else {
                            "DEVICE STORAGE"
                        },
                        active = settings.newContactStorageMode == mode,
                        onClick = { onSetStorageMode(mode) },
                        testTag = "contact_storage_mode_${mode.id}"
                    )
                }
            }

            if (settings.newContactStorageMode == NewContactStorageMode.GOOGLE_ACCOUNT) {
                HorizontalDivider(color = style.hairlineBorder)
                if (mergedAccounts.isNotEmpty()) {
                    Text(
                        text = "TARGET GOOGLE ACCOUNT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        mergedAccounts.forEach { email ->
                            DotMatrixTagChip(
                                label = email,
                                active = resolvedTargetAccount.equals(email, ignoreCase = true),
                                onClick = { onSelectPreferredGoogleAccount(email) },
                                testTag = "contact_storage_account_$email"
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Sign in with Google above or enable device Google sync; otherwise new contacts fall back to Device Storage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "New contacts stay local on this phone's Device Storage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceChangerSettingsCard(
    settings: DialerSettings,
    onSetMode: (VoiceChangerSettingsMode) -> Unit,
    onSelectPreset: (VoiceChangerPreset) -> Unit,
    onPreviewPreset: (VoiceChangerPreset) -> Unit
) {
    val style = LocalDotDialStyle.current

    NothingSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("voice_changer_settings_card"),
        highlighted = settings.voiceChangerPreset != VoiceChangerPreset.NORMAL &&
            settings.voiceChangerMode != VoiceChangerSettingsMode.OFF
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "In-call voice changer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "IN-CALL VOICE CHANGER FX",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                DotMatrixTagChip(
                    label = settings.voiceChangerPreset.shortLabel,
                    active = settings.voiceChangerPreset != VoiceChangerPreset.NORMAL,
                    onClick = { onPreviewPreset(settings.voiceChangerPreset) }
                )
            }

            Text(
                text = "ACTIVE MODE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VoiceChangerSettingsMode.entries.forEach { mode ->
                    DotMatrixTagChip(
                        label = mode.label,
                        active = settings.voiceChangerMode == mode,
                        onClick = { onSetMode(mode) },
                        testTag = "voice_changer_mode_${mode.id}"
                    )
                }
            }

            HorizontalDivider(color = style.hairlineBorder)

            Text(
                text = "DEFAULT IN-CALL PRESET",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VoiceChangerPreset.entries.forEach { preset ->
                    DotMatrixTagChip(
                        label = preset.label,
                        active = settings.voiceChangerPreset == preset,
                        onClick = { onSelectPreset(preset) },
                        testTag = "voice_changer_preset_${preset.id}"
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onPreviewPreset(settings.voiceChangerPreset) },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("preview_voice_changer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PREVIEW EFFECT", style = MaterialTheme.typography.labelMedium)
                }
            }

            Text(
                text = "Note: Stock Android locks cellular modem uplink streams, so Live DSP monitors & injects via speaker/BT/AI persona acoustic path.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
