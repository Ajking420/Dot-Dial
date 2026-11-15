package com.example.data.system

import com.example.data.local.ContactDao
import com.example.data.local.SpamRuleDao

data class SpamVerdict(
    val isSpam: Boolean,
    val riskScore: Int,
    val reason: String
)

class SpamDetectionEngine(
    private val contactDao: ContactDao,
    private val spamRuleDao: SpamRuleDao
) {
    suspend fun evaluateNumber(phoneNumber: String, callerLabel: String = ""): SpamVerdict {
        val normalized = T9Utils.normalizePhone(phoneNumber)
        if (normalized.isEmpty()) {
            return SpamVerdict(isSpam = false, riskScore = 0, reason = "Valid internal line")
        }

        val knownContact = contactDao.findByNormalizedNumber(normalized)
        if (knownContact != null) {
            if (knownContact.isBlocked) {
                return SpamVerdict(
                    isSpam = true,
                    riskScore = 100,
                    reason = "Number is on your local blocklist"
                )
            }
            if (knownContact.spamScore < 40) {
                return SpamVerdict(
                    isSpam = false,
                    riskScore = knownContact.spamScore,
                    reason = "Saved contact (${knownContact.name})"
                )
            }
        }

        val rules = spamRuleDao.getEnabledRules()
        for (rule in rules) {
            val cleanPattern = T9Utils.normalizePhone(rule.pattern)
            if (cleanPattern.isNotEmpty() &&
                (normalized.startsWith(cleanPattern) || normalized.contains(cleanPattern))
            ) {
                return SpamVerdict(
                    isSpam = rule.riskLevel >= 70,
                    riskScore = rule.riskLevel,
                    reason = rule.label
                )
            }
        }

        // Heuristic checks for spoofed / repeated digits / suspicious short/long robocall signatures
        val digits = normalized.filter { it.isDigit() }
        val uniqueDigitCount = digits.toSet().size
        if (digits.length >= 7 && uniqueDigitCount <= 2) {
            return SpamVerdict(
                isSpam = true,
                riskScore = 88,
                reason = "Suspected spoofed repeating-digit pattern"
            )
        }
        if (callerLabel.contains("spam", ignoreCase = true) ||
            callerLabel.contains("telemarket", ignoreCase = true) ||
            callerLabel.contains("scam", ignoreCase = true)
        ) {
            return SpamVerdict(
                isSpam = true,
                riskScore = 92,
                reason = "Caller ID flagged as telemarketing / scam"
            )
        }

        return SpamVerdict(
            isSpam = false,
            riskScore = 12,
            reason = "No local spam rule triggered"
        )
    }
}
