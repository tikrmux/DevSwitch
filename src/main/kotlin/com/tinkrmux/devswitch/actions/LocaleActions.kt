package com.tinkrmux.devswitch.actions

import com.android.ddmlib.IDevice
import com.tinkrmux.devswitch.executeShellCommand

// ============================================================================
// Locale Data
// ============================================================================

data class LocaleInfo(
    val code: String,
    val language: String,
    val country: String = "",
    val displayName: String = "$language${if (country.isNotEmpty()) " ($country)" else ""}"
)

/**
 * Common locales for testing.
 */
val COMMON_LOCALES = listOf(
    LocaleInfo("en-US", "English", "United States"),
    LocaleInfo("en-GB", "English", "United Kingdom"),
    LocaleInfo("es-ES", "Spanish", "Spain"),
    LocaleInfo("es-MX", "Spanish", "Mexico"),
    LocaleInfo("fr-FR", "French", "France"),
    LocaleInfo("de-DE", "German", "Germany"),
    LocaleInfo("it-IT", "Italian", "Italy"),
    LocaleInfo("pt-BR", "Portuguese", "Brazil"),
    LocaleInfo("pt-PT", "Portuguese", "Portugal"),
    LocaleInfo("zh-CN", "Chinese", "China (Simplified)"),
    LocaleInfo("zh-TW", "Chinese", "Taiwan (Traditional)"),
    LocaleInfo("ja-JP", "Japanese", "Japan"),
    LocaleInfo("ko-KR", "Korean", "South Korea"),
    LocaleInfo("ar-SA", "Arabic", "Saudi Arabia"),
    LocaleInfo("he-IL", "Hebrew", "Israel"),
    LocaleInfo("ru-RU", "Russian", "Russia"),
    LocaleInfo("hi-IN", "Hindi", "India"),
    LocaleInfo("th-TH", "Thai", "Thailand"),
    LocaleInfo("vi-VN", "Vietnamese", "Vietnam"),
    LocaleInfo("nl-NL", "Dutch", "Netherlands"),
    LocaleInfo("pl-PL", "Polish", "Poland"),
    LocaleInfo("tr-TR", "Turkish", "Turkey"),
    LocaleInfo("uk-UA", "Ukrainian", "Ukraine"),
    LocaleInfo("sv-SE", "Swedish", "Sweden"),
    LocaleInfo("da-DK", "Danish", "Denmark"),
    LocaleInfo("fi-FI", "Finnish", "Finland"),
    LocaleInfo("nb-NO", "Norwegian", "Norway"),
    LocaleInfo("cs-CZ", "Czech", "Czech Republic"),
    LocaleInfo("el-GR", "Greek", "Greece"),
    LocaleInfo("ro-RO", "Romanian", "Romania"),
    LocaleInfo("hu-HU", "Hungarian", "Hungary"),
    LocaleInfo("id-ID", "Indonesian", "Indonesia"),
    LocaleInfo("ms-MY", "Malay", "Malaysia")
)

/**
 * Pseudolocales for testing localization.
 */
val PSEUDOLOCALES = listOf(
    LocaleInfo("en-XA", "Pseudo-Accented", "", "Pseudo-Accented (Text expansion test)"),
    LocaleInfo("ar-XB", "Pseudo-Bidi", "", "Pseudo-Bidi (RTL mirroring test)")
)

// ============================================================================
// Locale Actions
// ============================================================================

/**
 * Set device locale.
 */
suspend fun IDevice.setLocale(localeCode: String) {
    val parts = localeCode.split("-")
    if (parts.size == 2) {
        val language = parts[0]
        val country = parts[1]
        executeShellCommand("setprop persist.sys.locale ${language}_$country")
        executeShellCommand("setprop persist.sys.language $language")
        executeShellCommand("setprop persist.sys.country $country")
        // Trigger configuration change
        executeShellCommand("am broadcast -a android.intent.action.LOCALE_CHANGED")
    }
}

/**
 * Get current device locale.
 */
suspend fun IDevice.getCurrentLocale(): String {
    val locale = executeShellCommand("getprop persist.sys.locale").trim()
    if (locale.isNotEmpty() && locale != "null") {
        return locale.replace("_", "-")
    }
    // Fallback to language + country
    val language = executeShellCommand("getprop persist.sys.language").trim()
    val country = executeShellCommand("getprop persist.sys.country").trim()
    return if (language.isNotEmpty() && country.isNotEmpty()) {
        "$language-$country"
    } else {
        "en-US"
    }
}

/**
 * Get list of available locales on device.
 */
suspend fun IDevice.getAvailableLocales(): List<String> {
    val result = executeShellCommand("pm list locales 2>/dev/null || echo ''")
    return result.lines()
        .filter { it.isNotEmpty() }
        .map { it.removePrefix("locale:").trim() }
}

// ============================================================================
// Pseudolocale Testing
// ============================================================================

/**
 * Enable pseudolocale for testing text expansion.
 */
suspend fun IDevice.enablePseudolocaleAccented() {
    setLocale("en-XA")
}

/**
 * Enable pseudolocale for testing RTL/Bidi.
 */
suspend fun IDevice.enablePseudolocaleBidi() {
    setLocale("ar-XB")
}

/**
 * Checks if pseudolocales are available on the device.
 * Pseudolocales require developer options to be enabled.
 */
suspend fun IDevice.arePseudolocalesAvailable(): Boolean {
    // Pseudolocales are available when developer options are enabled
    val developerEnabled = executeShellCommand("settings get global development_settings_enabled")
    return developerEnabled.trim() == "1"
}
