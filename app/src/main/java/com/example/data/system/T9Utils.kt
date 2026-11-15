package com.example.data.system

import com.example.data.local.ContactEntity

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val shortDateFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
}

object T9Utils {
    private val charToDigitMap = mapOf(
        'a' to '2', 'b' to '2', 'c' to '2',
        'd' to '3', 'e' to '3', 'f' to '3',
        'g' to '4', 'h' to '4', 'i' to '4',
        'j' to '5', 'k' to '5', 'l' to '5',
        'm' to '6', 'n' to '6', 'o' to '6',
        'p' to '7', 'q' to '7', 'r' to '7', 's' to '7',
        't' to '8', 'u' to '8', 'v' to '8',
        'w' to '9', 'x' to '9', 'y' to '9', 'z' to '9'
    )

    fun toT9Key(name: String): String {
        val sb = StringBuilder(name.length)
        for (ch in name.lowercase()) {
            when {
                ch.isDigit() -> sb.append(ch)
                charToDigitMap.containsKey(ch) -> sb.append(charToDigitMap[ch])
            }
        }
        return sb.toString()
    }

    fun normalizePhone(number: String): String {
        val trimmed = number.trim()
        val hasPlus = trimmed.startsWith("+")
        val digitsOnly = trimmed.filter { it.isDigit() }
        return if (hasPlus && digitsOnly.isNotEmpty()) "+$digitsOnly" else digitsOnly
    }

    fun matchesQuery(contact: ContactEntity, query: String): Boolean {
        if (query.isBlank()) return true
        val cleaned = query.trim()
        val digitsQuery = cleaned.filter { it.isDigit() }
        if (contact.name.contains(cleaned, ignoreCase = true) ||
            contact.email.contains(cleaned, ignoreCase = true) ||
            contact.phoneNumber.contains(cleaned, ignoreCase = true) ||
            contact.additionalNumbersJson.contains(cleaned, ignoreCase = true)
        ) {
            return true
        }
        if (digitsQuery.isNotEmpty()) {
            if (contact.t9Key.contains(digitsQuery)) return true
            if (contact.allNormalizedNumbers().any { it.contains(digitsQuery) }) return true
        }
        return false
    }

    fun buildNumberLookup(contacts: List<ContactEntity>): Map<String, ContactEntity> {
        if (contacts.isEmpty()) return emptyMap()
        val map = HashMap<String, ContactEntity>(contacts.size * 2)
        contacts.forEach { contact ->
            contact.allNormalizedNumbers().forEach { norm ->
                if (!map.containsKey(norm)) {
                    map[norm] = contact
                }
            }
        }
        return map
    }

    fun formatDuration(seconds: Int): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            "%02d:%02d:%02d".format(hrs, mins, secs)
        } else {
            "%02d:%02d".format(mins, secs)
        }
    }

    fun formatRelativeTimestamp(timestampMs: Long, nowMs: Long): String {
        val diffMs = (nowMs - timestampMs).coerceAtLeast(0L)
        val mins = diffMs / 60_000L
        return when {
            mins < 1L -> "Just now"
            mins < 60L -> "${mins}m ago"
            mins < 1440L -> "${mins / 60L}h ago"
            mins < 2880L -> "Yesterday"
            else -> shortDateFormatter.get()?.format(Date(timestampMs)).orEmpty()
        }
    }
}
