package dev.journey.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.journey.domain.Journey
import dev.journey.domain.Landmark

/**
 * The Trail: one line, with you somewhere on it.
 *
 * Everything the app shows is a view onto this line. Ahead of you, dimmed and unreachable, with
 * the next one carrying the countdown. At your position, how far you have come. Behind you,
 * everything reached — readable, unread ones marked.
 *
 * Spacing between Landmarks is proportional to real distance, so a long gap looks long. The
 * countdown carries the ordinary day, so it has to be felt rather than read.
 */

data class TrailState(
    val journey: Journey,
    val metresTravelled: Long,
    val readIds: Set<String>,
) {
    /** Progress stops at the end. Walking past the terminus is not a thing that happens. */
    val position: Long get() = metresTravelled.coerceAtMost(journey.totalMetres)

    val next: Landmark? get() = journey.nextAfter(position)
    val metresToNext: Long? get() = next?.let { it.metresFromStart - position }
    val reached: List<Landmark> get() = journey.reachedAt(position)
    val isComplete: Boolean get() = journey.isComplete(metresTravelled)
    val endingUnread: Boolean get() = isComplete && journey.ending.id !in readIds

    /** Everything waiting to be read, in the order it was reached. Drives the sequential reveal. */
    val unread: List<Entry>
        get() = buildList {
            addAll(reached.reversed().filter { it.id !in readIds }.map { it.toEntry() })
            if (endingUnread) add(journey.ending.toEntry())
        }
}

@Composable
fun TrailScreen(
    state: TrailState,
    onOpen: (Entry) -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSettings: () -> Unit,
    /** Shown above everything when the app cannot actually accrue. Never fail silently here. */
    problem: String? = null,
) {
    Surface(color = Ink, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            contentPadding = PaddingValues(vertical = 32.dp),
        ) {
            problem?.let {
                item {
                    Text(
                        it,
                        color = Here,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenSettings)
                            .padding(bottom = 24.dp),
                    )
                }
            }

            item { Header(state, onOpenAbout) }

            if (state.isComplete) {
                item { Terminus(state, onOpen) }
            } else {
                // The line runs the way you walk it: ahead is above, behind is below.
                val ahead = state.journey.landmarks.filter { it.metresFromStart > state.position }
                    .reversed()
                itemsIndexed(ahead) { index, landmark ->
                    LandmarkRow(
                        landmark = landmark,
                        reached = false,
                        unread = false,
                        isNext = landmark.id == state.next?.id,
                        metresToHere = landmark.metresFromStart - state.position,
                        gapMetres = gapBelow(state, ahead, index),
                        onClick = null,
                    )
                }
                item { YouAreHere(state) }
            }

            itemsIndexed(state.reached) { _, landmark ->
                LandmarkRow(
                    landmark = landmark,
                    reached = true,
                    unread = landmark.id !in state.readIds,
                    isNext = false,
                    metresToHere = null,
                    gapMetres = null,
                    onClick = { onOpen(landmark.toEntry()) },
                )
            }

            // Below the start of the line, where nothing else competes for attention.
            item {
                Spacer(Modifier.height(48.dp))
                Text(
                    "Settings and backup",
                    color = Ahead,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenSettings)
                        .padding(vertical = 12.dp),
                )
            }
        }
    }
}

/** Tapping the header opens the overview — a view of the whole line rather than a point on it. */
@Composable
private fun Header(state: TrailState, onOpenAbout: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenAbout)
            .padding(bottom = 40.dp)
    ) {
        Text(state.journey.name, color = Behind, fontSize = 28.sp, fontWeight = FontWeight.Medium)
        Text(state.journey.subtitle, color = Ahead, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Text("by ${state.journey.author.name} · about this route", color = Ahead, fontSize = 12.sp)
    }
}

/** Your position, and the countdown that carries the uneventful days. */
@Composable
private fun YouAreHere(state: TrailState) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(Here))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                "%.1f km walked".format(state.position / 1000.0),
                color = Here,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
            )
            val toNext = state.metresToNext
            Text(
                if (toNext != null) "%.1f km to %s".format(toNext / 1000.0, state.next?.name)
                else "Nothing ahead but the last of it.",
                color = Ahead,
                fontSize = 14.sp,
            )
        }
    }
}

/**
 * The end of the line. No celebration — the ending is authored per Journey and is simply the
 * last thing there is to read.
 */
@Composable
private fun Terminus(state: TrailState, onOpen: (Entry) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { onOpen(state.journey.ending.toEntry()) }
            .padding(vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(18.dp).height(3.dp).background(Here))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    state.journey.ending.title,
                    color = Behind,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    if (state.endingUnread) "New — tap to read"
                    else "%.0f km walked".format(state.position / 1000.0),
                    color = if (state.endingUnread) Here else Ahead,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun LandmarkRow(
    landmark: Landmark,
    reached: Boolean,
    unread: Boolean,
    isNext: Boolean,
    metresToHere: Long?,
    gapMetres: Long?,
    onClick: (() -> Unit)?,
) {
    Column {
        if (gapMetres != null) TrailSegment(gapMetres)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(if (unread) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (unread) Here else if (reached) Behind else Ahead)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        landmark.name,
                        color = if (reached) Behind else Ahead,
                        fontSize = if (isNext) 18.sp else 16.sp,
                        fontWeight = if (unread || isNext) FontWeight.Medium else FontWeight.Normal,
                    )
                    if (landmark.offRoute) {
                        Spacer(Modifier.width(8.dp))
                        Text("detour", color = Ahead, fontSize = 11.sp)
                    }
                }
                when {
                    unread -> Text("New — tap to read", color = Here, fontSize = 13.sp)
                    reached -> Text(landmark.standfirst, color = Ahead, fontSize = 13.sp)
                    isNext && metresToHere != null ->
                        Text("%.1f km ahead".format(metresToHere / 1000.0), color = Ahead, fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * The connecting line, its height proportional to real distance — so the 16 km of nothing between
 * Heddon and the Portgate looks like 16 km of nothing.
 */
@Composable
private fun TrailSegment(gapMetres: Long) {
    val height = (gapMetres / 1000.0 * 3.0).dp.coerceIn(12.dp, 140.dp)
    Row {
        Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.width(2.dp).height(height).background(Ahead.copy(alpha = 0.35f)))
        }
    }
}

private fun gapBelow(state: TrailState, ordered: List<Landmark>, index: Int): Long {
    val below = ordered.getOrNull(index + 1)
    return if (below != null) ordered[index].metresFromStart - below.metresFromStart
    else ordered[index].metresFromStart - state.position
}
