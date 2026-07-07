package com.wavelength.music.ui.search

import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Lets Home's "recent searches" chips jump to the Search tab with a query pre-filled, without
 * threading a nav argument through the bottom-nav route (which also handles plain tab taps). */
@Singleton
class PendingSearchQuery @Inject constructor() {
    private val pending = MutableStateFlow<String?>(null)

    fun set(query: String) {
        pending.value = query
    }

    fun consume(): String? {
        val value = pending.value
        pending.value = null
        return value
    }
}
