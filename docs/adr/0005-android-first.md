# Android first, despite iOS having the better health data

We are building for Android before iOS, using Health Connect. This is the opposite of the
technically indicated choice: HealthKit's `distanceWalkingRunning` is a single, reliably
deduplicated value on a platform where users carry the device constantly, while Android is
mid-migration from the Google Fit APIs to Health Connect.

The reason is not technical. The riskiest assumption in this product is whether ambient progress
along a famous route stays compelling over weeks and months, and that cannot be tested in a
simulator or a demo — it can only be found out by carrying the app through ordinary daily life.
The developer owns an Android device and no iOS device, so Android is the only platform on which
this app can currently be dogfooded at all. An untestable hypothesis on the better API is worth
less than a testable one on the worse API.

## Consequences

- iOS becomes a port once the design is validated, not a parallel build.
- Cross-platform frameworks were not considered a solution: health integration needs native work
  on both platforms regardless, so "write once" would save the UI and none of the hard part.
- Push-on-arrival (the core payoff moment) depends on Health Connect's background-read permission
  and its battery constraints.

## Update (2026-08-05): push-on-arrival is polled, not pushed

Verified against the documentation and AOSP. Health Connect has **no subscription, callback or
broadcast** for new data — the Changes API is itself a poll. There is nothing to be woken by.

So the arrival notification fires from a `PeriodicWorkRequest` calling `aggregate()`.
Consequences:

- **Latency is ~15 minutes at best** (WorkManager's floor), and hours under Doze. "You have
  reached Rivendell" arrives during the commute, not at the kerb. Accepted.
- `android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND` is required. It works via the
  Health Connect APK on Android 9-13, not only on Android 14+.
- No foreground service is needed. Health Connect's own rate limits are irrelevant at this
  cadence (1,000 background reads per rolling 15 minutes; a 15-minute poll uses ~96/day). Doze
  and App Standby buckets are the real limiter.

**The real app should not poll at a fixed rate.** If the next Landmark is 40 km away, no walking
rate reaches it today, so there is nothing to check for. Schedule the next poll for the earliest
physically possible arrival — a handful of polls on most days, tightening only near a Landmark.
This is both cheaper and more responsive than any fixed interval.

**Opening the app always syncs.** A foreground read on launch, before rendering progress, so the
user never sees a stale figure. This needs no background permission and has no battery cost —
the user is already looking at the screen.

That splits the two paths cleanly, and it is what makes a lazy background poll acceptable:

| | Purpose | Cadence |
| --- | --- | --- |
| **Foreground read on open** | Show accurate progress and distance-to-next-Landmark | Every launch |
| **Background poll** | Deliver the arrival notification when the app is closed | As lazy as we like |

The background poll is therefore **not** a freshness mechanism. Data is never stale when the
user is looking at it. Its only job is the surprise notification, so its cadence is purely a
question of how late that notification may arrive — not of how correct the app is.

The fixed interval remains **unresolved pending soak-test data** on whether the schedule actually
holds under Doze and OEM battery management, and on how stale Health Connect's data runs.
