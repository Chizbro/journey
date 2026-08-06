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

**Adaptive scheduling was proposed here and rejected** — see the 2026-08-06 update. The idea was
to skip polls when no walking rate could reach the next Landmark. Its only justification was
battery, and battery turned out not to be a cost worth optimising.

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

## Update (2026-08-06): soak results settle the interval

A ~21-hour soak on the Pixel 9 Pro, polling every 15 minutes.

**Ask for 15 minutes and accept whatever the OS gives.** Battery cost was **<1% per day**, so
battery was never the binding constraint — and since Android controls the real cadence regardless,
requesting a longer interval buys nothing.

This also kills the adaptive-scheduling idea proposed above. Its sole justification was saving
battery we now know we are not spending. It would add scheduling complexity, and it could make
latency *worse* — schedule six hours out, the user walks 20 km unexpectedly, and we are late for
a reason we caused rather than one Android imposed. **Fixed 15-minute request. No cleverness.**

Overnight charging was confirmed for the 2026-08-05 run, supporting the exemption reading of the
tight overnight cadence.

**The worker is never killed; it is deferred.** Swiping the app from Recents is not a force-stop
and does not cancel scheduled work (only a real force-stop does, though some OEMs conflate the
two). Across the soak every sample recorded `ok`, including fifteen consecutive overnight runs.
Observed gaps map onto **App Standby buckets**:

| Observed gap | Bucket |
| --- | --- |
| ~15-19 min | Active — app opened recently |
| ~2h 16m, ~2h 57m | Working set |
| **7h 43m** | Frequent |

The tight overnight cadence was most likely the charging exemption; that part is inferred, not
confirmed.

**This degrades precisely for our ideal user.** Someone who checks in daily stays Active and gets
15-minute notifications. Someone living the ambient premise — not opening the app for days — sinks
to Frequent or Rare and hears about a Landmark hours or a day late. Nothing on our side changes
this: foreground services, exact alarms and battery-optimisation exemptions all either breach Play
policy for this use case or wreck the experience.

**Accepted, because arrival is not time-critical.** The user is not actually at Rivendell and
nothing expires. Between the foreground read on open (always correct when looked at) and ADR-0008
(delay becomes a queue of unread arrivals rather than lost ones), a late notification is a
tolerable cost rather than a broken feature.

**Data freshness was never the problem.** `lag_seconds` ran 14-230s typically, with one 789s
outlier. Health Connect's data is fresh within minutes; scheduling is the entire constraint.

Two implementation notes from the same run: `aggregate()` returns **null, not zero**, when no
records exist in the range; and `DistanceRecord` remained empty across two days and ~12,000 steps,
which is further support for ADR-0007.
