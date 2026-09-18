package br.gov.interpretaai.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActivityProgressPolicyTest {
    @Test fun `continues before both limits`() {
        assertNull(ActivityProgressPolicy.reason(179_999, 2, true))
    }

    @Test fun `advances at three active minutes`() {
        assertEquals(
            AssistedAdvanceReason.TIME_LIMIT,
            ActivityProgressPolicy.reason(180_000, 0, false)
        )
    }

    @Test fun `advances after three checkable attempts`() {
        assertEquals(
            AssistedAdvanceReason.ATTEMPT_LIMIT,
            ActivityProgressPolicy.reason(1_000, 3, true)
        )
    }

    @Test fun `does not call exploration an error`() {
        assertNull(ActivityProgressPolicy.reason(1_000, 20, false))
    }
}
