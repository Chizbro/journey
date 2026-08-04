# Journey

A personal fitness app. Users choose a famous real or fictional route and travel it
over time, with every kilometre they walk or run in the real world moving them along it.

## Language

**Journey**:
A famous real or fictional route that users can choose to travel, defined by its total
distance and the Landmarks along it.
_Avoid_: Route, Track, Trail, Challenge

**Landmark**:
A named point on a Journey, positioned at a fixed distance from its start, that a user
passes and is told about.
_Avoid_: Milestone, Waypoint, Checkpoint

**Expedition**:
One user's traversal of a single Journey, holding the distance they have accumulated
towards it. A user has at most one active Expedition; the rest are dormant or completed.
An Expedition is never paused — dormancy is what switching Journeys produces.
_Avoid_: Attempt, Trek, Journey, Paused

**Milestone**:
Reserved, unused. Kept free for personal achievements ("your first 100 km") so it is
never spent on Landmarks.
