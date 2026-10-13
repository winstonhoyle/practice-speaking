package io.winstonhoyle.practicespeaking.live

import org.junit.Assume.assumeTrue
import java.io.File

/**
 * Gate and key source for live provider tests. Tests run only with `-Plive`, and skip when
 * the key is missing from the repo-root `.env`. Key values are never printed.
 */
object LiveEnv {
    private val values: Map<String, String> by lazy {
        val file = System.getProperty("envFile")?.let(::File)
        if (file == null || !file.exists()) return@lazy emptyMap()
        file
            .readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && "=" in it }
            .associate { line ->
                val (name, value) = line.split("=", limit = 2)
                name.trim() to value.trim().removeSurrounding("\"")
            }
    }

    /** Returns the key, or skips the calling test when live tests are off or the key is absent. */
    fun requireKey(name: String): String {
        assumeTrue("live tests run only with -Plive", System.getProperty("live") == "true")
        val key = values[name]
        assumeTrue("$name is not set in .env", !key.isNullOrBlank())
        return key!!
    }
}
