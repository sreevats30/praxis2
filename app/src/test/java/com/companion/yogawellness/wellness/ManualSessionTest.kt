package com.companion.yogawellness.wellness

import org.junit.Assert.assertEquals
import org.junit.Test

/** Duration formatting tests for session history. */
class ManualSessionTest {

    @Test
    fun durationFormatting() {
        assertEquals("42s", HistoryAggregator.formatDuration(42.0))
        assertEquals("3m 20s", HistoryAggregator.formatDuration(200.0))
        assertEquals("30m", HistoryAggregator.formatDuration(1800.0))
    }
}
