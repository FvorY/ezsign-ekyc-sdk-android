package io.github.fvory.ekyc

import android.app.Activity
import io.github.fvory.ekyc.model.EkycResult
import org.junit.Assert.assertEquals
import org.junit.Test

class EzsignEkycContractTest {
    @Test
    fun closingSdkWithoutResultReturnsCancelled() {
        val result = EzsignEkyc.Contract().parseResult(Activity.RESULT_CANCELED, null)
        assertEquals(EkycResult.Cancelled, result)
    }
}
