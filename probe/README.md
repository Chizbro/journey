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

Columns: `sampled_at, steps_today, metres_today, est_km, newest_record_end, lag_seconds, note`

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

**Battery**, after a day: Settings → Battery → Battery usage → Journey Probe. Or:

```
adb shell dumpsys batterystats --charged dev.journey.probe
```

Run the soak at **15 minutes** first — WorkManager's floor, and therefore the worst case.
If 15 is cheap then 60 is trivially cheap, and the interval becomes a UX choice rather
than a battery one. Measuring the gentle setting teaches nothing.

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
