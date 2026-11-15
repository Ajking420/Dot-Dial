package com.example.data.system

import com.example.data.local.ContactEntity

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
        return contact.name.contains(cleaned, ignoreCase = true) ||
            contact.phoneNumber.contains(cleaned, ignoreCase = true) ||
            (digitsQuery.isNotEmpty() && (
                contact.t9Key.contains(digitsQuery) ||
                    contact.normalizedNumber.contains(digitsQuery)
                ))
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
}
