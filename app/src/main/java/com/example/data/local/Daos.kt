package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY isFavorite DESC, name ASC")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts ORDER BY name ASC")
    suspend fun getAllOnce(): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE normalizedNumber = :normalized LIMIT 1")
    suspend fun findByNormalizedNumber(normalized: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(contact: ContactEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactEntity>)

    @Update
    suspend fun update(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM contacts WHERE accountName = :accountName")
    suspend fun deleteByAccountName(accountName: String)

    @Query("UPDATE contacts SET isBlocked = :blocked WHERE normalizedNumber = :normalized")
    suspend fun setBlockedByNumber(normalized: String, blocked: Boolean)

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun count(): Int
}

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<CallLogEntity>

    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): CallLogEntity?

    @Query("SELECT * FROM call_logs WHERE normalizedNumber = :normalized ORDER BY timestamp DESC")
    fun observeForNumber(normalized: String): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: CallLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<CallLogEntity>)

    @Query("UPDATE call_logs SET recordingId = :recordingId WHERE id = :logId")
    suspend fun attachRecording(logId: Long, recordingId: Long)

    @Query("DELETE FROM call_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM call_logs")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM call_logs")
    suspend fun count(): Int
}

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CallRecordingEntity>>

    @Query("SELECT * FROM recordings ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatest(): CallRecordingEntity?

    @Query("SELECT * FROM recordings WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CallRecordingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recording: CallRecordingEntity): Long

    @Update
    suspend fun update(recording: CallRecordingEntity)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface VibrationPatternDao {
    @Query("SELECT * FROM vibration_patterns ORDER BY isBuiltIn DESC, id ASC")
    fun observeAll(): Flow<List<VibrationPatternEntity>>

    @Query("SELECT * FROM vibration_patterns WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): VibrationPatternEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pattern: VibrationPatternEntity): Long

    @Query("DELETE FROM vibration_patterns WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteCustom(id: Long)

    @Query("SELECT COUNT(*) FROM vibration_patterns")
    suspend fun count(): Int
}

@Dao
interface SpamRuleDao {
    @Query("SELECT * FROM spam_rules ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SpamRuleEntity>>

    @Query("SELECT * FROM spam_rules WHERE isEnabled = 1")
    suspend fun getEnabledRules(): List<SpamRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: SpamRuleEntity): Long

    @Update
    suspend fun update(rule: SpamRuleEntity)

    @Query("DELETE FROM spam_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM spam_rules")
    suspend fun count(): Int
}
