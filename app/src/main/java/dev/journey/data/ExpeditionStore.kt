package dev.journey.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant

/**
 * The only copy of the user's progress that exists anywhere.
 *
 * Writes go through a temp file and a rename, because a torn write here is not a corrupted cache —
 * it is an Expedition gone for good (ADR-0006). The same reasoning is why export exists before any
 * of the interesting features do.
 */
class ExpeditionStore(private val context: Context) {

    private val file: File get() = File(context.filesDir, FILE_NAME)
    private val lock = Mutex()

    suspend fun load(): ExpeditionState? = withContext(Dispatchers.IO) {
        lock.withLock {
            if (!file.exists()) return@withLock null
            decodeExpedition(file.readText()).getOrNull()
        }
    }

    suspend fun save(state: ExpeditionState): Unit = withContext(Dispatchers.IO) {
        lock.withLock { writeAtomically(state.encode()) }
    }

    /** Start a fresh Expedition. Zero distance, watermark at now — never reaching backwards. */
    suspend fun begin(journeyId: String, heightCm: Int, now: Instant): ExpeditionState {
        val state = ExpeditionState(
            journeyId = journeyId,
            startedAtIso = now.toString(),
            syncedThroughIso = now.toString(),
            heightCm = heightCm,
        )
        save(state)
        return state
    }

    /**
     * Read-modify-write under the lock.
     *
     * Marking entries read happens a tap apart while a sync may also be writing, so callers must
     * not build an update from a state they read earlier — the loser of that race silently drops
     * whatever the winner wrote.
     */
    suspend fun update(transform: (ExpeditionState) -> ExpeditionState): ExpeditionState? =
        withContext(Dispatchers.IO) {
            lock.withLock {
                val current = if (file.exists()) decodeExpedition(file.readText()).getOrNull() else null
                current?.let { transform(it).also { updated -> writeAtomically(updated.encode()) } }
            }
        }

    /** Exactly what is on disk. Export is a copy, not a conversion. */
    suspend fun export(): String? = load()?.encode()

    /**
     * Replaces everything. Validated before it lands, so a malformed file cannot destroy a
     * working Expedition — the failure mode this guards against is importing over live progress.
     */
    suspend fun import(text: String): Result<ExpeditionState> {
        val decoded = decodeExpedition(text)
        decoded.getOrNull()?.let { save(it) }
        return decoded
    }

    private fun writeAtomically(text: String) {
        val temp = File(context.filesDir, "$FILE_NAME.tmp")
        temp.writeText(text)
        check(temp.renameTo(file)) { "Could not replace $FILE_NAME" }
    }

    private companion object {
        const val FILE_NAME = "expedition.json"
    }
}
