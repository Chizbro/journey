package dev.journey.sync

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dev.journey.data.ExpeditionState
import dev.journey.data.ExpeditionStore
import dev.journey.domain.Journey
import dev.journey.domain.Landmark
import java.time.Duration
import java.time.Instant

/**
 * Turns steps into distance, and distance into Landmarks reached.
 *
 * Reads `aggregate(StepsRecord.COUNT_TOTAL)` — never `readRecords()` and a sum. Aggregate
 * deduplicates across writing apps by the user's priority order; summing raw records
 * double-counts. Verified on device, ADR-0007.
 */
class SyncEngine(
    private val context: Context,
    private val store: ExpeditionStore,
    private val journey: Journey,
) {

    /**
     * Health Connect data arrives late — the soak measured a typical lag of seconds to minutes,
     * with an outlier past thirteen. If the watermark were advanced to *now*, steps written a
     * few minutes after the fact would fall behind it and be lost silently.
     *
     * So we only ever credit up to [SETTLE] ago, and leave the tail for the next run. Progress
     * therefore trails real time by about twenty minutes, which for an ambient app is nothing.
     */
    private val settle: Duration = SETTLE

    suspend fun sync(now: Instant = Instant.now()): SyncOutcome {
        val state = store.load() ?: return SyncOutcome.NoExpedition
        if (!hasPermissions()) return SyncOutcome.NeedsPermission(state)

        val creditTo = now.minus(settle)
        if (!creditTo.isAfter(state.syncedThrough)) return SyncOutcome.NothingNew(state)

        val steps = readSteps(state.syncedThrough, creditTo)
        val metres = (steps * state.metresPerStep).toLong()

        val before = state.metresCredited
        val after = (before + metres).coerceAtMost(journey.totalMetres)

        val reached = journey.landmarks.filter {
            it.metresFromStart > before && it.metresFromStart <= after
        }
        val finished = after >= journey.totalMetres && before < journey.totalMetres

        val updated = state.copy(
            metresCredited = after,
            syncedThroughIso = creditTo.toString(),
        )
        store.save(updated)

        return SyncOutcome.Synced(
            state = updated,
            metresAdded = after - before,
            reached = reached,
            justFinished = finished,
            warning = updated.syncWarning(now),
        )
    }

    /**
     * Aggregate returns **null**, not zero, when the range holds no records — a sleeping user
     * looks identical to a broken read. Treat it as zero and move on.
     */
    private suspend fun readSteps(from: Instant, to: Instant): Long {
        val client = HealthConnectClient.getOrCreate(context)
        val result = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(from, to),
                // No dataOriginFilter: filtering by origin would exclude natively recorded steps.
            )
        )
        return result[StepsRecord.COUNT_TOTAL] ?: 0L
    }

    private suspend fun hasPermissions(): Boolean = runCatching {
        HealthConnectClient.getOrCreate(context)
            .permissionController
            .getGrantedPermissions()
            .containsAll(REQUIRED_PERMISSIONS)
    }.getOrDefault(false)

    companion object {
        private val SETTLE: Duration = Duration.ofMinutes(20)

        val REQUIRED_PERMISSIONS = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND,
        )

        fun isAvailable(context: Context): Boolean =
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }
}

sealed interface SyncOutcome {
    data object NoExpedition : SyncOutcome
    data class NeedsPermission(val state: ExpeditionState) : SyncOutcome
    data class NothingNew(val state: ExpeditionState) : SyncOutcome

    data class Synced(
        val state: ExpeditionState,
        val metresAdded: Long,
        /** Landmarks crossed by this sync, in order. Several at once is the normal case. */
        val reached: List<Landmark>,
        val justFinished: Boolean,
        val warning: dev.journey.data.SyncWarning?,
    ) : SyncOutcome
}
