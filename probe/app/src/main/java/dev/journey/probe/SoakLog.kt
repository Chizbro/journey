package dev.journey.probe

import android.content.Context
import java.io.File

/**
 * Append-only CSV of soak samples, written to the app's external files dir so it can be
 * pulled without root:
 *
 *   adb pull /sdcard/Android/data/dev.journey.probe/files/soak.csv
 *
 * The columns that matter are `sampled_at` and `lag_seconds`. Gaps between consecutive
 * `sampled_at` values are the reliability measurement — a 15-minute schedule that produces
 * an 80-minute gap is Doze or the OEM battery manager deferring us, and that is exactly
 * what we are trying to find out.
 */
object SoakLog {

    private const val HEADER =
        "sampled_at,steps_today,metres_today,est_km,newest_record_end,lag_seconds,bucket,charging,note\n"

    fun file(ctx: Context) = File(ctx.getExternalFilesDir(null), "soak.csv")

    @Synchronized
    fun append(ctx: Context, row: String) {
        val f = file(ctx)
        if (!f.exists()) f.writeText(HEADER)
        f.appendText(row + "\n")
    }

    fun readLines(ctx: Context): List<String> {
        val f = file(ctx)
        return if (f.exists()) f.readLines() else emptyList()
    }

    fun clear(ctx: Context) {
        file(ctx).delete()
    }
}
