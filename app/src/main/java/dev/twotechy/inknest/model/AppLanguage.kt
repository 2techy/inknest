package dev.twotechy.inknest.model

internal enum class AppLanguage(val tag: String) {
    System(""),
    English("en"),
    Vietnamese("vi"),
    ;

    companion object {
        fun fromLanguageTag(tag: String?): AppLanguage = when (tag?.substringBefore('-')) {
            "en" -> English
            "vi" -> Vietnamese
            else -> System
        }
    }
}
