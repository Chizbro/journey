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
                    "PARTIAL, and this is the EXPECTED result — not a failure.\n" +
                        "aggregate() kept $delta of $INJECTED_STEPS. Health Connect deduplicates\n" +
                        "timeline segments, not whole records: where real data existed, the\n" +
                        "higher-priority source won and ours was discarded; across idle time ours\n" +
                        "was the only source, so it counted.\n" +
                        "Run the TIGHT test to contest every second and prove it properly."
            }
        )
        emit("")
        emit("Raw sum rose by ${rawAfter - rawBefore} — confirms readRecords() does NOT dedupe,")
        emit("which is why we never sum raw records.")

        cleanUp(ctx, emit)
    }

    /**
     * The tight version, and the one that actually proves anything.
     *
     * The broad test above spreads an injected record across hours of mostly-idle time, so
     * most of it has nothing to be deduplicated against and survives — which looks like
     * partial failure and is not.
     *
     * This instead injects over the *exact* span of a single real record, so every second of
     * it is contested. Either source winning is a pass; only the sum is a failure.
     */
    suspend fun runTight(ctx: Context, emit: (String) -> Unit) {
        val client = HealthConnectClient.getOrCreate(ctx)
        val end = Instant.now()
        val search = TimeRangeFilter.between(end.minus(6, ChronoUnit.HOURS), end)

        val theirs = client.readRecords(ReadRecordsRequest(StepsRecord::class, search))
            .records
            .filter { it.metadata.dataOrigin.packageName != ctx.packageName }

        emit("=== TIGHT DEDUP TEST ===")
        if (theirs.isEmpty()) {
            emit("No records from other apps in the last 6 hours. Walk a little, then retry.")
            return
        }

        val target = theirs.maxBy { it.count }
        val span = TimeRangeFilter.between(target.startTime, target.endTime)
        emit("Contesting one real record: ${target.count} steps")
        emit("  ${target.startTime} -> ${target.endTime}")
        emit("  from ${target.metadata.dataOrigin.packageName}")
        emit("")

        val before = aggregate(client, span)
        emit("BEFORE aggregate(): ${before ?: "null"}")

        client.insertRecords(
            listOf(
                StepsRecord(
                    startTime = target.startTime,
                    startZoneOffset = null,
                    endTime = target.endTime,
                    endZoneOffset = null,
                    count = INJECTED_STEPS,
                )
            )
        )

        val after = aggregate(client, span)
        val raw = rawSum(client, span)
        emit("AFTER  aggregate(): ${after ?: "null"}   (raw sum: $raw)")
        emit("")
        emit("=== VERDICT ===")

        val sum = (before ?: 0L) + INJECTED_STEPS
        emit(
            when {
                after == before ->
                    "PASS — deduplicated. Ours was discarded; the existing source has priority.\n" +
                        "ADR-0007's read path is sound."

                after != null && kotlin.math.abs(after - INJECTED_STEPS) < 100 ->
                    "PASS — deduplicated. Ours WON on priority and theirs was discarded.\n" +
                        "Either way only one source is counted, never the sum. ADR-0007 holds."

                after != null && kotlin.math.abs(after - sum) < 100 ->
                    "FAIL — aggregate() returned the SUM of both sources over identical spans.\n" +
                        "We would double-count. ADR-0007 must be revisited before shipping."

                else ->
                    "UNEXPECTED: got $after, where neither source alone ($before / $INJECTED_STEPS)\n" +
                        "nor their sum ($sum) explains it. Worth investigating."
            }
        )

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
