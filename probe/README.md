# Journey probe

Throwaway. Two instruments in one app.

## Snapshot

What Health Connect holds right now, who wrote it, and whether this device can record
natively at all.

**Result on a Pixel 9 Pro, Android 16, SDK Extension 22 (2026-08-05):** native step
recording available; 237 steps written by the synthetic package
`com.android.healthconnect.phone.*`; **zero `DistanceRecord` from any source**, ever.
Health Connect held nothing at all before `READ_STEPS` was granted.

Caveat on that result: the sample was a ten-minute walk around the house on Google's own
flagship. It establishes that this device records steps and that nothing writes distance
here. It does not establish reliability, and it says nothing about mid-range hardware or
OEM battery managers.

## Soak

The part that answers what a snapshot can't. Polls every 15 or 60 minutes and appends a
row to a CSV. Leave it running through a day or two of ordinary life.

It is also a working prototype of the background poll the real app needs — same permission,
same API, same OS constraints.

```
adb pull /sdcard/Android/data/dev.journey.probe/files/soak.csv
```

Columns: `sampled_at, steps_today, metres_today, est_km, newest_record_end, lag_seconds, bucket,
charging, note`

`bucket` is the app's real App Standby bucket at sample time, read via
`UsageStatsManager.getAppStandbyBucket()` — no permission needed for the app's own bucket. It
replaces inferring the bucket from gap sizes. `charging` matters because charging exempts an app
from much of the standby throttling.

Note that the snapshot screen always shows `ACTIVE`, because opening the app promotes it. Only
the soak log records the buckets that matter.

**What to look for:**

- **Gaps between consecutive `sampled_at`** — the reliability measurement. A 15-minute
  schedule producing an 80-minute gap is Doze or the OEM battery manager deferring us.
  Expect deferral overnight; that is the OS saving battery for free.
- **`lag_seconds`** — how far behind real time the newest record is. Decides whether a
  15-minute poll is even useful, and sets the floor on how fast a "you have arrived"
  notification can possibly fire.
- **`metres_today` ever becoming non-empty** — whether `DistanceRecord` shows up once a
  fitness app is involved.
- **`ERROR` rows** — failures are recorded rather than thrown away. The worker never
  returns `Result.failure()`, since that would cancel the periodic work and end the soak
  silently.
- **Blank rows are normal, not failures.** `steps_today` counts from local midnight, and
  `aggregate()` returns **null, not zero**, when no records exist in the range. A run of
  blank rows overnight means the user was asleep, not that anything broke.

**First soak, 2026-08-05/06, ~21 hours:** battery <1%/day. The worker was never killed —
every sample `ok`, including fifteen consecutive overnight runs while charging. Gaps of
~2h and 7h43m appeared once the app went unused, consistent with App Standby buckets; the
`bucket` column exists to confirm that directly rather than by inference. `lag_seconds`
ran 14-230s, so Health Connect data is fresh within minutes and scheduling is the entire
constraint. `metres_today` never populated across ~12,000 steps.

**Battery**, after a day: Settings → Battery → Battery usage → Journey Probe. Or:

```
adb shell dumpsys batterystats --charged dev.journey.probe
```

Run the soak at **15 minutes** first — WorkManager's floor, and therefore the worst case.
If 15 is cheap then 60 is trivially cheap, and the interval becomes a UX choice rather
than a battery one. Measuring the gentle setting teaches nothing.

## Dedup test

The most important instrument here, because it checks something the app's correctness
depends on.

We credit Expeditions from `aggregate(StepsRecord.COUNT_TOTAL)` on the documented promise
that Health Connect keeps only the highest-priority source when several apps write
overlapping data. If that promise does not hold, every Expedition over-counts and the app
is quietly, invisibly wrong. It has never been exercised, because this device has only
ever had one writer.

**Run dedup test** injects 50,000 steps overlapping the last 3 hours of real data, then
compares `aggregate()` against the raw sum:

| Result | Meaning |
| --- | --- |
| aggregate unchanged | Deduplicated — our record lost on priority. ADR-0007 holds. |
| aggregate +50,000 | **No dedup.** We would double-count. ADR-0007 needs revisiting. |
| aggregate +something | Priority resolved per sub-interval. Worth understanding before relying on it. |

It deletes its own record afterwards. An app can only delete data it wrote, so this
cannot touch real step history. **Clean up injected steps** re-runs the deletion if a
test is interrupted.

Needs real steps in the window to overlap with — walk for a few minutes first, or it will
tell you there is nothing to test against.

Note this tests **steps**, not distance. Distance dedup is irrelevant to us: ADR-0007 means
the app never reads `DistanceRecord`. A GPS tracker like OpenTracks cannot exercise this
path at all, since without `ACTIVITY_RECOGNITION` it cannot write steps.

## Height

Set it before starting a soak. Stride is estimated at `height x 0.414`, which is what
turns steps into `est_km`. Running stride is longer — a known inaccuracy, and not what
this instrument is measuring.

## Deliberate omissions

- **No `readRecords()` summing.** Raw reads appear only for provenance. Totals come from
  `aggregate()`, which deduplicates across apps by the user's priority order. Summing raw
  records double-counts, and it is the easiest way to get this wrong.
- **No `dataOriginFilter`.** Filtering by origin would exclude native on-device steps,
  whose attribution changed in June 2026.
- **No adaptive scheduling.** The real app should not poll at a fixed rate — if the next
  Landmark is 40 km away, no walking rate gets you there today, so don't check. The probe
  polls at a fixed rate on purpose, to measure the worst case.

## Background

`../docs/research/health-connect-background-reads.md`
