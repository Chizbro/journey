package dev.journey.sync

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
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
     * Reads, credits, and advances the watermark as one indivisible step.
     *
     * The whole thing runs inside the store's lock. Reading Health Connect sits between loading
     * the state and saving it, and an import landing in that gap would otherwise be overwritten by
     * this sync's stale copy — the restore would appear to do nothing at all.
     */
    suspend fun sync(now: Instant = Instant.now()): SyncOutcome {
        var outcome: SyncOutcome = SyncOutcome.NoExpedition

        store.mutate { state ->
            if (!hasPermissions()) {
                outcome = SyncOutcome.NeedsPermission(state)
                return@mutate state
            }

            val creditTo = creditHorizon(state.syncedThrough, now)
            if (!creditTo.isAfter(state.syncedThrough)) {
                outcome = SyncOutcome.NothingNew(state)
                return@mutate state
            }

            val steps = readSteps(state.syncedThrough, creditTo)
            val metres = (steps * state.metresPerStep).toLong()

            val before = state.metresCredited
            val after = (before + metres).coerceAtMost(journey.totalMetres)

            val reached = journey.landmarks.filter {
                it.metresFromStart > before && it.metresFromStart <= after
            }
            val updated = state.copy(
                metresCredited = after,
                syncedThroughIso = creditTo.toString(),
            )

            outcome = SyncOutcome.Synced(
                state = updated,
                metresAdded = after - before,
                reached = reached,
                justFinished = after >= journey.totalMetres && before < journey.totalMetres,
                warning = updated.syncWarning(now),
            )
            updated
        }

        return outcome
    }

    /**
     * How far it is safe to credit, and therefore how far the watermark may advance.
     *
     * Records are written to Health Connect *after* the period they cover. Advance the watermark
     * past a period before its record lands and that distance is gone — we never look below the
     * watermark again.
     *
     * The obvious guard is a fixed "credit up to N minutes ago", but the only evidence we have
     * for N is the soak's `lag_seconds`, which measured `now − newest_record_end`. That number
     * cannot tell a slow write apart from a user sitting still, so calibrating against it would
     * be calibrating against the wrong hazard.
     *
     * So: **credit up to the newest record we can actually see, and no further.** If we can read
     * a record ending at T, everything up to T has been written. Anything covering a later period
     * arrives above the watermark and is picked up next time. Nothing is skipped and nothing waits.
     *
     * An earlier version held back a further twenty minutes below the newest record, to catch a
     * record landing *out of order* beneath it, with a two-hour wall-clock floor so the tail of a
     * walk was not stranded. That combination cost hours: stop walking, the newest record stops
     * moving, and the last stretch is not credited until the floor sweeps past it. Out-of-order
     * writes need a second writing app — Health Connect's own recorder batches in order, about
     * once a minute — so it was hours of latency bought against a case that cannot currently
     * arise. **Revisit if a second source ever writes steps on this device.**
     *
     * With no records visible there is nothing to anchor to, so the watermark creeps forward on
     * wall-clock instead. Nothing is lost by that: the range holds no data by definition.
     */
    private suspend fun creditHorizon(from: Instant, now: Instant): Instant =
        newestRecordEnd(from, now) ?: now.minus(IDLE_ADVANCE)

    /**
     * A raw read, used only to find the newest visible record's end — never summed. Totals come
     * from [readSteps], because only `aggregate()` deduplicates across writing apps.
     */
    private suspend fun newestRecordEnd(from: Instant, to: Instant): Instant? = runCatching {
        HealthConnectClient.getOrCreate(context)
            .readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(from, to),
                    ascendingOrder = false,
                    pageSize = 1,
                )
            )
            .records
            .firstOrNull()
            ?.endTime
    }.getOrNull()

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

    /**
     * Everything the sync decided, in readable form.
     *
     * "It isn't updating" has at least four causes that look identical from the Trail — missing
     * background permission, steps too recent to credit, steps predating the Expedition, or a
     * sync that never ran. This makes them tell themselves apart.
     */
    suspend fun diagnose(now: Instant = Instant.now()): String {
        val state = store.load() ?: return "No Expedition yet."
        val granted = runCatching {
            HealthConnectClient.getOrCreate(context)
                .permissionController.getGrantedPermissions()
        }.getOrDefault(emptySet())

        val missing = REQUIRED_PERMISSIONS - granted
        val newest = newestRecordEnd(state.syncedThrough, now)
        val horizon = creditHorizon(state.syncedThrough, now)
        val pending = runCatching { readSteps(state.syncedThrough, now) }.getOrDefault(-1)
        val creditable = runCatching { readSteps(state.syncedThrough, horizon) }.getOrDefault(-1)

        return buildString {
            appendLine("Health Connect: ${HealthConnectClient.getSdkStatus(context)}")
            appendLine(if (missing.isEmpty()) "Permissions: all granted" else "MISSING: $missing")
            appendLine()
            appendLine("Expedition began: ${state.startedAt}")
            appendLine("Credited through: ${state.syncedThrough}")
            appendLine("Newest step record: ${newest ?: "none since watermark"}")
            appendLine("Will credit up to: $horizon")
            appendLine()
            appendLine("Steps since watermark: $pending")
            appendLine("  of which creditable now: $creditable")
            appendLine("  held back as too recent: ${(pending - creditable).coerceAtLeast(0)}")
            appendLine()
            appendLine("Stride: %.3f m/step (height ${state.heightCm} cm)".format(state.metresPerStep))
            appendLine("Credited so far: %.2f km".format(state.metresCredited / 1000.0))
        }
    }

    private suspend fun hasPermissions(): Boolean = runCatching {
        HealthConnectClient.getOrCreate(context)
            .permissionController
            .getGrantedPermissions()
            .containsAll(REQUIRED_PERMISSIONS)
    }.getOrDefault(false)

    companion object {
        /**
         * How far the watermark creeps when there are no records to anchor to. Only reached
         * when the range is empty, so it can never skip anything.
         */
        private val IDLE_ADVANCE: Duration = Duration.ofMinutes(30)

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
