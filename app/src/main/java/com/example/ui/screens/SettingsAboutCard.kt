package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.system.DeviceNameHelper
import com.example.ui.components.DotMatrixTagChip
import com.example.ui.components.NothingSurfaceCard
import com.example.ui.theme.LocalDotDialStyle

private data class AboutFeatureSpec(
    val title: String,
    val description: String
)

private data class AboutChangelogRelease(
    val version: String,
    val headline: String,
    val notes: List<String>
)

private enum class AboutModalSection(val label: String) {
    ALL("ALL"),
    FEATURES("FEATURES"),
    CHANGELOG("CHANGELOG")
}

private val BRIEF_CHANGELOG_ITEMS = listOf(
    "v2.2: Auto Day/Night theme following system mode",
    "One-time Google Sign-In prompt on first app launch",
    "Exclusive 3-mode Backdrops: Video, Photo, or Calling Card",
    "Full outgoing caller photo/name/number & active mic fix",
    "History expanded quick menu: Video Call, SMS & WhatsApp"
)

private val BRIEF_FEATURES_SUMMARY = listOf(
    "T9 Predictive Dialpad + Custom Vibration Glyphs",
    "Gemini AI Call Assistant (EN / HI / BN / Auto)",
    "Live In-Call Voice Changer + PIN Call Recording Vault",
    "Multi-Account Google Sign-In, Contacts & Drive Backup"
)

private val FULL_FEATURES_LIST = listOf(
    AboutFeatureSpec(
        title = "Nothing Dot-Matrix Aesthetic & Auto Day/Night",
        description = "Geometric dot-matrix surfaces, 7 curated themes, AMOLED black toggle, custom font/glow styles, and automatic system Day/Night visual switching."
    ),
    AboutFeatureSpec(
        title = "Predictive T9 Dialer & Custom Keypad Audio",
        description = "Instant T9 name and number matching with classic DTMF, Soft Pop, or Dot Chime feedback and configurable refresh rates."
    ),
    AboutFeatureSpec(
        title = "Gemini AI Call Assistant",
        description = "Intelligent call screening and hands-free answering with Busy Mode default trigger, custom owner greeting, EN/HI/BN/Auto voice selection, and live transcripts."
    ),
    AboutFeatureSpec(
        title = "Exclusive 3-Mode Caller Screen Backdrops",
        description = "Customize incoming and outgoing call screens per contact or globally with Video Backdrop, Gallery Photo, or Calling Card (only one mode active at a time)."
    ),
    AboutFeatureSpec(
        title = "Full Caller Identity & Live Voice Changer",
        description = "Displays contact photo, name, and number across dialing, active, and incoming screens, paired with 6 live voice presets, mute, speaker, and Bluetooth routing."
    ),
    AboutFeatureSpec(
        title = "Person-Wise History & Quick Actions",
        description = "Collapses repeated calls per person with Incoming/Outgoing/Missed/Spam filters plus direct Carrier Video Call, SMS, and WhatsApp buttons inside expanded cards."
    ),
    AboutFeatureSpec(
        title = "PIN-Locked Recording Vault & AI Summaries",
        description = "Save silent or manual call recordings inside an encrypted-feel 4-digit PIN vault with on-demand Gemini call summaries."
    ),
    AboutFeatureSpec(
        title = "Multi-Account Google Sign-In & Drive Backup",
        description = "Switch Google accounts cleanly, choose Google Account vs Device storage for new contacts, and back up/restore contacts, call logs, and full settings to Google Drive."
    ),
    AboutFeatureSpec(
        title = "Vibration Glyphs, Spam Shield & Home Widget",
        description = "Per-contact rhythmic vibration glyphs, real-time spam blocking rules, and a quick-dial favorite contacts home screen widget."
    )
)

private val FULL_CHANGELOG_RELEASES = listOf(
    AboutChangelogRelease(
        version = "v2.2",
        headline = "Day/Night Polish, Exclusive Backdrops & History Actions",
        notes = listOf(
            "Added Auto System Day/Night visual style toggle in Settings without overriding saved dark/AMOLED presets.",
            "Fixed Google Sign-In dialog so it prompts strictly once on first launch instead of every app open.",
            "Hid AI Call Assistant button on incoming popups when disabled and set 'When Busy Mode On' as the default trigger.",
            "Removed Popup Ring & Spam chips from the Dialer header for a cleaner keypad layout.",
            "Fixed hands-free SpeechRecognizer mic cycling by restricting listening to ringing state only.",
            "Enriched dialing and active calls with full contact photo, name, and number.",
            "Added mutually exclusive 3-mode backdrops (Video, Gallery Photo, Calling Card) per contact and in Settings.",
            "Added direct Carrier Video Call, SMS, and WhatsApp quick actions inside expanded History contact cards.",
            "Added 'My Thinking On Nothing Inspired Dialer' features/changelog sheet and Telegram channel link."
        )
    ),
    AboutChangelogRelease(
        version = "v2.1",
        headline = "Multi-Account Google Sign-In, Grouped History & Voice Changer",
        notes = listOf(
            "Added Credential Manager Google Sign-In & account switching for personal Drive backups and AI greeting identity.",
            "Added Google Account vs Device storage selection when saving new contacts.",
            "Grouped Call History person-wise with collapsible multi-call threads and Incoming/Outgoing filters.",
            "Added In-Call Voice Changer presets (Natural, Deep, Bright Female, Robot, Studio Warm, Radio Lo-Fi) and mute controls."
        )
    ),
    AboutChangelogRelease(
        version = "v2.0",
        headline = "Gemini AI Call Assistant & Google Drive Sync",
        notes = listOf(
            "Introduced Gemini AI Call Assistant with Auto-Chooses English, Hindi, and Bengali voice replies.",
            "Added Drive AppData backup and full Settings JSON export/restore."
        )
    ),
    AboutChangelogRelease(
        version = "v1.0",
        headline = "Initial Nothing OS Inspired Release",
        notes = listOf(
            "Launched Dot Dial with T9 keypad, dot-matrix theme engine, vibration glyphs, video backdrops, spam defense, analytics, and PIN recording vault."
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingsCard() {
    val context = LocalContext.current
    val style = LocalDotDialStyle.current
    val deviceName = remember(context) {
        DeviceNameHelper.resolveDeviceShortName(context)
    }
    var showPhilosophySheet by remember { mutableStateOf(false) }

    NothingSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_about_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(R.string.about_section_title),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.about_section_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                DotMatrixTagChip(
                    label = "v${BuildConfig.VERSION_NAME}",
                    active = true,
                    onClick = { showPhilosophySheet = true }
                )
            }

            // Interactive Header: "My Thinking On Nothing Inspired Dialer" + Brief Changelog & Features
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .minimumInteractiveComponentSize()
                    .clickable { showPhilosophySheet = true }
                    .testTag("about_philosophy_header"),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            Icon(
                                imageVector = Icons.Default.NewReleases,
                                contentDescription = stringResource(R.string.about_dialer_philosophy_title),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.about_dialer_philosophy_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.about_dialer_philosophy_subtitle),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.about_dialer_philosophy_subtitle),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    HorizontalDivider(color = style.hairlineBorder)

                    Text(
                        text = "BRIEF CHANGELOG (v${BuildConfig.VERSION_NAME})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        BRIEF_CHANGELOG_ITEMS.forEach { item ->
                            Text(
                                text = "• $item",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "KEY FEATURES",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        BRIEF_FEATURES_SUMMARY.forEach { feat ->
                            Text(
                                text = "• $feat",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            AboutCreditRow(
                icon = Icons.Default.Person,
                label = stringResource(R.string.about_maintained_by_label),
                value = stringResource(R.string.about_maintainer_name)
            )

            HorizontalDivider(color = style.hairlineBorder)

            AboutCreditRow(
                icon = Icons.Default.Smartphone,
                label = stringResource(R.string.about_platform_label),
                value = "${stringResource(R.string.about_for_nothing_os)}  •  $deviceName"
            )

            HorizontalDivider(color = style.hairlineBorder)

            AboutLinkRow(
                icon = Icons.Default.Code,
                label = stringResource(R.string.about_github_label),
                handle = stringResource(R.string.about_github_handle),
                openDescription = stringResource(R.string.about_open_github),
                url = "https://github.com/Ajking420",
                testTag = "about_github_row"
            )

            HorizontalDivider(color = style.hairlineBorder)

            AboutLinkRow(
                icon = Icons.AutoMirrored.Filled.Send,
                label = stringResource(R.string.about_telegram_label),
                handle = stringResource(R.string.about_telegram_handle),
                openDescription = stringResource(R.string.about_open_telegram),
                url = "https://t.me/aj_apurba_personal_builds",
                testTag = "about_telegram_row"
            )

            HorizontalDivider(color = style.hairlineBorder)

            AboutCreditRow(
                icon = Icons.Default.AutoAwesome,
                label = stringResource(R.string.about_engine_label),
                value = stringResource(R.string.about_built_with_gemini)
            )
        }
    }

    if (showPhilosophySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var selectedSection by remember { mutableStateOf(AboutModalSection.ALL) }

        ModalBottomSheet(
            onDismissRequest = { showPhilosophySheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 8.dp)
                    .testTag("about_philosophy_sheet"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.about_dialer_philosophy_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "DOT DIAL v${BuildConfig.VERSION_NAME}  •  AJ APURBA",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showPhilosophySheet = false },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("close_about_philosophy_sheet")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, style.hairlineBorder)
                ) {
                    Text(
                        text = "Built with a focus on tactile dot-matrix clarity and zero-clutter ergonomics—combining Nothing OS inspired aesthetics with practical Gemini AI call screening, multi-account Google Drive continuity, and per-contact visual personalization.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AboutModalSection.entries.forEach { section ->
                        DotMatrixTagChip(
                            label = section.label,
                            active = selectedSection == section,
                            onClick = { selectedSection = section },
                            modifier = Modifier.weight(1f),
                            testTag = "about_sheet_tab_${section.name.lowercase()}"
                        )
                    }
                }

                if (selectedSection == AboutModalSection.ALL || selectedSection == AboutModalSection.FEATURES) {
                    Text(
                        text = "ALL APP FEATURES (${FULL_FEATURES_LIST.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FULL_FEATURES_LIST.forEach { feature ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, style.hairlineBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = feature.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = feature.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (selectedSection == AboutModalSection.ALL || selectedSection == AboutModalSection.CHANGELOG) {
                    Text(
                        text = "FULL CHANGELOG",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FULL_CHANGELOG_RELEASES.forEach { release ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (release.version == "v${BuildConfig.VERSION_NAME}") {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                } else {
                                    style.hairlineBorder
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = release.headline,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    DotMatrixTagChip(
                                        label = release.version,
                                        active = release.version == "v${BuildConfig.VERSION_NAME}",
                                        onClick = null
                                    )
                                }
                                release.notes.forEach { note ->
                                    Text(
                                        text = "• $note",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun AboutLinkRow(
    icon: ImageVector,
    label: String,
    handle: String,
    openDescription: String,
    url: String,
    testTag: String
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .minimumInteractiveComponentSize()
            .clickable {
                runCatching {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(url)
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            }
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = handle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = openDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun AboutCreditRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
