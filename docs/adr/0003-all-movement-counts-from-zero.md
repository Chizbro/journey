# All movement counts, and every Expedition starts at zero

Every kilometre the user walks counts towards their active Expedition — not just deliberately
recorded workouts. The commute, the supermarket, pacing on a call. But an Expedition begins at
zero on the day the user starts it, and never absorbs step history that predates it.

These two decisions are recorded together because each one exists to make the other survivable.

Counting passive movement is the product: the intended feeling is arriving somewhere you did not
notice walking to, and it makes the app work for people trying to raise their step count rather
than only for people who log workouts. Starting from zero is what stops that generosity from
becoming hollow — the phone already holds years of step history, so absorbing it would complete
several Journeys before the user had done anything at all.

## Consequences

- Journey lengths must be calibrated against passive accrual of roughly 4.4 km/day. A 135 km
  Journey is about a month; 2,900 km is nearly two years. The catalogue must span that range, and
  short Journeys are what make a new user's first screen bearable.
- We never need Health Connect's permission for reading data older than 30 days, because we only
  ever read forward from the day an Expedition starts.
- Distance walked while no Expedition is active is lost. This is accepted.
- The reward loop cannot rely on Landmark frequency alone — at this pace a long Journey would go
  weeks between them. The "distance to next Landmark" countdown carries the daily motivation.

## Update (2026-08-05)

Starting at zero turns out to be **forced by the platform, not merely chosen**. Health Connect's
native step recording only runs while at least one app holds `READ_STEPS`, so it begins writing
at the moment permission is granted. A probe run found 30 days of nothing followed by data
starting at the grant.

There is no history to absorb even if we wanted it. The decision above was the only available
behaviour, and it costs us nothing we could otherwise have had.
