package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DotDialConverters {
    @TypeConverter
    fun fromDirection(value: CallDirection): String = value.name

    @TypeConverter
    fun toDirection(value: String): CallDirection =
        runCatching { CallDirection.valueOf(value) }.getOrDefault(CallDirection.OUTGOING)
}

@Database(
    entities = [
        ContactEntity::class,
        CallLogEntity::class,
        CallRecordingEntity::class,
        VibrationPatternEntity::class,
        SpamRuleEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(DotDialConverters::class)
abstract class DotDialDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun callLogDao(): CallLogDao
    abstract fun recordingDao(): RecordingDao
    abstract fun vibrationPatternDao(): VibrationPatternDao
    abstract fun spamRuleDao(): SpamRuleDao

    companion object {
        @Volatile
        private var INSTANCE: DotDialDatabase? = null

        fun getInstance(context: Context): DotDialDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DotDialDatabase::class.java,
                    "dot_dial_vault.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { db ->
                        INSTANCE = db
                        CoroutineScope(Dispatchers.IO).launch {
                            seedDefaultsIfNeeded(db)
                        }
                    }
            }
        }

        private suspend fun seedDefaultsIfNeeded(db: DotDialDatabase) {
            val vibDao = db.vibrationPatternDao()
            if (vibDao.count() == 0) {
                listOf(
                    VibrationPatternEntity(
                        id = 1L,
                        name = "GLYPH PULSE",
                        description = "Signature Phone (3a) Lite crisp syncopated glyph pulse",
                        timingsCsv = "0,70,90,70,90,180,220",
                        amplitudesCsv = "0,220,0,220,0,255,0",
                        isBuiltIn = true
                    ),
                    VibrationPatternEntity(
                        id = 2L,
                        name = "DOUBLE DOT",
                        description = "Minimal twin tap for silent pocket alerts",
                        timingsCsv = "0,60,110,120,400",
                        amplitudesCsv = "0,200,0,255,0",
                        isBuiltIn = true
                    ),
                    VibrationPatternEntity(
                        id = 3L,
                        name = "MORSE MATRIX",
                        description = "Rapid dot-dash telegraph rhythm",
                        timingsCsv = "0,55,70,55,70,180,90,55,250",
                        amplitudesCsv = "0,180,0,180,0,255,0,200,0",
                        isBuiltIn = true
                    ),
                    VibrationPatternEntity(
                        id = 4L,
                        name = "HEARTBEAT RED",
                        description = "Low-frequency dual thump with soft tail",
                        timingsCsv = "0,95,80,145,520",
                        amplitudesCsv = "0,210,0,255,0",
                        isBuiltIn = true
                    )
                ).forEach { vibDao.insert(it) }
            }

            val spamDao = db.spamRuleDao()
            if (spamDao.count() == 0) {
                listOf(
                    SpamRuleEntity(
                        pattern = "+1800",
                        label = "Toll-Free Telemarketer Prefix",
                        riskLevel = 72
                    ),
                    SpamRuleEntity(
                        pattern = "+1888",
                        label = "Automated Robocall Range",
                        riskLevel = 78
                    ),
                    SpamRuleEntity(
                        pattern = "140",
                        label = "Registered Commercial Telemarketing",
                        riskLevel = 88
                    )
                ).forEach { spamDao.insert(it) }
            }
        }
    }
}
