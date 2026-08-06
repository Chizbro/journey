package dev.journey.probe

import android.os.Build
import android.os.Bundle
import android.os.ext.SdkExtensions
import android.text.InputType
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Two instruments in one throwaway app.
 *
 * SNAPSHOT — what does Health Connect hold right now, and who wrote it.
 *
 * SOAK — poll every 15 or 60 minutes for a day or two of ordinary life, appending each
 * sample to a CSV. Answers the questions a snapshot cannot: do steps keep arriving, how
 * stale is the data, does the schedule actually hold under Doze and OEM battery managers,
 * and does DistanceRecord ever show up. It is also a working prototype of the background
 * poll the real app needs, tested against the real constraint rather than assumed.
 */
class MainActivity : ComponentActivity() {

    private lateinit var output: TextView
    private lateinit var heightInput: EditText

    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND,
    )

    private val requestPermissions =
        registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
            report(granted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        output = TextView(this).apply {
            setTextIsSelectable(true)
            textSize = 12f
        }
        heightInput = EditText(this).apply {
            hint = "Height in cm"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(Stride.heightCm(this@MainActivity).toString())
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            addView(heightInput)
            addView(button("Save height") {
                val cm = heightInput.text.toString().toIntOrNull()
                if (cm == null || cm !in 100..250) {
                    emit("Height must be between 100 and 250 cm.")
                } else {
                    Stride.setHeightCm(this@MainActivity, cm)
                    emit("Height $cm cm -> stride %.3f m/step".format(Stride.metresPerStep(this@MainActivity)))
                }
            })
            addView(button("Run snapshot") { clear(); probe() })
            addView(button("Start soak (15 min)") { startSoak(15) })
            addView(button("Start soak (60 min)") { startSoak(60) })
            addView(button("Stop soak") {
                WorkManager.getInstance(this@MainActivity).cancelUniqueWork(SOAK)
                emit("Soak cancelled.")
            })
            addView(button("Show soak log") { showLog() })
            addView(button("Clear soak log") {
                SoakLog.clear(this@MainActivity)
                emit("Soak log cleared.")
            })
            addView(output)
        }
        setContentView(ScrollView(this).apply { addView(root) })

        when (HealthConnectClient.getSdkStatus(this)) {
            HealthConnectClient.SDK_UNAVAILABLE ->
                emit("Health Connect is unavailable on this device. Nothing to probe.")

            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                emit("Health Connect needs updating. Update com.google.android.apps.healthdata, then relaunch.")

            else -> lifecycleScope.launch {
                val client = HealthConnectClient.getOrCreate(this@MainActivity)
                val granted = client.permissionController.getGrantedPermissions()
                if (granted.containsAll(permissions)) probe() else requestPermissions.launch(permissions)
            }
        }
    }

    private fun report(granted: Set<String>) {
        val missing = permissions - granted
        if (missing.isEmpty()) {
            probe()
        } else {
            emit("Missing permissions: $missing")
            emit("Background reads will silently return nothing without the background permission.")
            emit("Grant them in the Health Connect app, then relaunch.")
        }
    }

    private fun startSoak(minutes: Long) {
        val request = PeriodicWorkRequestBuilder<SoakWorker>(minutes, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(SOAK, ExistingPeriodicWorkPolicy.UPDATE, request)
        emit("Soak started at ${minutes}-minute intervals.")
        emit("WorkManager's floor is 15 minutes; asking for less is silently ignored.")
        emit("Log: ${SoakLog.file(this).absolutePath}")
        emit("Leave it running and live normally. Gaps in sampled_at are the finding.")
    }

    private fun showLog() {
        clear()
        val lines = SoakLog.readLines(this)
        if (lines.isEmpty()) {
            emit("No soak samples yet. The first one lands within ~15 minutes of starting.")
            return
        }
        emit("${lines.size - 1} samples  (${SoakLog.file(this).absolutePath})")
        emit("")
        lines.takeLast(40).forEach { emit(it) }
    }

    private fun probe() = lifecycleScope.launch {
        try {
            val client = HealthConnectClient.getOrCreate(this@MainActivity)
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            // 30 days: the most readable without the history permission.
            val start = today.minusDays(30).atStartOfDay()
            val end = LocalDateTime.now(zone)
            val range = TimeRangeFilter.between(start, end)

            emit("=== DEVICE CAPABILITY ===")
            emit("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            emit("Device: ${Build.MANUFACTURER} ${Build.MODEL}")

            val ext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            } else -1
            emit("SDK Extension (API 34 line): $ext")
            emit(
                when {
                    Build.VERSION.SDK_INT < 34 -> "  -> Native step recording NOT available (needs Android 14+)."
                    ext < 20 -> "  -> Native step recording NOT available (needs extension >= 20, have $ext)."
                    else -> "  -> Native step recording IS available on this device."
                }
            )

            val granted = client.permissionController.getGrantedPermissions()
            emit("Granted: ${granted.ifEmpty { "NONE" }}")
            emit(
                if (HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND in granted)
                    "  -> Background reads permitted. The soak can run."
                else
                    "  -> NO background permission. The soak will read nothing once backgrounded."
            )
            emit("Stride: %.3f m/step (height ${Stride.heightCm(this@MainActivity)} cm)"
                .format(Stride.metresPerStep(this@MainActivity)))
            emit("App Standby bucket: ${SoakWorker.standbyBucket(this@MainActivity)}" +
                "   charging: ${if (SoakWorker.isCharging(this@MainActivity)) "yes" else "no"}")
            emit("  (Opening the app promotes it to ACTIVE, so this always reads ACTIVE here.")
            emit("   The soak log is where the real buckets show up.)")
            emit("")

            emit("Window: $start  ->  $end")
            emit("Timezone: $zone")
            emit("")

            // aggregate(), never readRecords() + sum. Aggregate deduplicates across apps
            // using the user's priority order. No dataOriginFilter — filtering would
            // exclude native on-device steps.
            val totals = client.aggregate(
                AggregateRequest(
                    metrics = setOf(DistanceRecord.DISTANCE_TOTAL, StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = range,
                )
            )
            val metres = totals[DistanceRecord.DISTANCE_TOTAL]?.inMeters
            val steps = totals[StepsRecord.COUNT_TOTAL]

            emit("=== 30-DAY TOTALS (deduplicated) ===")
            emit("Distance : ${metres?.let { "%.0f m  (%.2f km)".format(it, it / 1000) } ?: "NULL — none"}")
            emit("Steps    : ${steps ?: "NULL — none"}")
            steps?.let {
                emit("Est. from steps: %.2f km".format(it * Stride.metresPerStep(this@MainActivity) / 1000))
            }
            emit("")

            if (metres != null && steps != null && steps > 0) {
                emit("Measured stride: %.3f m/step".format(metres / steps))
                emit("(Compare with the estimate above. Typical walking stride is 0.6-0.8 m.)")
            }
            emit("")

            emit("=== DAY BY DAY ===")
            val daily = client.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(DistanceRecord.DISTANCE_TOTAL, StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = range,
                    timeRangeSlicer = Period.ofDays(1),
                )
            )
            if (daily.isEmpty()) emit("(no buckets returned)")
            else daily.forEach { bucket ->
                val d = bucket.result[DistanceRecord.DISTANCE_TOTAL]?.inMeters
                val s = bucket.result[StepsRecord.COUNT_TOTAL]
                emit("%s  dist=%-10s steps=%s".format(
                    bucket.startTime.toLocalDate(),
                    d?.let { "%.0fm".format(it) } ?: "-",
                    s ?: "-",
                ))
            }
            emit("")

            // Raw reads for provenance only — never for summing.
            emit("=== WHO IS WRITING THIS DATA ===")
            val distOrigins = client.readRecords(ReadRecordsRequest(DistanceRecord::class, range))
                .records.groupingBy { it.metadata.dataOrigin.packageName }.eachCount()
            val stepOrigins = client.readRecords(ReadRecordsRequest(StepsRecord::class, range))
                .records.groupingBy { it.metadata.dataOrigin.packageName }.eachCount()

            emit("DistanceRecord sources:")
            if (distOrigins.isEmpty()) emit("  (none)")
            else distOrigins.forEach { (pkg, n) -> emit("  $pkg  ($n records)${origin(pkg)}") }

            emit("StepsRecord sources:")
            if (stepOrigins.isEmpty()) emit("  (none)")
            else stepOrigins.forEach { (pkg, n) -> emit("  $pkg  ($n records)${origin(pkg)}") }

            emit("")
            emit(
                if (stepOrigins.keys.any { isNative(it) })
                    "Native on-device step recording IS producing data."
                else "No natively-recorded steps. Everything here comes from installed apps."
            )
        } catch (e: Exception) {
            emit("FAILED: ${e::class.simpleName}: ${e.message}")
            Log.e("JourneyProbe", "probe failed", e)
        }
    }

    /**
     * Health Connect's own recording writes under a synthetic package name — "android"
     * before the June 2026 attribution change, "com.android.healthconnect.*" after it.
     */
    private fun isNative(pkg: String) =
        pkg == "android" || pkg.startsWith("com.android.healthconnect")

    private fun origin(pkg: String) =
        if (isNative(pkg)) "   <- NATIVE on-device recording" else "   <- installed app"

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { onClick() }
    }

    private fun clear() {
        output.text = ""
    }

    private fun emit(line: String) {
        Log.d("JourneyProbe", line)
        output.append(line + "\n")
    }

    private companion object {
        const val SOAK = "soak"
    }
}
