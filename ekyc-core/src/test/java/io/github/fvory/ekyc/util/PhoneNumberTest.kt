package io.github.fvory.ekyc.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumberTest {

    @Test
    fun normalizesCommonInputs() {
        assertEquals("+6281234567890", PhoneNumber.normalize("81234567890"))
        assertEquals("+6281234567890", PhoneNumber.normalize("081234567890"))
        assertEquals("+6281234567890", PhoneNumber.normalize("6281234567890"))
        assertEquals("+6281234567890", PhoneNumber.normalize("+6281234567890"))
        assertEquals("+6281234567890", PhoneNumber.normalize("+62 812-3456-7890"))
        assertEquals("+6281234567890", PhoneNumber.normalize("+62081234567890"))
    }

    @Test
    fun rejectsShortOrNonNumericInputs() {
        assertNull(PhoneNumber.normalize("+62"))
        assertNull(PhoneNumber.normalize("08123456"))
        assertNull(PhoneNumber.normalize("81234abc90"))
    }
}
