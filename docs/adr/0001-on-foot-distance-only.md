# Progress is measured in on-foot distance only

An Expedition advances on one number: the distance the user walked or ran, in kilometres.
Elevation is not tracked and cycling does not count. Distance is the only unit that works
identically for a real trail and a fictional route, and it is the unit that makes the app's
promise literal — you covered the ground they covered.

## Considered Options

- **Distance and elevation** — would have kept summit journeys like Everest, but needs a second
  progress model, a second set of screens, and elevation data that trackers report poorly.
- **Distance including cycling** — a cycled kilometre costs a fraction of a walked one, so
  cycling either trivialises long Journeys or needs a multiplier. Any multiplier is a magic
  number we would have to defend forever, and "you cycled 30 km and moved 10 km" breaks the
  one promise the app makes.

## Consequences

- No vertical journeys. Everest is out; Everest Base Camp trek (~130 km) would fit natively.
- The catalogue is on-foot routes only — no Route 66, no Tour de France.
- Data integration collapses to a single OS-provided value, already deduplicated by the
  platform. There is no workout stream to reconcile against step data.

## Update (2026-08-05)

That single value turned out to be **steps, not distance** — see ADR-0007. This strengthens
rather than weakens the decision above.

`DistanceRecord` carries no modality tag, so cycling and swimming apps write to it with no
documented way to filter them out. Had we read distance, "cycling does not count" would have
been unenforceable — bike rides would have silently credited Expeditions. Steps cannot come
from a bike or a pool, so reading steps enforces this ADR by construction.

The cost is that distance is now an estimate derived from stride length rather than a
measurement. ADR-0007 covers the trade.
