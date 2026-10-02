package com.sureja.accountant.sync

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ForegroundSyncTest {
    @Test fun syncsImmediatelyAndEveryMinuteAndStopsWhenClosed() = runTest {
        val connected = MutableStateFlow(true)
        var calls = 0
        val job = launch { runForegroundSync(connected) { calls++ } }
        runCurrent(); assertEquals(1, calls)
        advanceTimeBy(59_999); runCurrent(); assertEquals(1, calls)
        advanceTimeBy(1); runCurrent(); assertEquals(2, calls)
        job.cancel(); runCurrent()
        advanceTimeBy(120_000); assertEquals(2, calls)
    }
    @Test fun skipsOfflineAndSyncsOnReconnection() = runTest {
        val connected = MutableStateFlow(false)
        var calls = 0
        val job = launch { runForegroundSync(connected) { calls++ } }
        runCurrent(); advanceTimeBy(120_000); assertEquals(0, calls)
        connected.value = true; runCurrent(); assertEquals(1, calls)
        connected.value = false; runCurrent(); advanceTimeBy(120_000); assertEquals(1, calls)
        connected.value = true; runCurrent(); assertEquals(2, calls)
        job.cancel()
    }
}
