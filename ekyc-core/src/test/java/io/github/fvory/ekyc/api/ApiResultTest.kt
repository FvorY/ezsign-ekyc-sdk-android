package io.github.fvory.ekyc.api

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class ApiResultTest {
    @Test
    fun leavingScreenPropagatesRequestCancellation() {
        val cancelled = CancellationException("View destroyed")
        try {
            apiResult<Unit> { throw cancelled }
            fail("A cancelled request must not reach UI failure callbacks")
        } catch (actual: CancellationException) {
            assertSame(cancelled, actual)
        }
    }
}
