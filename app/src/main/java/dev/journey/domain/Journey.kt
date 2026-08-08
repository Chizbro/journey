package dev.journey.domain

/**
 * A famous real or fictional route that users can choose to travel, defined by its total
 * distance and the Landmarks along it.
 *
 * A Journey is a distance with ordered Landmarks, not a mapped route — see ADR-0002. It has
 * no coordinates and no polyline, which is what lets a fictional route and a real trail use
 * one model.
 */
data class Journey(
    val id: String,
    val name: String,
    val subtitle: String,
    val totalMetres: Long,
    val landmarks: List<Landmark>,
) {
    init {
        require(landmarks.isNotEmpty()) { "A Journey needs Landmarks" }
        require(landmarks.zipWithNext().all { (a, b) -> a.metresFromStart <= b.metresFromStart }) {
            "Landmarks must be ordered by distance from start"
        }
    }

    /** The next Landmark strictly ahead of [metres], or null once they are all behind you. */
    fun nextAfter(metres: Long): Landmark? = landmarks.firstOrNull { it.metresFromStart > metres }

    /** Everything reached at [metres], nearest first — the view looking back down the trail. */
    fun reachedAt(metres: Long): List<Landmark> =
        landmarks.filter { it.metresFromStart <= metres }.reversed()

    fun isComplete(metres: Long): Boolean = metres >= totalMetres
}

/**
 * A named point on a Journey, positioned at a fixed distance from its start.
 *
 * A Landmark is *reached* when an Expedition's distance passes it, and *read* once the user
 * has seen its entry. Those two states live on the Expedition, not here — this is content,
 * and it is identical for every user.
 */
data class Landmark(
    val id: String,
    val name: String,
    /** Where it sits on the Journey. Positions on real trails are interpolated; see the research. */
    val metresFromStart: Long,
    /** One line shown in the trail list, before you open it. */
    val standfirst: String,
    /** The entry itself. This is the payoff the whole reward loop exists to deliver. */
    val body: String,
    /** True where the place is a short detour from the route rather than on it. */
    val offRoute: Boolean = false,
)
