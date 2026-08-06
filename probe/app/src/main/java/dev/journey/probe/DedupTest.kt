package dev.journey.probe

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Does `aggregate()` actually deduplicate steps across writing apps?
 *
 * This is the load-bearing question for ADR-0007. We credit Expeditions from
 * `aggregate(StepsRecord.COUNT_TOTAL)` on the documented promise that Health Connect keeps
 * only the highest-priority source when several apps write overlapping data. If that promise
 * does not hold, every Expedition over-counts and the app is quietly wrong.
 *
 * It has never been exercised, because this device has only ever had one writer.
 *
 * The test injects a deliberately absurd StepsRecord overlapping real data, then compares
 * `aggregate()` against the raw sum:
 *
 *   aggregate unchanged            -> deduplicated; our record lost on priority. ADR-0007 holds.
 *   aggregate rose by the full lot -> NO dedup. We would double-count. ADR-0007 is wrong.
 *   aggregate rose partially       -> priority resolved per-interval. Needs thought.
 *
 * Deletes its own record afterwards. Apps can only delete data they wrote, so this cannot
 * touch the device's real step history.
 */
object DedupTest {

    private const val INJECTED_STEPS = 50_000L
    private const val WINDOW_HOURS = 3L

    suspend fun run(ctx: Context, emit: (String) -> Unit) {
        val client = HealthConnectClient.getOrCreate(ctx)
        val end = Instant.now()
        val start = end.minus(WINDOW_HOURS, ChronoUnit.HOURS)
        val range = TimeRangeFilter.between(start, end)

        emit("=== DEDUP TEST ===")
        emit("Window: last $WINDOW_HOURS hours")
        emit("")

        val aggBefore = aggregate(client, range)
        val rawBefore = rawSum(client, range)
        emit("BEFORE")
        emit("  aggregate(): ${aggBefore ?: "null"}")
        emit("  raw sum    : $rawBefore  (${sources(client, range)})")

        if (aggBefore == null || aggBefore == 0L) {
            emit("")
            emit("No real steps in this window, so an overlap cannot be created.")
            emit("Walk for a few minutes, then run this again.")
            return
        }

        emit("")
        emit("Injecting $INJECTED_STEPS steps across the same window...")
        client.insertRecords(
            listOf(
                StepsRecord(
                    startTime = start,
                    startZoneOffset = null,
                    endTime = end,
                    endZoneOffset = null,
                    count = INJECTED_STEPS,
                )
            )
        )

        val aggAfter = aggregate(client, range)
        val rawAfter = rawSum(client, range)
        emit("")
        emit("AFTER")
        emit("  aggregate(): ${aggAfter ?: "null"}")
        emit("  raw sum    : $rawAfter  (${sources(client, range)})")

        emit("")
        emit("=== VERDICT ===")
        val delta = (aggAfter ?: 0L) - aggBefore
        emit(
            when {
                delta == 0L ->
                    "DEDUPLICATED. aggregate() ignored our $INJECTED_STEPS entirely — the\n" +
                        "higher-priority source won. ADR-0007's read path is sound."

                delta >= INJECTED_STEPS ->
                    "NOT DEDUPLICATED. aggregate() added the full $INJECTED_STEPS on top of real\n" +
                        "data. Summing across sources means we would double-count.\n" +
                        "ADR-0007 needs revisiting before any of this ships."

                else ->
                    "PARTIAL: aggregate() rose by $delta of $INJECTED_STEPS. Priority is likely\n" +
                        "being resolved per sub-interval rather than per source. Worth understanding\n" +
                        "before relying on it."
            }
        )
        emit("")
        emit("Raw sum rose by ${rawAfter - rawBefore} — confirms readRecords() does NOT dedupe,")
        emit("which is why we never sum raw records.")

        cleanUp(ctx, emit)
    }

    /** Removes only records this app wrote. Real step history is untouchable from here. */
    suspend fun cleanUp(ctx: Context, emit: (String) -> Unit) {
        val client = HealthConnectClient.getOrCreate(ctx)
        val end = Instant.now()
        val start = end.minus(WINDOW_HOURS + 1, ChronoUnit.HOURS)
        client.deleteRecords(StepsRecord::class, TimeRangeFilter.between(start, end))
        emit("")
        emit("Cleaned up: deleted this app's own injected records.")
        val range = TimeRangeFilter.between(start, end)
        emit("aggregate() now: ${aggregate(client, range) ?: "null"}")
    }

    private suspend fun aggregate(client: HealthConnectClient, range: TimeRangeFilter): Long? =
        client.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), range))[StepsRecord.COUNT_TOTAL]

    private suspend fun rawSum(client: HealthConnectClient, range: TimeRangeFilter): Long =
        client.readRecords(ReadRecordsRequest(StepsRecord::class, range)).records.sumOf { it.count }

    private suspend fun sources(client: HealthConnectClient, range: TimeRangeFilter): String =
        client.readRecords(ReadRecordsRequest(StepsRecord::class, range))
            .records
            .groupingBy { it.metadata.dataOrigin.packageName }
            .eachCount()
            .entries
            .joinToString(", ") { (pkg, n) -> "${pkg.substringAfterLast('.')}:$n" }
            .ifEmpty { "no sources" }
}
