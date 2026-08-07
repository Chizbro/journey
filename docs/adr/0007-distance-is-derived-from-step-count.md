# Distance is derived from step count, not read directly

We read `StepsRecord` from Health Connect and convert to distance using a stride length
estimated from the user's height (`height x 0.414`). We do not read `DistanceRecord`, even
though it is the literal answer to "how far did they travel".

## Why

**Distance is not guaranteed to exist.** Health Connect's native on-device recording writes
steps and nothing else; no native distance writer is documented. `DistanceRecord` can therefore
only ever arrive from an installed third-party app. An app that works only if the user happens
to have the right fitness app installed is not shippable, and "install this, then install and
configure something else first" destroys the effortless-ambient-progress premise.

**Distance is untrustworthy even when present.** `DistanceRecord` carries no modality tag —
cycling, swimming and rowing apps write to it, and there is no documented way to filter to
on-foot travel. Steps cannot come from a bike or a pool. So reading steps is what makes
ADR-0001's on-foot-only rule enforceable by construction rather than by hope.

**The risk is asymmetric.** Build for steps and distance turns out to be available: we lose some
accuracy. Build for distance and it is absent: the app does not function.

## Evidence, and its limits

A probe (`probe/`) run on a Pixel 9 Pro, Android 16, SDK Extension 22 on 2026-08-05 found
native step recording available, 237 steps written by the synthetic package
`com.android.healthconnect.phone.*`, and **zero `DistanceRecord` from any source**. Health
Connect held nothing at all before `READ_STEPS` was granted.

That sample was a ten-minute indoor walk on Google's own flagship hardware. It establishes that
this device records steps natively and that nothing writes distance on it. It does **not**
establish reliability over time, and it says nothing about mid-range or heavily-skinned devices.
The decision above rests on the documented absence of a native distance writer and on the risk
asymmetry — not on this sample, which is too small to carry it.

## Update (2026-08-07): deduplication verified on device

The read path assumes `aggregate(StepsRecord.COUNT_TOTAL)` keeps only the highest-priority source
when several apps write overlapping data. If that were untrue we would double-count and every
Expedition would be quietly wrong. It had never been exercised, because the test device has only
ever had one writer.

The probe now injects an overlapping `StepsRecord` itself and compares. Injecting **50,000 steps
across the exact span of a real 301-step record**: `aggregate()` returned **301, unchanged**, while
the raw sum returned **50,301**. Our record was discarded outright.

**Confirmed: `aggregate()` deduplicates by source priority. `readRecords()` does not.** Never sum
raw records.

A second finding, from injecting across a broader window: **deduplication is per timeline segment,
not per record.** A 50,000-step record spread over three hours kept ~32,650 — the portion covering
time when no other source had data. Where sources genuinely overlap the highest-priority one wins
that slice; where only one source has data, it counts. That is the desirable behaviour: two apps
tracking different parts of the day both contribute, and neither inflates the other.

Practical consequence: a user running a second step-tracking app cannot inflate their progress, and
cannot silently lose the hours their other app covered.

## Consequences

- Distance shown to the user is an estimate. Height is asked for during onboarding; it is the
  only personal measurement the app needs.
- Running stride is longer than walking stride and we do not distinguish them. Accepted.
- Error is roughly +/-10-20%. Over a 2,900 km Journey that is hundreds of kilometres, and users
  with a watch or health app may notice the discrepancy. This is the price of a signal that
  always exists.
- **Later refinement, not now:** if `DistanceRecord` ever does appear, a true stride can be
  derived from the distance-to-steps ratio and applied silently. Guard it by rejecting values
  outside ~0.5-1.0 m, so a cycling app writing distance cannot corrupt the calibration.
