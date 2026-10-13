package io.winstonhoyle.practicespeaking.data.settings

/** Light or dark appearance. [System] follows the phone's setting. */
enum class ThemeMode {
    System,
    Light,
    Dark,
    ;

    fun isDark(systemDark: Boolean): Boolean =
        when (this) {
            System -> systemDark
            Light -> false
            Dark -> true
        }
}
