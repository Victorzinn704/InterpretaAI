package br.gov.interpretaai.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ReengagementPolicyTest {
    @Test fun noInvitationBeforeTwentySeconds() {
        assertEquals(ReengagementCue.NONE, ReengagementPolicy.cue(19_999, false))
    }

    @Test fun visualAtTwentyAndSingleSpeechAtFortySeconds() {
        assertEquals(ReengagementCue.VISUAL, ReengagementPolicy.cue(20_000, false))
        assertEquals(ReengagementCue.SPEAK_ONCE, ReengagementPolicy.cue(40_000, false))
        assertEquals(ReengagementCue.VISUAL, ReengagementPolicy.cue(50_000, true))
    }
}
