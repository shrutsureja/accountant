package com.sureja.accountant.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class UiModeResolverTest {
    @Test fun householdDefaultsUseAccountIdentity() {
        assertEquals(UiMode.SIMPLE, UiModeResolver.defaultFor("mom"))
        assertEquals(UiMode.SIMPLE, UiModeResolver.defaultFor("dad"))
        assertEquals(UiMode.DETAILED, UiModeResolver.defaultFor("shrut"))
    }

    @Test fun unknownIdentityStartsSimple() {
        assertEquals(UiMode.SIMPLE, UiModeResolver.defaultFor(null))
    }
}
