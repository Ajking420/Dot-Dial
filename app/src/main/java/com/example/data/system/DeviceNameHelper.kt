package com.example.data.system

import android.content.Context
import android.os.Build
import android.provider.Settings

object DeviceNameHelper {

    private val knownModelMap = mapOf(
        "A063" to "1",
        "A065" to "2",
        "A142" to "2A",
        "A142P" to "2A PLUS",
        "A059" to "3A",
        "A059P" to "3A PRO",
        "A015" to "CMF 1"
    )

    fun resolveDialerBrandHeader(context: Context): String {
        val deviceLabel = resolveDeviceShortName(context)
        return "DOT DIAL // $deviceLabel"
    }

    fun resolveDeviceShortName(context: Context): String {
        val globalName = runCatching {
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        }.getOrNull()?.trim().orEmpty()

        val model = Build.MODEL?.trim().orEmpty()
        val brand = Build.BRAND?.trim().orEmpty()
        val manufacturer = Build.MANUFACTURER?.trim().orEmpty()

        val upperModel = model.uppercase()
        val upperGlobal = globalName.uppercase()

        knownModelMap[upperGlobal]?.let { mapped ->
            if (globalName.equals(model, ignoreCase = true) || globalName.isBlank()) {
                return mapped
            }
        }

        val rawCandidate = when {
            globalName.isNotBlank() &&
                !globalName.equals("android", ignoreCase = true) &&
                !globalName.equals("localhost", ignoreCase = true) &&
                knownModelMap[upperGlobal] == null -> globalName
            knownModelMap[upperModel] != null -> return knownModelMap.getValue(upperModel)
            model.isNotBlank() -> model
            brand.isNotBlank() -> brand
            else -> "ANDROID"
        }

        return formatDeviceLabel(rawCandidate, manufacturer, brand)
    }

    internal fun formatDeviceLabel(
        rawName: String,
        manufacturer: String = "",
        brand: String = ""
    ): String {
        val trimmed = rawName.trim()
        if (trimmed.isEmpty()) return "ANDROID"

        knownModelMap[trimmed.uppercase()]?.let { return it }

        val isNothingOrCmf = listOf(trimmed, manufacturer, brand).any { value ->
            value.contains("nothing", ignoreCase = true) ||
                value.contains("cmf", ignoreCase = true)
        }

        var cleaned = trimmed
            .replace("(", " ")
            .replace(")", " ")

        if (isNothingOrCmf) {
            cleaned = cleaned
                .replace(Regex("\\bby\\s+nothing\\b", RegexOption.IGNORE_CASE), " ")
                .replace(Regex("\\bnothing\\b", RegexOption.IGNORE_CASE), " ")
                .replace(Regex("\\bphone\\b", RegexOption.IGNORE_CASE), " ")
        } else {
            cleaned = cleaned.replace(Regex("\\bphone\\b", RegexOption.IGNORE_CASE), " ")
        }

        cleaned = cleaned
            .replace('_', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
            .uppercase()

        if (cleaned.isBlank()) {
            return trimmed.uppercase()
        }

        return cleaned.take(24).trim()
    }
}
