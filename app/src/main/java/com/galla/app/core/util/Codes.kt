package com.galla.app.core.util

import java.security.SecureRandom

/**
 * Every item gets a short code that never changes, like "K7Q-3MX". It is printed under the
 * QR sticker so it can be read out or typed if the sticker is damaged.
 */
object Codes {
    /** No 0/O or 1/I, so a code read aloud or typed by hand cannot be confused. */
    private const val ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
    private val random = SecureRandom()
    private val pattern = Regex("^[2-9A-HJ-NP-Z]{3}-?[2-9A-HJ-NP-Z]{3}$")

    fun newCode(): String {
        val chars = CharArray(6) { ALPHABET[random.nextInt(ALPHABET.length)] }
        return String(chars, 0, 3) + "-" + String(chars, 3, 3)
    }

    /** What goes inside the QR code. The "1" is a format version, so the format can change later. */
    private const val QR_PREFIX = "galla:1:"

    fun qrPayload(code: String): String = QR_PREFIX + code

    /** The item code inside a scanned QR (or a typed code), or null if this is not a Galla sticker. */
    fun codeFromScan(text: String): String? {
        val trimmed = text.trim()
        val candidate = (if (trimmed.startsWith(QR_PREFIX)) trimmed.substring(QR_PREFIX.length) else trimmed).uppercase()
        if (!pattern.matches(candidate)) return null
        return if (candidate.contains('-')) candidate else candidate.substring(0, 3) + "-" + candidate.substring(3)
    }
}
