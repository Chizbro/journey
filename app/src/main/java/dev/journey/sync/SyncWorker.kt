package dev.journey.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.journey.content.HADRIANS_WALL
import dev.journey.data.ExpeditionStore
import java.util.concurrent.TimeUnit

/**
 * The background half of the sync.
 *
 * Its only job is to notice arrivals while the app is closed. It is **not** a freshness mechanism:
 * opening the app always syncs first, so the user never sees a stale figure (ADR-0005). That is
 * what makes it acceptable for Android to defer this by hours.
 *
 * Fifteen minutes is WorkManager's floor and costs under 1% of battery a day. Asking for longer
 * buys nothing, because App Standby buckets take the real cadence out of our hands regardless —
 * a soak measured deferrals of two hours and, once the app went unused, nearly eight.
 */
class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            val store = ExpeditionStore(applicationContext)
            val engine = SyncEngine(applicationContext, store, HADRIANS_WALL)
            engine.sync()
            announceIfDue(store)
            Result.success()
        } catch (e: Exception) {
            // Never Result.failure() — that cancels the periodic work and the app stops syncing
            // silently, which given the 30-day history cliff is data loss, not an inconvenience.
            Log.e(TAG, "sync failed", e)
            Result.success()
        }
    }

    /**
     * Announces what is unread rather than what this sync crossed, so an arrival held back
     * overnight is still announced in the morning without the worker having to remember it.
     */
    private suspend fun announceIfDue(store: ExpeditionStore) {
        if (!Arrivals.isWakingHour()) {
            Log.d(TAG, "arrival held until waking hours")
            return
        }
        val state = store.load() ?: return
        val announcement = Arrivals.pending(HADRIANS_WALL, state) ?: return
        Arrivals.post(applicationContext, announcement)
        store.update { it.copy(lastNotifiedId = announcement.id) }
        Log.d(TAG, "announced ${announcement.id}")
    }

    companion object {
        private const val TAG = "JourneySync"
        private const val NAME = "journey-sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.NONE)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
