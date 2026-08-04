# Expedition progress is our own record, and it is irreplaceable

We persist the *outcome* of reading Health Connect, not the readings. An Expedition holds the
distance credited to it so far, plus a watermark marking the instant we have credited up to.
Each sync reads the deduplicated aggregate from the watermark to now, adds it, and advances the
watermark. Individual readings are discarded once credited — they have no value after the fact.

This record cannot be reconstructed from Health Connect. Without the historical-data permission,
Health Connect only exposes the last 30 days, anchored at the moment permission was first
granted — not a sliding window we can reach back through. Combined with the decision to keep
everything on-device with no account and no server (no backend in v1), our datastore is the only
copy of the user's progress that exists anywhere.

Three things follow, and all three are requirements rather than nice-to-haves.

## Export and import are built before the dogfood run, not after

They are the only backup that will ever exist. A lost database is a permanently lost Expedition —
800 km toward Mount Doom, unrecoverable by any means, for the developer during testing and for
every user afterwards.

This is why export exists early in a pre-release app with no users, and it should not be mistaken
for a premature user-facing feature and cut. Restore is a first-class path, not a debug
affordance.

## A stale sync is silent data loss, so it must not be silent

If the app does not sync for more than 30 days, the distance accrued in that gap becomes
permanently unreadable. This is a realistic failure, not a theoretical one:

- Force-stopping an Android app halts its scheduled background work until the app is next
  launched manually.
- Aggressive OEM battery management (Samsung, Xiaomi and others) kills background work
  independently of anything we do.
- The user may simply revoke the permission and forget.

So the app must track how stale its watermark is and warn the user *before* the cliff — well
inside 30 days, not on day 29. If the window is ever exceeded, say so honestly: some distance was
lost and cannot be recovered.

## The release keystore is backed up off-machine

Losing it means the installed app can never be upgraded in place — only uninstalled and
reinstalled, which destroys the local datastore, which is the only copy of the progress.

## Considered and rejected

- **Storing every reading.** More data, no benefit. Once distance is credited, the reading that
  produced it answers no question we ever ask.
- **Android auto-backup instead of explicit export.** Restores unreliably across reinstalls, and
  it is cloud-backed, which quietly contradicts the on-device promise.
- **Accepting wipes during development and seeding synthetic progress.** The hypothesis under
  test is whether accumulated progress stays compelling over months. Synthetic data tells us the
  screen renders at 71%, not whether anyone cares at 71%.

## Open

Requesting `PERMISSION_READ_HEALTH_DATA_HISTORY` as insurance against the 30-day cliff was not
decided here. It would make a long gap recoverable, at the cost of a permission we currently
avoid entirely (see ADR-0003) and probably heavier Play review. Revisit if stale syncs turn out
to be common in practice.
