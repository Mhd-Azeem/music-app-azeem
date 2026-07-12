package com.wavelength.music.data.cache

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private class Entry<V>(val value: V, val cachedAtMs: Long)

/**
 * In-memory stale-while-revalidate cache for repository-level network calls, mirroring the
 * backend's Cache API/KV layer on the client: a fresh hit returns instantly with no network call;
 * a stale hit still returns instantly but kicks off a background refresh so the *next* call gets
 * newer data (falling back to the last good value if that refresh fails); a miss blocks on
 * [fetch] like a normal cold call, deduplicated so concurrent misses for the same key only hit the
 * network once. Process-lifetime only (not persisted) — the OkHttp disk cache is what survives
 * app restarts; this exists to avoid re-hitting the network and re-parsing JSON for repeat calls
 * within a single app session (e.g. Home's featured/suggested/daily-mix sections all searching
 * overlapping queries).
 */
class TtlCache<K : Any, V>(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val entries = ConcurrentHashMap<K, Entry<V>>()
    private val locks = ConcurrentHashMap<K, Mutex>()

    /** [forceRefresh] skips both the fresh and stale hits below and always blocks on [fetch] for a
     * genuine call, still deduplicated via the same per-key lock — for callers like pull-to-refresh
     * where returning an instantly-cached value would defeat the point of a visible loading state. */
    suspend fun getOrPut(
        key: K,
        freshForMs: Long,
        staleForMs: Long,
        forceRefresh: Boolean = false,
        fetch: suspend () -> V
    ): V {
        if (!forceRefresh) {
            val cached = entries[key]
            val ageMs = cached?.let { System.currentTimeMillis() - it.cachedAtMs }

            if (cached != null && ageMs != null) {
                if (ageMs < freshForMs) return cached.value
                if (ageMs < freshForMs + staleForMs) {
                    refreshInBackground(key, fetch)
                    return cached.value
                }
            }
        }

        val lock = locks.getOrPut(key) { Mutex() }
        return lock.withLock {
            // Another caller may have already populated this key while we were waiting for the lock.
            val recheck = entries[key]
            val recheckAgeMs = recheck?.let { System.currentTimeMillis() - it.cachedAtMs }
            if (!forceRefresh && recheck != null && recheckAgeMs != null && recheckAgeMs < freshForMs + staleForMs) {
                recheck.value
            } else {
                val fresh = fetch()
                entries[key] = Entry(fresh, System.currentTimeMillis())
                fresh
            }
        }
    }

    private fun refreshInBackground(key: K, fetch: suspend () -> V) {
        val lock = locks.getOrPut(key) { Mutex() }
        if (!lock.tryLock()) return // a refresh (or a cold fetch) for this key is already in flight
        scope.launch {
            try {
                val fresh = fetch()
                entries[key] = Entry(fresh, System.currentTimeMillis())
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Keep serving the last good cached value; the next call will retry.
            } finally {
                lock.unlock()
            }
        }
    }
}
