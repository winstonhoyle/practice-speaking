package io.winstonhoyle.practicespeaking.data.settings

/** Encrypts short secrets (API keys) to a Base64 string and back. */
interface SecretCipher {
    fun encrypt(plain: String): String

    fun decrypt(encoded: String): String
}
