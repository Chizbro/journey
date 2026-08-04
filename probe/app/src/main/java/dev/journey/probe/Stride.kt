package dev.journey.probe

import android.content.Context

/**
 * Steps -> distance. Health Connect's native recording writes steps and nothing else,
 * so this conversion is the app's only route to a distance figure.
 *
 * Walking stride is conventionally estimated at height x 0.414. Running stride is longer,
 * which is a known and accepted inaccuracy here — the probe is measuring whether the
 * signal exists and how fresh it is, not chasing precision.
 */
object Stride {

    private const val PREFS = "probe"
    private const val KEY_HEIGHT_CM = "height_cm"
    private const val WALKING_FACTOR = 0.414
    private const val DEFAULT_HEIGHT_CM = 170

    fun heightCm(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_HEIGHT_CM, DEFAULT_HEIGHT_CM)

    fun setHeightCm(ctx: Context, cm: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_HEIGHT_CM, cm)
            .apply()
    }

    fun metresPerStep(ctx: Context): Double = heightCm(ctx) * WALKING_FACTOR / 100.0
}
