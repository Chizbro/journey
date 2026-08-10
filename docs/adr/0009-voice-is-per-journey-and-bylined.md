# Voice is authored per Journey, and every Journey carries a byline

Each Journey's entries are written in whatever voice their author chooses. There is no house
style. In exchange, `Author` is a required field on `Journey`, shown on the Trail header and in
full on the overview, and `Source` is a required field on every `Landmark` — the constructor
rejects a Landmark with none.

## Why not a single house style

A house style is the obvious choice: the voice is the product, it is delivered at every payoff
moment, and one voice means a reader who liked one Journey knows what they are getting from the
next. That was the original recommendation.

The argument against it is better. If several people write Journeys, readers will come to prefer
particular writers, and following a writer is a reason to open the app that a route alone does not
provide. A house style flattens exactly the thing that would make the catalogue feel personal.

## The catch, and what it forces

Varying voice only reads as personality if the reader knows whose voice it is. Unattributed,
the same variation reads as carelessness — as though nobody was minding the tone.

So attribution is not decoration here, it is what makes the decision work, and that is why
`Author` is required rather than optional. Structure stays fixed across every Journey — title,
standfirst, body, sources — and only voice varies. Consistent experience, customisable delivery.

## Consequences

- **Sources are mandatory and enforced in the type.** A writer may pick their own tone. They may
  not decline to say where the facts came from.
- The catalogue's quality now depends on editorial judgement about writers, not on a style guide.
  There is no automated way to keep tone in bounds, and that is accepted.
- Hadrian's Wall is attributed to **Claude**, which drafted it from cited primary sources. Putting
  a human name on it would have been false, and leaving it unattributed would have undercut the
  decision above. Its byline states the interpolated distances carry ±1–2 km, on the grounds that
  if readers are to trust a writer they should know where that writer's soft edges are.
- Copy currently lives in Kotlin, which is compile-checked and typo-proof but closed to
  non-developers. Move it to data files when there is a second writer, not before.
