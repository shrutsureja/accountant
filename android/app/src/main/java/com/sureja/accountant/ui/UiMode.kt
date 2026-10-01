package com.sureja.accountant.ui

import java.util.Locale

enum class UiMode { SIMPLE, DETAILED }

object UiModeResolver {
    fun defaultFor(username: String?): UiMode =
        if (username?.lowercase(Locale.ROOT) == "shrut") UiMode.DETAILED else UiMode.SIMPLE
}
