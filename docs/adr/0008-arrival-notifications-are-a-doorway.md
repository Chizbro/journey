# Arrival notifications are a doorway, not the content

When a poll finds that several Landmarks have been reached since the last one, we send **one**
notification naming the furthest Landmark reached. Tapping it opens a sequential reveal of every
unread Landmark, in order, each with its own entry.

We do not send one notification per Landmark, and we do not summarise several Landmarks into a
single line of notification text.

## Why this comes up at all

Multiple crossings per poll is the normal case, not an edge case. Android's App Standby buckets
defer our background poll by up to ~2 hours, ~8 hours, or a day depending on how recently the app
was opened (see ADR-0005). A 20 km Saturday walk on a Journey with Landmarks every 5 km is four
crossings, and they can easily all land inside one gap.

## Why a doorway

The Landmark's writing is the product — it is the payoff the whole reward loop exists to deliver
(ADR-0002). Four simultaneous notifications is spam, and Android stacks them into something
unreadable. One summary line — "You passed Vindolanda, Housesteads and Sycamore Gap" — is tidy
and throws away the three pieces of prose we care most about.

Treating the notification as a doorway keeps every Landmark's moment intact while costing one
buzz.

It also inverts the cost of our worst scheduling behaviour. **A long delay stops being loss and
becomes accumulation**: an eight-hour gap does not rob the user of three arrivals, it means three
arrivals are stacked up waiting when they next open the app. The app's least controllable
weakness reads as generosity.

## Consequences

- A Landmark needs *reached* and *read* as distinct states, and the app needs the ordered queue of
  reached-but-unread Landmarks. This is now in `CONTEXT.md`.
- **Arrivals are announced whenever the poll finds them, at any hour.** There is no night hold —
  see below.
- **An arrival is only recorded as announced once the notification is actually shown.** Android
  drops a post silently when notifications are off rather than failing, so recording it regardless
  marks the arrival as announced while the user has seen nothing, and granting the permission
  afterwards brings it back. It stays pending instead.
- There is almost always something waiting when the app is opened, which is quietly good for
  retention.
- The notification names the furthest Landmark reached — where the user actually is now. Earlier
  ones are not spoiled by this, since they were passed first and are read in order.

## Removed: the night hold

This ADR originally suppressed notifications outside 08:00–22:00, on the reasoning that a poll
deferred by App Standby can surface a 9pm crossing at 3am, in bed, long after the moment has
passed. That was then refined to judge the *freshness of the step data* rather than the clock,
since crossing a Landmark at 3am means the user is out walking at 3am and saying so is the point
of the app.

Both versions are gone. The rule was subtle enough to be wrong twice: the clock version buzzed
about stale arrivals and stayed silent about live ones, and the freshness version read the sync
watermark as a proxy for "is the user walking", which it is not — the watermark advances on
wall-clock when there are no records to anchor to, so a sleeping phone looks permanently 30
minutes fresh and every held arrival escaped on the very next poll. Fixing that meant carrying
the newest visible record end out of the sync purely to feed the hold.

That is a lot of machinery, and a lot of ways to be wrong, guarding against one buzz at a bad
hour on a phone that is in Do Not Disturb anyway. The batching in this ADR already does the real
work: at most one notification per poll, no matter how many Landmarks were crossed. Android's own
per-app notification schedule handles the rest, and handles it as a user preference rather than
as our guess about their sleep.

Revisit only if the untimed version actually annoys someone in practice.
