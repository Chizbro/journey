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
- **Notifications are suppressed outside sensible hours.** A 3am crossing joins the queue and
  greets the user at breakfast. This composes with batching rather than fighting it.
- There is almost always something waiting when the app is opened, which is quietly good for
  retention.
- The notification names the furthest Landmark reached — where the user actually is now. Earlier
  ones are not spoiled by this, since they were passed first and are read in order.
