package io.winstonhoyle.practicespeaking.data

import io.winstonhoyle.practicespeaking.data.settings.SecretCipher

/** Robolectric has no Android Keystore, so tests use a reversible stand-in. */
class FakeCipher : SecretCipher {
    /** Simulates a Keystore key lost after a backup restore: every decrypt fails. */
    var broken = false

    override fun encrypt(plain: String) = "enc:" + plain.reversed()

    override fun decrypt(encoded: String): String {
        check(!broken) { "Keystore key is gone" }
        return encoded.removePrefix("enc:").reversed()
    }
}
