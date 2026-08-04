# Health Connect: background reads, change detection, and on-foot distance

**Date of access: 2026-08-03.** All sources are primary: `developer.android.com`, `support.google.com` (Play Console Help), AndroidX source on the `androidx/androidx` mirror, and AOSP source on `android.googlesource.com`. No blog posts, Stack Overflow, or Medium were used as authority.

---

## Summary: can we do push-on-arrival, and what does it cost?

**Push-on-arrival in the strict sense: NO.** Health Connect has no subscription, callback, listener, or broadcast that wakes your app when another app writes data. This is stated in the official sync guide — "As your app can't get notified of new data, check for new data at two points…" ([Synchronize data](https://developer.android.com/health-and-fitness/health-connect/sync-data)) — and confirmed by inspecting the platform API surface: `HealthConnectManager` exposes only settings/permission/migration intent actions, and no data-change broadcast ([`HealthConnectManager.java`, AOSP](https://android.googlesource.com/platform/packages/modules/HealthFitness/+/refs/heads/main/framework/java/android/health/connect/HealthConnectManager.java)).

**Push-on-a-short-delay: YES, and it is well supported.** You can grant your app `android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND`, run a `PeriodicWorkRequest` on WorkManager, poll Health Connect, and fire a local notification when the threshold is crossed. This is precisely the pattern Google documents, including the sample worker ([Read raw data](https://developer.android.com/health-and-fitness/health-connect/read-data)).

**What it costs:**

| Cost | Detail |
| --- | --- |
| Latency | Best realistic case is ~15 min granularity (WorkManager floor), degrading to hours under Doze / App Standby. Health Connect's own on-device step writer batches "no more frequently than once per minute", so the source data is already delayed. |
| Extra permission | One extra user-visible grant (`READ_HEALTH_DATA_IN_BACKGROUND`), separate from `READ_DISTANCE`. |
| Version floor | Health Connect APK ≥ `171302`, or Android 14 + SDK Extension ≥ 13. Must be checked at runtime. |
| Quota | 1,000 background reads / rolling 15 min and 8,000 / rolling 24 h per app (AOSP defaults). A 15-minute poll uses ~96 calls/day — roughly 1.2% of the daily background budget. Not a constraint. |
| Play Store | Health apps declaration + Data safety + privacy policy, all required for closed/open/production tracks. **Not** required for local `adb` debug builds. |
| Dogfooding | Zero Play Console involvement for a locally installed debug build. |

**Recommended approach:** `PeriodicWorkRequest` at 15 minutes with `READ_HEALTH_DATA_IN_BACKGROUND`, calling `aggregate(DistanceRecord.DISTANCE_TOTAL)` over "since journey start" (or per-day buckets), not `readRecords`. Use the Changes API only as a secondary optimisation if quota ever becomes a problem — it does not buy you push, and it adds token-expiry bookkeeping. See §3 and §4 for why `aggregate` is the important choice.

---

## 1. Background read permission: exact constant, versions, and the Android 14 split

### The constant

Confirmed. Two names for the same thing:

- **Manifest / platform string:** `android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND`
- **AndroidX Kotlin constant:** `HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND`

From AndroidX source ([`HealthPermission.kt`](https://raw.githubusercontent.com/androidx/androidx/androidx-main/health/connect/connect-client/src/main/java/androidx/health/connect/client/permission/HealthPermission.kt)):

```kotlin
internal const val PERMISSION_PREFIX = "android.permission.health."

/**
 * A permission to read data in background.
 *
 * An attempt to read data in background without this permission may result in an error.
 *
 * This feature is dependent on the version of HealthConnect installed on the device. To
 * check if it's available call [HealthConnectFeatures.getFeatureStatus] and pass
 * [HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND] as an argument.
 */
const val PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND =
    PERMISSION_PREFIX + "READ_HEALTH_DATA_IN_BACKGROUND"
```

The permission goes in `AndroidManifest.xml` as a direct child of `<manifest>`, alongside the data-type permissions ([Get started with Health Connect](https://developer.android.com/health-and-fitness/health-connect/get-started)):

```xml
<manifest>
  <uses-permission android:name="android.permission.health.READ_DISTANCE"/>
  <uses-permission android:name="android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"/>
  <application> ... </application>
</manifest>
```

> ⚠️ **Documentation inconsistency, flagged.** The [Read raw data](https://developer.android.com/health-and-fitness/health-connect/read-data) page shows the `<uses-permission>` element nested *inside* `<application>`. This contradicts the [Get started](https://developer.android.com/health-and-fitness/health-connect/get-started) page and general Android manifest schema (`uses-permission` is a child of `manifest`). Treat the Read-raw-data snippet as a documentation error; follow Get started.

### Version support — the definitive answer

The guide never publishes a version table, but the AndroidX source encodes the exact floors ([`HealthConnectFeatures.kt`](https://raw.githubusercontent.com/androidx/androidx/androidx-main/health/connect/connect-client/src/main/java/androidx/health/connect/client/HealthConnectFeatures.kt)):

```kotlin
private val SDK_EXT_13_PLATFORM_VERSION: HealthConnectPlatformVersion =
    HealthConnectPlatformVersion(buildVersionCode = 34, sdkExtensionVersion = 13)

internal val FEATURE_TO_VERSION_INFO_MAP: Map<Int, HealthConnectVersionInfo> =
    mapOf(
        FEATURE_READ_HEALTH_DATA_IN_BACKGROUND to
            HealthConnectVersionInfo(
                apkVersionCode = 171302,
                platformVersion = SDK_EXT_13_PLATFORM_VERSION,
            ),
        FEATURE_READ_HEALTH_DATA_HISTORY to
            HealthConnectVersionInfo(
                apkVersionCode = 171302,
                platformVersion = SDK_EXT_13_PLATFORM_VERSION,
            ),
        ...
    )
```

So background reads require **either**:

- **Android 14+ (API 34) with SDK Extension version ≥ 13** — Health Connect is a framework module, no APK; or
- **Android 9–13 with the Health Connect APK at version code ≥ 171302** — the standalone [Health Connect app](https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata).

### The Android 14 split, stated plainly

- **Android 14 (API 34) and higher:** Health Connect is part of the Android Framework as a framework module. No setup necessary. ([Get started](https://developer.android.com/health-and-fitness/health-connect/get-started))
- **Android 13 and lower:** Health Connect is not part of the framework; the user must install the Health Connect APK from Play. The SDK supports API 26+, but the Health Connect app itself is "only compatible with Android 9 (API level 28) or higher." ([Get started](https://developer.android.com/health-and-fitness/health-connect/get-started))
- **General caveat:** "If a feature isn't available, ask the user to update Health Connect. Features tied to the system module remain unavailable on Android 13 and lower, even with the APK." ([Get started](https://developer.android.com/health-and-fitness/health-connect/get-started)); and "Users using the APK (on Android 13 and lower) can't use the system module features that are only available on devices running Android 14 or higher." ([Check for feature availability](https://developer.android.com/health-and-fitness/health-connect/features/availability))

**Does background read behave differently across the split?** No — background read is *not* one of the Android-14-only system-module features. It has both an `apkVersionCode` and a `platformVersion` in the map above, meaning it is satisfiable on the APK. This is corroborated by the AndroidX release notes: version **1.1.0-alpha11 (15 January 2025)** — "Background and history read permissions updated to support Android 13 and below." ([Health Connect Jetpack release notes](https://developer.android.com/jetpack/androidx/releases/health-connect)). By contrast, features like `FEATURE_PLANNED_EXERCISE` and `FEATURE_PERSONAL_HEALTH_RECORD` have *only* a `platformVersion` entry — those are the Android-14-only ones.

Background read was originally added in **1.1.0-alpha09 (18 September 2024)** ("Added `READ_HEALTH_DATA_IN_BACKGROUND` permission, guarded by feature availability"). Latest stable AndroidX client is **1.1.0 (8 October 2025)**; latest alpha **1.2.0-alpha04 (22 April 2026)**. Feature-availability APIs require `androidx.health.connect:connect-client:1.1.0-alpha08` or higher ([Check for feature availability](https://developer.android.com/health-and-fitness/health-connect/features/availability)).

### Runtime check you must do

```kotlin
if (healthConnectClient.features.getFeatureStatus(
        HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND
    ) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
) { /* background read available */ }
```

`FEATURE_STATUS_UNAVAILABLE = 1`, `FEATURE_STATUS_AVAILABLE = 2` ([`HealthConnectFeatures.kt`](https://raw.githubusercontent.com/androidx/androidx/androidx-main/health/connect/connect-client/src/main/java/androidx/health/connect/client/HealthConnectFeatures.kt); [Read raw data](https://developer.android.com/health-and-fitness/health-connect/read-data)).

And: "If the user doesn't grant all of the permissions that are required for background reads, your app should still run, and it should perform as many tasks as it can with the permissions that the user granted." ([Read raw data](https://developer.android.com/health-and-fitness/health-connect/read-data))

---

## 2. Constraints on background reads

### Health Connect's own rate limits

The public guide describes the *shape* of the limits but publishes **no numbers** ([Plan to avoid rate limiting](https://developer.android.com/health-and-fitness/health-connect/rate-limiting)):

> "Limits are placed on both foreground and background API operations as **fixed request rate quotas**."
>
> "For read and changelog limits, Health Connect imposes two limits on the number of API calls available to your app: A periodic limit on the number of API calls your app can make to the API. A daily limit on the number of API calls your app can make."
>
> "Battery usage for background operations reduces the user experience and raises questions regarding data privacy. As such, background rate limiting is stricter than foreground rate limiting. It's therefore important to limit the amount of API calls your app carries out in the background."
>
> "To minimize the risk of your app being rate limited, you should utilize changelog handling to synchronize your database with data from Health Connect, rather than over-relying on raw read requests."

**The actual numbers are in AOSP.** From [`RateLimiter.java`](https://android.googlesource.com/platform/packages/modules/HealthFitness/+/refs/heads/main/framework/java/android/health/connect/ratelimiter/RateLimiter.java):

```java
public static final int QUOTA_BUCKET_READS_PER_15M_FOREGROUND_DEFAULT_FLAG_VALUE = 2000;
public static final int QUOTA_BUCKET_READS_PER_24H_FOREGROUND_DEFAULT_FLAG_VALUE = 16000;
public static final int QUOTA_BUCKET_READS_PER_15M_BACKGROUND_DEFAULT_FLAG_VALUE = 1000;
public static final int QUOTA_BUCKET_READS_PER_24H_BACKGROUND_DEFAULT_FLAG_VALUE = 8000;
public static final int QUOTA_BUCKET_WRITES_PER_15M_FOREGROUND_DEFAULT_FLAG_VALUE = 1000;
public static final int QUOTA_BUCKET_WRITES_PER_24H_FOREGROUND_DEFAULT_FLAG_VALUE = 8000;
public static final int QUOTA_BUCKET_WRITES_PER_15M_BACKGROUND_DEFAULT_FLAG_VALUE = 1000;
public static final int QUOTA_BUCKET_WRITES_PER_24H_BACKGROUND_DEFAULT_FLAG_VALUE = 8000;
public static final int CHUNK_SIZE_LIMIT_IN_BYTES_DEFAULT_FLAG_VALUE   = 5_000_000;
public static final int RECORD_SIZE_LIMIT_IN_BYTES_DEFAULT_FLAG_VALUE  = 1_000_000;
```

The windows are rolling, confirmed in the same file:

```java
case QuotaBucket.QUOTA_BUCKET_READS_PER_24H_BACKGROUND:
    return Duration.ofHours(24);
case QuotaBucket.QUOTA_BUCKET_READS_PER_15M_BACKGROUND:
    return Duration.ofMinutes(15);
```

Summary of background read quota: **1,000 API calls per rolling 15 minutes, 8,000 per rolling 24 hours, per app (per uid)**. Cost is `DEFAULT_API_CALL_COST = 1` per call.

> ⚠️ **Caveat on these numbers.** They are `*_DEFAULT_FLAG_VALUE` constants — they are DeviceConfig-overridable flags, so a given device/build can in principle carry different values. They are also from `main`, not necessarily a shipped release. Google deliberately does not publish them, so do not hard-code any assumption; still catch and back off. There is also a `setLowerRateLimitsForTesting(boolean)` that divides all quotas by 10 — test-only, not production behaviour.

**Practical read:** a 15-minute WorkManager poll doing one `aggregate()` call is 96 calls/day against an 8,000/day background budget. Rate limiting is a non-issue at our scale.

### Exception to catch

The guide shows catching `IllegalStateException` from the AndroidX client ([Read raw data](https://developer.android.com/health-and-fitness/health-connect/read-data)):

```kotlin
} catch (quotaError: IllegalStateException) {
    // Backoff
}
```

At the platform layer the error surfaces as `HealthConnectException.ERROR_RATE_LIMIT_EXCEEDED` ([`RateLimiter.java`](https://android.googlesource.com/platform/packages/modules/HealthFitness/+/refs/heads/main/framework/java/android/health/connect/ratelimiter/RateLimiter.java)).

### Pagination

`ReadRecordsRequest` has a default `pageSize` of 1000; if a response exceeds it you must iterate with `pageToken`, "However, be careful to avoid rate-limiting concerns." ([Read raw data](https://developer.android.com/health-and-fitness/health-connect/read-data))

### Doze and App Standby — the real latency constraint

This, not Health Connect's quota, is what governs how promptly the notification fires.

WorkManager minimums ([Define your work requests](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)):

> "The minimum repeat interval that can be defined is 15 minutes (same as the JobScheduler API)."
>
> "The exact time that the worker is going to be executed also depends on the constraints that are used in your work request and on system optimizations. WorkManager is designed to give the best possible behavior under these restrictions."

Doze ([Optimize for Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby)):

> "Doesn't let `JobScheduler` run… `WorkManager` uses `JobScheduler` internally, so `WorkManager` tasks don't run."
>
> "Periodically, the system exits Doze for a brief time to let apps complete their deferred activities. During this *maintenance window*, the system runs all pending syncs, jobs, and alarms, and lets apps access the network."
>
> "Over time, the system schedules maintenance windows less frequently, helping reduce battery consumption in cases of longer inactivity when the device isn't charging."

App Standby Bucket job limits ([Power management restrictions](https://developer.android.com/topic/performance/power/power-details)):

| App standby bucket | Regular jobs | Expedited jobs | Alarms | Network |
| --- | --- | --- | --- | --- |
| Active | Up to 20 minutes in a rolling 60 minute period | Up to 30 mins in a rolling 24h period | No execution limits | No restrictions |
| Working set | Up to 10 minutes in a rolling 4 hour period | Up to 15 minutes in a rolling 24h period | Limited to 10 per hour | No restrictions |
| Frequent | Up to 10 minutes in a rolling 12 hour period | Up to 10 minutes in a rolling 24h period | Limited to 2 per hour | No restrictions |
| Rare | Up to 10 minutes in a rolling 24 hour period | Up to 10 minutes in a rolling 24h period | Limited to 1 per hour | Disabled |
| Restricted | Once per day for up to 10 minutes | Up to 5 minutes in a rolling 24h window | One alarm per day | Disabled |

And per device state, when "Screen off and doze is active": "Execution limits are enforced based on the standby bucket, and execution is deferred to doze maintenance window."

**Consequence for our feature:** notification timing is inherently best-effort. A user who opens the app daily should land in Active/Working set and see ~15–60 minute latency. A user who ignores the app for a week drops to Rare/Restricted and may only get a job once a day. There is no documented way around this short of a foreground service.

### Foreground service: required?

**No, not for reading Health Connect data in the background** — that is exactly what `READ_HEALTH_DATA_IN_BACKGROUND` is for, and the documented pattern is a WorkManager `CoroutineWorker`, not a service ([Read raw data](https://developer.android.com/health-and-fitness/health-connect/read-data)).

A foreground service is only *suggested* for the foreground case: "you may consider using a foreground service to run this operation in case the user or system places your app in the background during a read operation." ([Read raw data](https://developer.android.com/health-and-fitness/health-connect/read-data))

There is one adjacent fact worth knowing, because it uses the same constant name. If you ever wanted a **`health`-type foreground service** (e.g. to poll continuously with low latency), you need `FOREGROUND_SERVICE_HEALTH` in the manifest plus one of `HIGH_SAMPLING_RATE_SENSORS`, `BODY_SENSORS` (API ≤ 35), `READ_HEART_RATE`, `READ_SKIN_TEMPERATURE`, `READ_OXYGEN_SATURATION`, or `ACTIVITY_RECOGNITION`. And ([Foreground service types are required](https://developer.android.com/about/versions/14/changes/fgs-types-required)):

> "you cannot create a `health` foreground service that uses body sensors while your app is in the background unless you've been granted the `BODY_SENSORS_BACKGROUND` (API level 33 to 35) or `READ_HEALTH_DATA_IN_BACKGROUND` (API level 36 and higher) permissions."

Note `READ_DISTANCE` is not in the qualifying list, so a `health` FGS is not a straightforward option for a distance-only app anyway. We do not need one.

---

## 3. Passive / subscription APIs — what exists and what does not

### Health Connect Changes API (`getChangesToken` / `getChanges`) — a phone API, but it is polling

It exists and works, but **it is a diff API, not a push API.** You still have to call it. From [Synchronize data](https://developer.android.com/health-and-fitness/health-connect/sync-data):

```kotlin
val changesToken = healthConnectClient.getChangesToken(
    ChangesTokenRequest(recordTypes = setOf(WeightRecord::class))
)
```

> "We recommend getting separate tokens per data type instead of getting them in bulk to avoid having an `Exception` in case one of the permissions is revoked."

```kotlin
suspend fun processChanges(context: Context, token: String): String {
    var nextChangesToken = token
    do {
        val response = healthConnectClient.getChanges(nextChangesToken)
        response.changes.forEach { change ->
            when (change) {
                is UpsertionChange ->
                    if (change.record.metadata.dataOrigin.packageName != context.packageName) {
                        processUpsertionChange(change)
                    }
                is DeletionChange -> processDeletionChange(change)
            }
        }
        nextChangesToken = response.nextChangesToken
    } while (response.hasMore)
    return nextChangesToken
}
```

Token expiry:

> "Since an unused *Changes* token expires within 30 days, you must use a sync strategy that avoids losing information in such a case."

### Is there any callback or broadcast? Definitively no.

Two independent confirmations:

1. **Documentation.** "As your app can't get notified of new data, check for new data at two points: Each time your app becomes active in the foreground… Periodically, while your app remains in the foreground…" ([Synchronize data](https://developer.android.com/health-and-fitness/health-connect/sync-data))
2. **Platform API surface.** `android.health.connect.HealthConnectManager` declares no data-change broadcast and no observer/listener registration. The only public `ACTION_*` constants are `ACTION_MANAGE_HEALTH_PERMISSIONS`, `ACTION_REQUEST_EXERCISE_ROUTE`, `ACTION_REQUEST_HEALTH_PERMISSIONS`, `ACTION_HEALTH_HOME_SETTINGS`, `ACTION_MANAGE_HEALTH_DATA`, `ACTION_SHOW_MIGRATION_INFO`, `ACTION_HEALTH_CONNECT_MIGRATION_READY`, `ACTION_SHOW_ONBOARDING` — all settings/permission/migration UI intents, none data-change. ([`HealthConnectManager.java`, AOSP](https://android.googlesource.com/platform/packages/modules/HealthFitness/+/refs/heads/main/framework/java/android/health/connect/HealthConnectManager.java))

> ⚠️ **Flagged: the sync-data page is stale.** It still asserts under "Foreground reads" that *"Apps can only read data from Health Connect while they are in the foreground."* That statement predates `READ_HEALTH_DATA_IN_BACKGROUND` (Sept 2024) and directly contradicts the current [Read raw data](https://developer.android.com/health-and-fitness/health-connect/read-data) page. The Read-raw-data page is the current one. Do not let the sync page mislead you into thinking background reads are impossible. But note the "can't get notified of new data" claim on the same page is *still true* — it was never about foreground/background.

### Health Services `PassiveMonitoringClient` — Wear OS only, does NOT apply to us

Health Services is a **Wear OS** API. "Wear OS 3 and higher includes a service called Health Services." ([Health Services on Wear OS](https://developer.android.com/health-and-fitness/health-services)). It offers the three clients `PassiveMonitoringClient`, `MeasureClient`, `ExerciseClient`, and `PassiveMonitoringClient` genuinely *does* give a callback-style `PassiveListenerService` — but only on a watch. There is no phone equivalent. **Not applicable to a phone-only app.**

### Recording API (`LocalRecordingClient`) — a phone API, but not a push API either

This is the mobile replacement for the deprecated Google Fit Android API ([Record fitness data using the Recording API](https://developer.android.com/health-and-fitness/recording-api)):

- Phone/mobile, not Wear.
- Accountless; data stored on-device.
- Records `TYPE_STEP_COUNT_DELTA`, `TYPE_DISTANCE_DELTA`, `TYPE_CALORIES_EXPENDED`.
- Retains up to **10 days** of data, and "Data is only available when there is an active subscription. If a subscription is removed by calling `unsubscribe`, collected data won't be accessible."
- **Requires Google Play services** at `LocalRecordingClient.LOCAL_RECORDING_CLIENT_MIN_VERSION_CODE`.
- It is still a *pull* model: you `subscribe`, then call `readData` whenever you want. No callback on threshold crossing.
- Google's own steer: "If your app needs to read other health and fitness data from various sources in addition to on-device steps, integrating with Health Connect is a better option. Health Connect also provides access to on-device steps natively on Android 14 (API level 34) and higher."

**Verdict:** the Recording API is a *data source*, not a notification mechanism, and it would only give us this phone's own sensor rather than the merged multi-app picture Health Connect provides. It does not solve push-on-arrival. Skip it.

### Summary table

| API | Platform | Push/callback? | Relevant to us? |
| --- | --- | --- | --- |
| Health Connect `readRecords` / `aggregate` | Phone | No — pull | **Yes, primary** |
| Health Connect `getChangesToken` / `getChanges` | Phone | No — pull, diff-based | Optional optimisation |
| Health Services `PassiveMonitoringClient` | **Wear OS 3+ only** | Yes (on watch) | **No** |
| Recording API `LocalRecordingClient` | Phone (needs Play services) | No — pull | No |

---

## 4. `DistanceRecord` vs `StepsRecord`, and the deduplication question

### Which to read

**`DistanceRecord`.** It is the literal answer to "how far did the user travel on foot", in metres. Its KDoc ([`DistanceRecord.kt`](https://raw.githubusercontent.com/androidx/androidx/androidx-main/health/connect/connect-client/src/main/java/androidx/health/connect/client/records/DistanceRecord.kt)):

> "Captures distance travelled by the user since the last reading. The total distance over an interval can be calculated by adding together all the values during the interval."

| | `DistanceRecord` | `StepsRecord` |
| --- | --- | --- |
| Record type | Interval | Interval |
| Unit | `Length` (metres) | Count |
| Read permission | `android.permission.health.READ_DISTANCE` | `android.permission.health.READ_STEPS` |
| Aggregate metric | `DISTANCE_TOTAL` | `COUNT_TOTAL` |
| Mandatory fields | `distance`, `startTime`, `endTime`, `metadata` | `count`, `startTime`, `endTime`, `metadata` |

([Health Connect data types](https://developer.android.com/health-and-fitness/health-connect/data-types))

Steps are the wrong primitive for distance — converting steps to metres requires a stride-length estimate we do not have and would be a fabricated number. `DistanceRecord` is the correct read.

> ⚠️ **Ambiguity, flagged.** Neither the data-types page nor the DistanceRecord KDoc says anything about **modality**. `DistanceRecord` is a generic distance-travelled type and is used by cycling, swimming and rowing apps too. Health Connect does not tag a `DistanceRecord` as "on foot". If ADR 0001 ("on-foot distance only") is to be honoured strictly, `DistanceRecord` alone cannot enforce it. Options — none of them documented as canonical — would be to cross-reference `ExerciseSessionRecord` for the exercise type in overlapping intervals, or to accept all distance and treat this as a known limitation. **This is an unresolved design question, not a documented fact.**
>
> ⚠️ **Practical caveat, flagged.** Health Connect's *native on-device* tracking writes **steps**, not distance ([Track steps](https://developer.android.com/health-and-fitness/health-connect/features/steps)). There is no equivalent native on-device distance writer documented. So on a device with no fitness app installed, `DistanceRecord` may simply be empty while `StepsRecord` is populated. **Verify on the dogfooding device before committing to distance-only.** A fallback to steps (with an explicit, user-visible stride assumption) may be needed.

### Deduplication — the definitive answer

**It depends entirely on which API you call.**

**`aggregate()` deduplicates. `readRecords()` does not.**

From [Read aggregated data](https://developer.android.com/health-and-fitness/health-connect/aggregate-data):

> "End users can set priority for the Sleep and Activity apps that they have integrated with Health Connect. Only end users can alter these priority lists. **When you perform an aggregate read, the Aggregate API accounts for any duplicate data and keeps only the data from the app with the highest priority.** Duplicate data could exist if the user has multiple apps writing the same kind of data—such as the number of steps taken or the distance covered—at the same time."

> "**Only the Activity and Sleep data types are deduped by Health Connect**, and the data totals shown are the values after the dedupe has been performed by the Aggregate API. These totals show the most recent full day where data exists for steps and distance. For other types of data, the aggregated results combine all data of the type in Health Connect from all apps which wrote the data."

> "Even though the Aggregate API calculates Activity and Sleep apps' data by deduping data according to how the user has set priorities, you can still build your own logic to calculate the data separately for each app writing that data."

**Distance and Steps are both in the ACTIVITY category**, so both are covered by the dedup. Confirmed in AOSP: `HealthPermissionCategory.DISTANCE = 2` and `STEPS = 6` sit under the `// ACTIVITY` grouping ([`HealthPermissionCategory.java`](https://android.googlesource.com/platform/packages/modules/HealthFitness/+/refs/heads/main/framework/java/android/health/connect/HealthPermissionCategory.java)), and `HealthDataCategory.ACTIVITY = 1` ([`HealthDataCategory.java`](https://android.googlesource.com/platform/packages/modules/HealthFitness/+/refs/heads/main/framework/java/android/health/connect/HealthDataCategory.java)).

And the explicit steer from the read guide ([Read raw data](https://developer.android.com/health-and-fitness/health-connect/read-data)):

> "**For cumulative types like `StepsRecord`, use `aggregate()` instead of `readRecords()` to avoid double counting from multiple sources and improve accuracy.**"

> "If you're interested in obtaining calculated data such as averages and totals, it is recommended to use aggregation… The aggregation API also contains logic to handle duplicate records, and lessens the chances of rate limiting."

**Conclusion: if we call `aggregate(DistanceRecord.DISTANCE_TOTAL)`, Health Connect deduplicates for us, using the priority order the user configured in the Health Connect app. If we call `readRecords(DistanceRecord::class)` and sum, we get raw overlapping records from every writing app and we double-count.** This is the single most important implementation decision in this document.

The priority order is **user-controlled only** — "Only end users can alter these priority lists." There is no API to set it, and there is no documented API on the AndroidX client to *read* it either (the platform has a `FetchDataOriginsPriorityOrderResponse`, but it is `@SystemApi`/hidden, so it is not available to a normal app).

### Data origin and package filtering

Every record carries `metadata.dataOrigin.packageName`. You can filter aggregates by origin ([Read aggregated data](https://developer.android.com/health-and-fitness/health-connect/aggregate-data)):

```kotlin
healthConnectClient.aggregate(
    AggregateRequest(
        metrics = setOf(StepsRecord.COUNT_TOTAL),
        timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
        dataOriginFilter = setOf(DataOrigin(appPackageName))
    )
)
```

For our use case we want the **opposite** — no `dataOriginFilter`, so we get the deduped merged total across all the user's sources.

### ⚠️ Recent/rolling change: on-device step attribution (June 2026)

Flagged because it is recent and only partially rolled out. From [Track steps](https://developer.android.com/health-and-fitness/health-connect/features/steps):

- On Android 14 (API 34) with **SDK Extension ≥ 20**, Health Connect provides native on-device step counting via the low-power `TYPE_STEP_COUNTER` sensor.
- "Step data is batched and written no more frequently than once per minute" — a floor on data freshness independent of our polling.
- It is only active when at least one app holds `READ_STEPS`.
- Attribution changed: before June 2026 these steps were attributed to package `"android"`; **from the June 2026 update they are attributed to a per-device, per-app Synthetic Package Name (SPN)**, e.g. `com.android.healthconnect.phone.jd5bdd37e1a8d3667a05d0abebfc4a89e`, retrievable via `getCurrentDeviceDataSource()` (Android 14, SDK Extension ≥ 11).
- Crucially: "If your app reads aggregated step counts and doesn't filter by `DataOrigin`, on-device steps are automatically included in the total with no changes required for the June 2026 update."

**Implication:** another reason to use unfiltered `aggregate()`. Any `DataOrigin` allow-listing we might be tempted to write would break on this change.

---

## 5. Historical data permission and the 30-day limit

### The constant

Confirmed: `android.permission.health.READ_HEALTH_DATA_HISTORY`, exposed as `HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY` ([`HealthPermission.kt`](https://raw.githubusercontent.com/androidx/androidx/androidx-main/health/connect/connect-client/src/main/java/androidx/health/connect/client/permission/HealthPermission.kt)).

### What exactly the limit is

The KDoc is the most precise statement available:

> "A permission that allows to read the entire history of health data (of any type).
>
> Without this permission:
> 1. Any attempt to read a single data point, via `HealthConnectClient.readRecord`, older than 30 days from before the first HealthConnect permission was granted to the calling app, will result in an error.
> 2. Any other read attempts will not return data points older than 30 days from before the first HealthConnect permission was granted to the calling app.
>
> This permission applies for the following api methods: `readRecord`, `readRecords`, `aggregate`, `aggregateGroupByPeriod`, `aggregateGroupByDuration` and `getChanges`."

Three things to note:

1. **The anchor is not "today" — it is "30 days before your app's first Health Connect permission grant".** It is a fixed floor set at grant time, not a sliding 30-day window.
2. **Single-record reads *error*; bulk reads *silently truncate*.** `readRecord` throws; `readRecords`/`aggregate`/`getChanges` just omit older data. This asymmetry was clarified in AndroidX **1.1.0-alpha11 (15 January 2025)** ([release notes](https://developer.android.com/jetpack/androidx/releases/health-connect)) — before that the docs implied everything errored.
3. `getChanges` is in the list, so the Changes API is subject to the same floor.

The guide adds the version-dependent framing ([Read raw data](https://developer.android.com/health-and-fitness/health-connect/read-data)):

> "For Android 14 and higher: No historical limit on an app reading its own data. 30-day limit on an app reading other data.
> For Android 13 and lower: 30-day limit on app reading any data."

> "If you need to extend read permissions beyond any of the default restrictions, request the `PERMISSION_READ_HEALTH_DATA_HISTORY`. Otherwise, without this permission, an attempt to read records older than 30 days results in an error."

And the reinstall behaviour:

> "If a user deletes your app, all permissions, including the history permission, are revoked. If the user reinstalls your app and grants permission again, the same default restrictions apply, and your app can read data from Health Connect for up to 30 days prior to that new date. For example, suppose the user deletes your app on May 10, 2023 and then reinstalls the app on May 15, 2023, and grants read permissions. The earliest date your app can now read data from by default is April 15, 2023."

### Does reading forward-only avoid needing it? — YES

**Definitively yes.** Our app reads only from the day the user starts a journey onward, i.e. from at-or-after the permission grant date. That is comfortably inside the default 30-day-back window, so `READ_HEALTH_DATA_HISTORY` is **not required**.

This is a meaningful win, not just a technicality:

- One fewer permission on the consent screen (better conversion, less user suspicion).
- One fewer line on the Play Console Health apps declaration to justify.
- Same version floor as background read anyway (APK ≥ 171302 / SDK Ext ≥ 13), so no compatibility gain from avoiding it — the benefit is purely UX and review friction.

**One caveat to design around:** the app must persist its own running total. If we ever tried to reconstruct a months-long journey total by re-reading Health Connect from scratch, we would hit the 30-day floor. The credited-distance ledger must live in our own datastore, with Health Connect used only as an incremental source. That is the correct architecture anyway.

Also note the reinstall case: if the user uninstalls and reinstalls, our local ledger is gone *and* we cannot read back more than 30 days to rebuild it. Journey progress should be treated as needing its own backup/restore story if we care about it surviving reinstall.

---

## 6. Google Play requirements, and whether they apply to local dogfooding

### What Google requires to publish

From [Publish your health app on Google Play](https://developer.android.com/health-and-fitness/health-connect/publish), four obligations:

**1. Policy compliance** — the app must comply with the [User data](https://support.google.com/googleplay/android-developer/answer/10144311) policy and the [Permissions and APIs that access sensitive information](https://support.google.com/googleplay/android-developer/answer/9888170) policy, which contains the Health Connect section.

**2. Data safety section:**

> "As part of your app publishing process, you must provide information for Google Play's Data safety section. This helps users understand your app's data collection, sharing, and security practices."

**3. Health apps declaration form:**

> "When your app is ready for release, the next step is to declare uses of the data types you reviewed earlier. You complete this declaration process while preparing your app for publishing on Google Play. This process must be completed for all publishing requests, both for a new app that has not been published yet, or when updating an existing, already published app that now uses a different set of data types."

Located at **Play Console → Policy → App content → Health apps**.

**4. Privacy policy:**

> "Post your app's privacy policies on its Play store page. This must be the same privacy policy that is displayed to users when they click the privacy policy link in Health Connect."

This is why the manifest needs the `ACTION_SHOW_PERMISSIONS_RATIONALE` activity (Android ≤ 13) and the `ViewPermissionUsageActivity` activity-alias with `android.intent.category.HEALTH_PERMISSIONS` (Android 14+) ([Get started](https://developer.android.com/health-and-fitness/health-connect/get-started)).

### The Health Connect policy gate

From [Permissions and APIs that access sensitive information](https://support.google.com/googleplay/android-developer/answer/9888170#ahp):

> "**Apps distributed through Google Play must meet the following policy requirements in order to read and/or write data to Health Connect.**"
>
> "Only applications or services with one or more features designed to benefit users' health and fitness are permitted to request access to Health Connect Permissions."
>
> "Approved use cases include: fitness and wellness, rewards, fitness coaching, corporate wellness, medical care, health research, and games."

A distance-tracking app that credits walking toward a goal falls squarely within "fitness and wellness" and arguably "rewards"/"games". This should be an easy justification.

### Review process and friction

From [Android Health Permissions: Guidance and FAQs](https://support.google.com/googleplay/android-developer/answer/12991134):

> "All access requests for health & fitness and body sensor permissions will be subject to review so that the use of this sensitive data aligns with approved use cases."

Requirements: clear justification per permission, documentation of how data benefits users, minimum-necessary access ("Do not request broader access than necessary"), and a comprehensive privacy policy. Heightened scrutiny applies to sensitive categories — "reproductive health (for example, `READ_MENSTRUAL_CYCLE_PHASE`), substance use (for example, `READ_ALCOHOL_CONSUMPTION`), clinical vitals." **`READ_DISTANCE` and `READ_STEPS` are not in the heightened-scrutiny list**, which should mean lower friction for us.

If denied: "Developers can revise and resubmit their requests with additional information or clarification." Common causes are incomplete justifications, misalignment with approved use cases, and insufficient detail.

> ⚠️ **Not documented: review turnaround times.** Neither the developer.android.com publish page nor the Play Console Help pages state an SLA or typical duration for health permission review. Any number you have heard for this is anecdotal, not primary-sourced. **I could not find a definitive answer and am not going to guess.**

Also note the historical deadlines, now well past and included only so nobody re-derives them as live: the Health apps declaration became mandatory after **31 August 2024** ([Health apps declaration form](https://support.google.com/googleplay/android-developer/answer/14738291)), and legacy Google Health Connect API Request form users had to migrate their declaration by **22 January 2025** ([Publish your health app](https://developer.android.com/health-and-fitness/health-connect/publish)).

### Does any of this apply to a local `adb` debug build? — NO

**The developer can dogfood freely with zero Play Console involvement.** Reasoning, from primary sources:

1. The Health Connect policy is explicitly scoped: "**Apps distributed through Google Play** must meet the following policy requirements in order to read and/or write data to Health Connect." ([9888170](https://support.google.com/googleplay/android-developer/answer/9888170#ahp)) A debug APK sideloaded via `adb install` is not distributed through Google Play.
2. The Health apps declaration is scoped to published apps: "**All developers that have an app published on Google Play** must complete the Health apps declaration, including apps on closed testing, open testing, or production tracks." ([14738291](https://support.google.com/googleplay/android-developer/answer/14738291))
3. There is **no technical gate**. Nothing in the Health Connect permission grant flow, in `HealthConnectClient`, or in the platform `HealthConnectManager` consults Play Console state. The permissions are ordinary Android runtime permissions in the `android.permission.health.*` group, granted by the user through the Health Connect permission UI. Confirmed by the absence of any such check across [`HealthPermission.kt`](https://raw.githubusercontent.com/androidx/androidx/androidx-main/health/connect/connect-client/src/main/java/androidx/health/connect/client/permission/HealthPermission.kt), [`HealthConnectFeatures.kt`](https://raw.githubusercontent.com/androidx/androidx/androidx-main/health/connect/connect-client/src/main/java/androidx/health/connect/client/HealthConnectFeatures.kt) and [`HealthConnectManager.java`](https://android.googlesource.com/platform/packages/modules/HealthFitness/+/refs/heads/main/framework/java/android/health/connect/HealthConnectManager.java).

> Note on rigour: points 1 and 2 are *scoping* statements — Google says what the requirements apply to, rather than affirmatively saying "sideloaded apps are exempt." I could find **no primary source that explicitly addresses non-Play distribution of Health Connect apps.** The conclusion is a sound reading of the scoping language plus the absence of any technical enforcement, but it is inference, not a quoted exemption. It is a safe inference for a solo developer on their own device.

### Does an internal-testing track release trigger the declaration? — Probably not, but this is genuinely ambiguous

This is the one question where the documentation is unsatisfying, so here it is precisely.

**Data safety section — explicitly exempts internal testing.** From [Provide information for Google Play's Data safety section](https://support.google.com/googleplay/android-developer/answer/10787469):

> "Apps that are active on internal testing tracks are exempt from inclusion in the data safety section."
>
> "All developers that have an app published on Google Play must complete the Data safety form, including apps on closed, open, or production testing tracks."

**Health apps declaration — internal testing is not mentioned at all.** From [Provide information for the Health apps declaration form](https://support.google.com/googleplay/android-developer/answer/14738291):

> "All developers that have an app published on Google Play must complete the Health apps declaration, including apps on closed testing, open testing, or production tracks."
>
> "System services and private apps do not need to complete the Health apps declaration."

The enumerated tracks are **closed, open, production** — internal testing is conspicuously absent, exactly mirroring the Data safety wording where internal testing *is* explicitly exempted. The parallel structure strongly suggests the same exemption is intended.

> ⚠️ **Flagged as unverified.** The Health apps declaration page **does not use the phrase "internal testing" anywhere**, so unlike Data safety there is no affirmative exemption to quote. The listed exemptions are only "system services and private apps". I cannot confirm from primary sources whether Play Console will block an internal-testing rollout for a missing Health apps declaration. Treat "internal testing is exempt" as a reasonable inference from parallel wording, **not** an established fact. If we ever want internal testing, the cheap move is to just complete the declaration — it is a form, not a review gate for our (non-heightened-scrutiny) permissions.

### Practical staging for this project

| Stage | Play Console work needed |
| --- | --- |
| `adb install` debug build on own device | **None.** |
| Internal testing track | Data safety: explicitly exempt. Health apps declaration: probably exempt, unverified — just fill it in. |
| Closed / open testing | Data safety + Health apps declaration + privacy policy URL + policy compliance, all required. |
| Production | Same as above; permission access subject to review. |

The declaration is not the wall it is sometimes made out to be for a distance app — `READ_DISTANCE` is a non-heightened-scrutiny permission with an obvious fitness-and-wellness justification.

---

## Open questions / unverified

Things I could not settle from primary sources, stated plainly rather than guessed:

1. **Play review turnaround time for health permissions.** No SLA or typical duration published anywhere I could find on developer.android.com or Play Console Help. Unknown.
2. **Whether internal-testing releases require the Health apps declaration.** Inferred exempt from parallel wording with the Data safety page, but never stated. See §6.
3. **Exact production rate-limit values.** The AOSP numbers in §2 are `*_DEFAULT_FLAG_VALUE` constants from `main` and are DeviceConfig-overridable. They are the right order of magnitude and Google deliberately does not publish them. Do not hard-code assumptions; always catch and back off.
4. **Whether `DistanceRecord` can be constrained to on-foot travel.** Health Connect does not tag distance with a modality. Cycling/swimming/rowing apps write `DistanceRecord` too. No documented mechanism to filter to walking/running. Cross-referencing `ExerciseSessionRecord` is a plausible but undocumented workaround. **Unresolved, and it bears directly on ADR 0001.**
5. **Whether `DistanceRecord` will be populated at all on a bare device.** Health Connect's native on-device tracking writes *steps*, not distance ([Track steps](https://developer.android.com/health-and-fitness/health-connect/features/steps) documents no native distance writer). If no fitness app is installed, distance may be empty. **Must be verified empirically on the dogfooding device — this is the first thing to test.**
6. **Reading the user's data-source priority order.** The dedup depends on it, but there is no documented public AndroidX API to read it (`FetchDataOriginsPriorityOrderResponse` exists in AOSP but is `@SystemApi`). We cannot show the user which source won, or explain a discrepancy.
7. **Real-world background-read latency under Doze.** The documented bucket limits (§2) give worst cases, but actual observed latency for a user who opens the app daily is not something the docs commit to. Measure it during dogfooding.
8. **Documentation inconsistencies flagged, both worth re-checking later:**
   - [Read raw data](https://developer.android.com/health-and-fitness/health-connect/read-data) shows `<uses-permission>` nested inside `<application>`, contradicting [Get started](https://developer.android.com/health-and-fitness/health-connect/get-started) and the manifest schema. Almost certainly a doc bug.
   - [Synchronize data](https://developer.android.com/health-and-fitness/health-connect/sync-data) still says "Apps can only read data from Health Connect while they are in the foreground", which has been false since background reads shipped in Sept 2024. That page has not been updated. (Its separate claim that apps "can't get notified of new data" remains true.)
9. **Recently changed / still rolling out:** the June 2026 on-device step attribution change from package `"android"` to a per-device Synthetic Package Name ([Track steps](https://developer.android.com/health-and-fitness/health-connect/features/steps)). Not a problem for us as long as we never use `dataOriginFilter`, but it is fresh and worth watching.

---

## Sources

- [Read raw data — Health Connect](https://developer.android.com/health-and-fitness/health-connect/read-data)
- [Read aggregated data — Health Connect](https://developer.android.com/health-and-fitness/health-connect/aggregate-data)
- [Synchronize data — Health Connect](https://developer.android.com/health-and-fitness/health-connect/sync-data)
- [Plan to avoid rate limiting — Health Connect](https://developer.android.com/health-and-fitness/health-connect/rate-limiting)
- [Get started with Health Connect](https://developer.android.com/health-and-fitness/health-connect/get-started)
- [Check for feature availability — Health Connect](https://developer.android.com/health-and-fitness/health-connect/features/availability)
- [Health Connect data types](https://developer.android.com/health-and-fitness/health-connect/data-types)
- [Track steps — Health Connect](https://developer.android.com/health-and-fitness/health-connect/features/steps)
- [Publish your health app on Google Play](https://developer.android.com/health-and-fitness/health-connect/publish)
- [Health Connect Jetpack release notes](https://developer.android.com/jetpack/androidx/releases/health-connect)
- [Health Services on Wear OS](https://developer.android.com/health-and-fitness/health-services)
- [Record fitness data using the Recording API](https://developer.android.com/health-and-fitness/recording-api)
- [Define your work requests — WorkManager](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)
- [Optimize for Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby)
- [Power management restrictions](https://developer.android.com/topic/performance/power/power-details)
- [Foreground service types are required (Android 14)](https://developer.android.com/about/versions/14/changes/fgs-types-required)
- [Permissions and APIs that access sensitive information — Play Console Help](https://support.google.com/googleplay/android-developer/answer/9888170)
- [Android Health Permissions: Guidance and FAQs — Play Console Help](https://support.google.com/googleplay/android-developer/answer/12991134)
- [Provide information for the Health apps declaration form — Play Console Help](https://support.google.com/googleplay/android-developer/answer/14738291)
- [Provide information for Google Play's Data safety section — Play Console Help](https://support.google.com/googleplay/android-developer/answer/10787469)
- [AndroidX `HealthPermission.kt`](https://raw.githubusercontent.com/androidx/androidx/androidx-main/health/connect/connect-client/src/main/java/androidx/health/connect/client/permission/HealthPermission.kt)
- [AndroidX `HealthConnectFeatures.kt`](https://raw.githubusercontent.com/androidx/androidx/androidx-main/health/connect/connect-client/src/main/java/androidx/health/connect/client/HealthConnectFeatures.kt)
- [AndroidX `DistanceRecord.kt`](https://raw.githubusercontent.com/androidx/androidx/androidx-main/health/connect/connect-client/src/main/java/androidx/health/connect/client/records/DistanceRecord.kt)
- [AOSP `RateLimiter.java`](https://android.googlesource.com/platform/packages/modules/HealthFitness/+/refs/heads/main/framework/java/android/health/connect/ratelimiter/RateLimiter.java)
- [AOSP `HealthConnectManager.java`](https://android.googlesource.com/platform/packages/modules/HealthFitness/+/refs/heads/main/framework/java/android/health/connect/HealthConnectManager.java)
- [AOSP `HealthPermissionCategory.java`](https://android.googlesource.com/platform/packages/modules/HealthFitness/+/refs/heads/main/framework/java/android/health/connect/HealthPermissionCategory.java)
- [AOSP `HealthDataCategory.java`](https://android.googlesource.com/platform/packages/modules/HealthFitness/+/refs/heads/main/framework/java/android/health/connect/HealthDataCategory.java)
