package dev.journey.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.Instant

/**
 * One user's traversal of a single Journey.
 *
 * We persist the *outcome* of reading Health Connect, never the readings — see ADR-0006. Each
 * sync reads from [syncedThroughIso] to a point shortly before now, credits the distance, and
 * advances the watermark. Individual readings answer no question once credited.
 *
 * This record cannot be rebuilt from Health Connect, which only exposes 30 days anchored at the
 * moment permission was first granted. With no account and no server, this JSON *is* the user's
 * progress. It is also, deliberately, exactly what export writes out.
 */
@Serializable
data class ExpeditionState(
    val version: Int = 1,
    val journeyId: String,
    val startedAtIso: String,
    /** Distance credited so far, in metres. The only number that matters. */
    val metresCredited: Long = 0,
    /** We have credited everything up to this instant. Never move it forward speculatively. */
    val syncedThroughIso: String,
    /** Landmarks (and the ending) the user has actually read. */
    val readIds: Set<String> = emptySet(),
    val heightCm: Int,
) {
    val startedAt: Instant get() = Instant.parse(startedAtIso)
    val syncedThrough: Instant get() = Instant.parse(syncedThroughIso)

    /** Walking stride estimated from height. The only personal measurement the app needs. */
    val metresPerStep: Double get() = heightCm * WALKING_STRIDE_FACTOR / 100.0

    fun staleness(now: Instant): Duration = Duration.between(syncedThrough, now)

    /**
     * Health Connect only serves the last 30 days without the history permission, which we do not
     * request. A gap longer than that is distance permanently lost, so warn well before the cliff
     * rather than at it.
     */
    fun syncWarning(now: Instant): SyncWarning? {
        val days = staleness(now).toDays()
        return when {
            days >= HISTORY_WINDOW_DAYS -> SyncWarning.DataLost(days)
            days >= WARN_AFTER_DAYS -> SyncWarning.GoingStale(days, HISTORY_WINDOW_DAYS - days)
            else -> null
        }
    }

    companion object {
        const val WALKING_STRIDE_FACTOR = 0.414
        const val HISTORY_WINDOW_DAYS = 30L
        const val WARN_AFTER_DAYS = 21L
    }
}

sealed interface SyncWarning {
    /** Approaching the 30-day cliff. Still recoverable if the app syncs. */
    data class GoingStale(val daysSinceSync: Long, val daysRemaining: Long) : SyncWarning

    /** Past it. Some distance is gone and cannot be recovered by any means. */
    data class DataLost(val daysSinceSync: Long) : SyncWarning
}

/** Export and import speak this. It is the storage format, unchanged — no separate mapping. */
val ExpeditionJson: Json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun ExpeditionState.encode(): String = ExpeditionJson.encodeToString(this)

fun decodeExpedition(text: String): Result<ExpeditionState> = runCatching {
    ExpeditionJson.decodeFromString<ExpeditionState>(text).also {
        require(it.version == 1) { "Unsupported export version ${it.version}" }
        require(it.heightCm in 100..250) { "Implausible height ${it.heightCm} cm" }
        require(it.metresCredited >= 0) { "Negative distance" }
        Instant.parse(it.startedAtIso)
        Instant.parse(it.syncedThroughIso)
    }
}
