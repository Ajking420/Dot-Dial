package com.example.data.ai

import android.util.Log
import com.example.data.preferences.AiAssistantLanguageMode
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiAiRepository"

data class CallSummaryResult(
    val headline: String,
    val bulletPoints: List<String>,
    val actionItems: List<String>,
    val sentiment: String,
    val followUpSuggestion: String
)

enum class AiCommandIntent {
    ANSWER_CALL,
    END_CALL,
    PLACE_CALL,
    TOGGLE_SPEAKER,
    TOGGLE_MUTE,
    TOGGLE_RECORD,
    SUMMARIZE_LAST_CALL,
    BLOCK_NUMBER,
    SET_THEME,
    NAVIGATE_TAB,
    SIMULATE_INCOMING,
    ANALYTICS_INSIGHT,
    UNKNOWN
}

data class AiParsedCommand(
    val intent: AiCommandIntent,
    val targetQuery: String,
    val themeName: String,
    val tabName: String,
    val confirmationMessage: String
)

data class SpamRiskAssessment(
    val riskScore: Int,
    val classification: String,
    val reason: String,
    val recommendedAction: String
)

enum class AssistantReplyLanguage(
    val code: String,
    val displayLabel: String,
    val languageTag: String
) {
    EN("EN", "English", "en-IN"),
    HI("HI", "Hindi", "hi-IN"),
    BN("BN", "Bengali", "bn-IN");

    companion object {
        fun fromCode(code: String?): AssistantReplyLanguage =
            entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) } ?: EN
    }
}

data class AiAssistantTurnResult(
    val replyText: String,
    val language: AssistantReplyLanguage,
    val callerPurposeTag: String,
    val shouldEndCall: Boolean
)

class GeminiAiRepository {

    private val summarySchema = Schema.obj(
        properties = mapOf(
            "headline" to Schema.string(description = "One-sentence summary headline of the phone conversation"),
            "bulletPoints" to Schema.array(
                items = Schema.string(),
                description = "3 to 5 concise key points discussed on the call"
            ),
            "actionItems" to Schema.array(
                items = Schema.string(),
                description = "Concrete follow-up tasks or commitments mentioned"
            ),
            "sentiment" to Schema.enumeration(
                values = listOf("POSITIVE", "NEUTRAL", "URGENT", "CAUTIOUS"),
                description = "Overall emotional tone of the conversation"
            ),
            "followUpSuggestion" to Schema.string(description = "Short proactive recommendation for the user")
        )
    )

    private val commandSchema = Schema.obj(
        properties = mapOf(
            "intent" to Schema.enumeration(
                values = AiCommandIntent.entries.map { it.name },
                description = "Matched dialer control action"
            ),
            "targetQuery" to Schema.string(description = "Contact name or phone number if applicable, else empty"),
            "themeName" to Schema.string(description = "Theme preset id if SET_THEME: nothing_dark, pure_oled_red, chalk_matrix, or stealth_amber"),
            "tabName" to Schema.string(description = "Destination tab if NAVIGATE_TAB: DIALER, CONTACTS, VAULT, ANALYTICS, or STUDIO"),
            "confirmationMessage" to Schema.string(description = "Crisp Nothing OS style spoken confirmation message")
        )
    )

    private val spamSchema = Schema.obj(
        properties = mapOf(
            "riskScore" to Schema.integer(description = "Spam risk score from 0 to 100"),
            "classification" to Schema.enumeration(
                values = listOf("SAFE", "SUSPICIOUS", "HIGH_RISK_SPAM"),
                description = "Caller risk classification"
            ),
            "reason" to Schema.string(description = "Short explanation of why the caller was scored this way"),
            "recommendedAction" to Schema.string(description = "Action advice such as Answer, Screen, or Block")
        )
    )

    private val summaryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = summarySchema
            }
        )
    }

    private val commandModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = commandSchema
            }
        )
    }

    private val spamModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = spamSchema
            }
        )
    }

    private val assistantSchema = Schema.obj(
        properties = mapOf(
            "replyText" to Schema.string(
                description = "Natural 1 to 2 sentence spoken reply in the target language (Devanagari for HI, Bengali script for BN, English for EN). No markdown or emojis."
            ),
            "languageCode" to Schema.enumeration(
                values = listOf("EN", "HI", "BN"),
                description = "Language code of replyText: EN for English, HI for Hindi, BN for Bengali"
            ),
            "callerPurposeTag" to Schema.string(
                description = "2 to 5 word summary of why the caller is calling"
            ),
            "shouldEndCall" to Schema.boolean(
                description = "True only if the caller clearly finished explaining and said goodbye or confirmed they are done"
            )
        )
    )

    private val assistantModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = assistantSchema
            }
        )
    }

    suspend fun summarizeConversation(
        contactName: String,
        phoneNumber: String,
        durationSeconds: Int,
        transcriptOrNotes: String
    ): Result<CallSummaryResult> {
        return try {
            val prompt = buildString {
                appendLine("You are the Dot Dial conversation intelligence engine on a Nothing Phone.")
                appendLine("Summarize the following phone call accurately and concisely.")
                appendLine("Caller: $contactName ($phoneNumber)")
                appendLine("Duration: ${durationSeconds}s")
                appendLine("Conversation Transcript / Notes:")
                appendLine(transcriptOrNotes.ifBlank { "No spoken transcript captured; summarize call metadata briefly." })
            }

            val response = withTimeout(60_000L) {
                summaryModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("Gemini returned an empty response.")
            val json = JSONObject(rawJson)

            val bulletsArray = json.optJSONArray("bulletPoints")
            val bullets = buildList {
                if (bulletsArray != null) {
                    for (i in 0 until bulletsArray.length()) add(bulletsArray.optString(i))
                }
            }.filter { it.isNotBlank() }

            val actionsArray = json.optJSONArray("actionItems")
            val actions = buildList {
                if (actionsArray != null) {
                    for (i in 0 until actionsArray.length()) add(actionsArray.optString(i))
                }
            }.filter { it.isNotBlank() }

            Result.success(
                CallSummaryResult(
                    headline = json.optString("headline", "Call summary generated"),
                    bulletPoints = bullets,
                    actionItems = actions,
                    sentiment = json.optString("sentiment", "NEUTRAL"),
                    followUpSuggestion = json.optString("followUpSuggestion", "")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini summarization timed out", e)
            Result.failure(IllegalStateException("Gemini summary timed out after 60s. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini summarization failed", e)
            Result.failure(e)
        }
    }

    suspend fun parseFullControlCommand(
        commandText: String,
        hasIncomingCall: Boolean,
        hasActiveCall: Boolean,
        knownContacts: List<String>
    ): Result<AiParsedCommand> {
        return try {
            val prompt = buildString {
                appendLine("You are Dot Dial AI Full Control for Nothing Phone 3a Lite.")
                appendLine("Convert the user's voice or text command into an exact dialer action.")
                appendLine("Current call state: incomingRinging=$hasIncomingCall, activeCall=$hasActiveCall")
                if (knownContacts.isNotEmpty()) {
                    appendLine("Known contacts: ${knownContacts.take(25).joinToString(", ")}")
                }
                appendLine("User command: \"$commandText\"")
            }

            val response = withTimeout(45_000L) {
                commandModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("Gemini returned an empty command response.")
            val json = JSONObject(rawJson)

            val intentStr = json.optString("intent", AiCommandIntent.UNKNOWN.name)
            val intent = runCatching { AiCommandIntent.valueOf(intentStr) }
                .getOrDefault(AiCommandIntent.UNKNOWN)

            Result.success(
                AiParsedCommand(
                    intent = intent,
                    targetQuery = json.optString("targetQuery", ""),
                    themeName = json.optString("themeName", ""),
                    tabName = json.optString("tabName", ""),
                    confirmationMessage = json.optString("confirmationMessage", "Command processed.")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini command parsing timed out", e)
            Result.failure(IllegalStateException("AI command timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini command parsing failed", e)
            Result.failure(e)
        }
    }

    suspend fun assessCallerSpamRisk(
        phoneNumber: String,
        callerLabel: String,
        recentBurstCount: Int
    ): Result<SpamRiskAssessment> {
        return try {
            val prompt = buildString {
                appendLine("Analyze phone caller metadata for real-time spam/scam probability.")
                appendLine("Number: $phoneNumber")
                appendLine("Caller ID Label: ${callerLabel.ifBlank { "Unknown" }}")
                appendLine("Recent repeated calls from prefix: $recentBurstCount")
            }

            val response = withTimeout(30_000L) {
                spamModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("Empty spam check response.")
            val json = JSONObject(rawJson)

            Result.success(
                SpamRiskAssessment(
                    riskScore = json.optInt("riskScore", 25).coerceIn(0, 100),
                    classification = json.optString("classification", "SAFE"),
                    reason = json.optString("reason", "No suspicious signals detected."),
                    recommendedAction = json.optString("recommendedAction", "Answer normally")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Spam check timed out", e)
            Result.failure(IllegalStateException("Spam assessment timed out."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Spam check failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateCallAssistantReply(
        ownerName: String,
        callerName: String,
        phoneNumber: String,
        languageMode: AiAssistantLanguageMode,
        historyLines: List<String>,
        latestCallerSpeech: String
    ): Result<AiAssistantTurnResult> {
        return try {
            val langInstruction = when (languageMode) {
                AiAssistantLanguageMode.AUTO,
                AiAssistantLanguageMode.ENGLISH_GREET_AUTO ->
                    "Detect whether the caller spoke English, Hindi (including Hinglish), or Bengali (including Banglish), and reply naturally in that exact same language (use Devanagari script for Hindi, Bengali script for Bengali, English for English)."
                AiAssistantLanguageMode.ENGLISH ->
                    "Always reply in natural, polite English and set languageCode to EN."
                AiAssistantLanguageMode.HINDI ->
                    "Always reply in natural conversational Hindi using Devanagari script and set languageCode to HI."
                AiAssistantLanguageMode.BENGALI ->
                    "Always reply in natural conversational Bengali using Bengali script and set languageCode to BN."
            }

            val prompt = buildString {
                appendLine("You are the personal AI call assistant for $ownerName on their Android phone.")
                appendLine("$ownerName is currently busy and cannot take the call. You have already notified $ownerName that this caller rang and will call back as soon as possible.")
                appendLine("Caller info: $callerName ($phoneNumber)")
                appendLine("Language rule: $langInstruction")
                appendLine("Style rule: Keep replyText to 1 or 2 crisp, warm spoken sentences suitable for Text-To-Speech. Acknowledge what the caller said, ask a brief clarifying detail only if needed (name, urgency, or message), or reassure them $ownerName will call back shortly.")
                if (historyLines.isNotEmpty()) {
                    appendLine("Previous conversation turns:")
                    historyLines.takeLast(8).forEach { appendLine(it) }
                }
                appendLine("Caller just said: \"$latestCallerSpeech\"")
            }

            val response = withTimeout(30_000L) {
                assistantModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("AI assistant returned empty reply.")
            val json = JSONObject(rawJson)

            val lang = AssistantReplyLanguage.fromCode(json.optString("languageCode", "EN"))
            val forcedLang = when (languageMode) {
                AiAssistantLanguageMode.ENGLISH -> AssistantReplyLanguage.EN
                AiAssistantLanguageMode.HINDI -> AssistantReplyLanguage.HI
                AiAssistantLanguageMode.BENGALI -> AssistantReplyLanguage.BN
                else -> lang
            }

            Result.success(
                AiAssistantTurnResult(
                    replyText = json.optString("replyText").ifBlank {
                        "Thank you for sharing that. I have noted your message for $ownerName."
                    },
                    language = forcedLang,
                    callerPurposeTag = json.optString("callerPurposeTag", "Callback request"),
                    shouldEndCall = json.optBoolean("shouldEndCall", false)
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI assistant turn timed out", e)
            Result.failure(IllegalStateException("AI assistant timed out waiting for Gemini."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI assistant reply failed", e)
            Result.failure(e)
        }
    }
}
