package com.sureja.accountant.data

import org.junit.Assert.*
import org.junit.Test

class SyncMergeTest {
    @Test fun preservesAnEditMadeWhileUploadWasInFlight() { assertFalse(canApplySyncResponse("edited revision", "uploaded revision")) }
    @Test fun preservesNewUnsyncedRowsNotInUpload() { assertFalse(canApplySyncResponse("new expense", null)) }
    @Test fun acceptsAcknowledgementOfUnchangedUpload() { assertTrue(canApplySyncResponse("same revision", "same revision")) }
    @Test fun acceptsRemoteChangesWithoutPendingLocalEdits() { assertTrue(canApplySyncResponse(null, "uploaded revision")); assertTrue(canApplySyncResponse<String>(null, null)) }
}
