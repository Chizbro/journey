# A Journey is a distance with Landmarks, not a mapped route

A Journey is modelled as a total distance plus an ordered list of Landmarks, each at a fixed
offset from the start. It is not a geographic polyline. Fictional routes have no coordinates —
Middle-earth has no latitude, and every good map of it is someone's copyrighted artwork — so a
geographic model would work for exactly the half of the catalogue that matters least.

## Consequences

- There is no map view, and its absence is deliberate. The primary display is progress plus a
  live "distance to next Landmark" countdown.
- Real and fictional Journeys use one content pipeline and one renderer.
- A map can be added later to individual Journeys that have real coordinates, as decoration,
  without changing the model.
- Landmark prose is the app's most important content asset, since it carries the entire payoff
  moment. Real trails supply Landmarks cheaply (villages, crossings, huts every 10–25 km);
  fictional routes are sparse and rely on the countdown to fill the gaps — see ADR-0003 for the
  pacing this has to work against.
