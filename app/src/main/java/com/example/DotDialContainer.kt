package com.example

import android.content.Context
import com.example.auth.GoogleAccountSignInManager
import com.example.data.ai.GeminiAiRepository
import com.example.data.local.DotDialDatabase
import com.example.data.preferences.DialerPreferences
import com.example.data.system.AudioRouteManager
import com.example.data.system.BackupExportManager
import com.example.data.system.DeviceContactsSync
import com.example.data.system.GoogleDriveBackupManager
import com.example.data.system.PrivacyAudioRecorder
import com.example.data.system.RingtonePlayer
import com.example.data.system.SpamDetectionEngine
import com.example.data.system.UiSoundEffects
import com.example.data.system.VibrationGlyphEngine
import com.example.voice.AiCallAssistantEngine
import com.example.voice.InCallVoiceChangerEngine
import com.example.voice.VoiceCommandController

class DotDialContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: DotDialDatabase by lazy { DotDialDatabase.getInstance(appContext) }
    val preferences: DialerPreferences by lazy { DialerPreferences(appContext) }
    val vibrationEngine: VibrationGlyphEngine by lazy { VibrationGlyphEngine(appContext) }
    val audioRecorder: PrivacyAudioRecorder by lazy { PrivacyAudioRecorder(appContext) }
    val geminiAi: GeminiAiRepository by lazy { GeminiAiRepository() }
    val voiceController: VoiceCommandController by lazy { VoiceCommandController(appContext) }
    val aiAssistantEngine: AiCallAssistantEngine by lazy { AiCallAssistantEngine(appContext) }
    val voiceChangerEngine: InCallVoiceChangerEngine by lazy { InCallVoiceChangerEngine(appContext) }
    val uiSounds: UiSoundEffects by lazy { UiSoundEffects(appContext) }
    val audioRouteManager: AudioRouteManager by lazy { AudioRouteManager(appContext) }
    val ringtonePlayer: RingtonePlayer by lazy { RingtonePlayer(appContext) }
    val driveBackupManager: GoogleDriveBackupManager by lazy { GoogleDriveBackupManager(appContext) }
    val signInManager: GoogleAccountSignInManager by lazy { GoogleAccountSignInManager(appContext) }

    val contactsSync: DeviceContactsSync by lazy {
        DeviceContactsSync(
            context = appContext,
            contactDao = database.contactDao(),
            callLogDao = database.callLogDao()
        )
    }

    val backupManager: BackupExportManager by lazy {
        BackupExportManager(
            context = appContext,
            contactDao = database.contactDao(),
            callLogDao = database.callLogDao(),
            preferences = preferences
        )
    }

    val spamEngine: SpamDetectionEngine by lazy {
        SpamDetectionEngine(
            contactDao = database.contactDao(),
            spamRuleDao = database.spamRuleDao()
        )
    }
}
