package io.github.fvory.ekyc.util

/**
 * Indonesian phone numbers in the `+62…` form the ezSign API expects, matching formatPhone()
 * and the length check of the ezSign Flutter app (the iOS SDK uses the same rules).
 */
internal object PhoneNumber {

    private const val COUNTRY_CODE = "+62"
    private const val MIN_NATIONAL_DIGITS = 9

    /** Returns the normalized number, or null when it is too short or not numeric. */
    fun normalize(input: String): String? {
        val compact = input.filterNot { it.isWhitespace() || it == '-' }
        val national = when {
            compact.startsWith(COUNTRY_CODE) -> compact.removePrefix(COUNTRY_CODE)
            compact.startsWith("62") -> compact.removePrefix("62")
            else -> compact
        }.trimStart('0')
        if (national.length < MIN_NATIONAL_DIGITS || !national.all { it.isDigit() }) return null
        return COUNTRY_CODE + national
    }
}
