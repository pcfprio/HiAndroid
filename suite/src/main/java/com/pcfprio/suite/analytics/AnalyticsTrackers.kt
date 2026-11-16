package com.pcfprio.suite.analytics

import com.pcfprio.base.analytics.AnalyticsTracker
import com.pcfprio.base.analytics.CompositeAnalyticsTracker
import com.pcfprio.base.analytics.NoOpAnalyticsTracker

object AnalyticsTrackers {
    fun noop(): AnalyticsTracker = NoOpAnalyticsTracker

    fun composite(vararg trackers: AnalyticsTracker): AnalyticsTracker {
        return composite(trackers.toList())
    }

    fun composite(trackers: List<AnalyticsTracker>): AnalyticsTracker {
        return when (trackers.size) {
            0 -> NoOpAnalyticsTracker
            1 -> trackers.first()
            else -> CompositeAnalyticsTracker(trackers)
        }
    }
}
