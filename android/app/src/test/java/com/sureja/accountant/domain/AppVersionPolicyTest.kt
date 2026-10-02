package com.sureja.accountant.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AppVersionPolicyTest {
    private val policy = AppVersionPolicy(2, 4, "1.3.0")
    @Test fun oldBuildIsBlocked() { assertEquals(UpdateRequirement.REQUIRED, updateRequirement(policy, 1)) }
    @Test fun supportedOlderBuildGetsOptionalUpdate() { assertEquals(UpdateRequirement.OPTIONAL, updateRequirement(policy, 2)) }
    @Test fun currentAndNewerBuildsAreAllowed() {
        assertEquals(UpdateRequirement.NONE, updateRequirement(policy, 4))
        assertEquals(UpdateRequirement.NONE, updateRequirement(policy, 5))
    }
    @Test fun absentOrInvalidPolicyDoesNotInventMandatoryUpdate() {
        assertEquals(UpdateRequirement.NONE, updateRequirement(null, 1))
        assertEquals(UpdateRequirement.NONE, updateRequirement(AppVersionPolicy(4, 2, "bad"), 1))
    }
}
