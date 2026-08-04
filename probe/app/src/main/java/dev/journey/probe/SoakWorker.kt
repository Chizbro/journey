package dev.journey.probe

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * One soak sample. Deliberately does the minimum the real app would do:
 * a single aggregate() call, plus one raw read purely to measure how stale the data is.
 *
 * Records the outcome either way — a failed sample is a result, not a reason to stop.
 */
class SoakWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val sampledAt = LocalDateTime.now(ZoneId.systemDefault())

        return try {
            val client = HealthConnectClient.getOrCreate(ctx)
            val zone = ZoneId.systemDefault()
            val startOfDay = LocalDate.now(zone).atStartOfDay()
            val now = LocalDateTime.now(zone)
            val range = TimeRangeFilter.between(startOfDay, now)

            // aggregate(), not readRecords() + sum — deduplicates across sources.
            val agg = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL, DistanceRecord.DISTANCE_TOTAL),
                    timeRangeFilter = range,
                )
            )
            val steps = agg[StepsRecord.COUNT_TOTAL]
            val metres = agg[DistanceRecord.DISTANCE_TOTAL]?.inMeters

            // Freshness: how far behind real time is the newest record we can see?
            // This is what decides whether a 15-minute poll is even useful.
            val newest: Instant? = client
                .readRecords(ReadRecordsRequest(StepsRecord::class, range))
                .records
                .maxOfOrNull { it.endTime }
            val lagSeconds = newest?.let { Duration.between(it, Instant.now()).seconds }

            val estKm = steps?.let { it * Stride.metresPerStep(ctx) / 1000.0 }

            val row = listOf(
                sampledAt.toString(),
                steps?.toString() ?: "",
                metres?.let { "%.1f".format(it) } ?: "",
                estKm?.let { "%.3f".format(it) } ?: "",
                newest?.toString() ?: "",
                lagSeconds?.toString() ?: "",
                "ok",
            ).joinToString(",")

            SoakLog.append(ctx, row)
            Log.d("JourneyProbe", "soak: $row")
            Result.success()
        } catch (e: Exception) {
            // A failure is data. Never Result.failure() — that would cancel the
            // periodic work and end the soak silently.
            SoakLog.append(
                ctx,
                "$sampledAt,,,,,,ERROR ${e::class.simpleName}: ${e.message?.replace(',', ';')}",
            )
            Log.e("JourneyProbe", "soak sample failed", e)
            Result.success()
        }
    }
}
