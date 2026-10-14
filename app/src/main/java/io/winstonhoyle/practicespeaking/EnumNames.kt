package io.winstonhoyle.practicespeaking

/** The [E] entry stored under [name] (settings and the database save enums by name), or null for an unknown or missing one. */
inline fun <reified E : Enum<E>> enumByName(name: String?): E? = enumValues<E>().firstOrNull { it.name == name }
