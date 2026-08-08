package dev.journey.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
 * everything reached — readable, with unread ones marked.
 *
 * Spacing between Landmarks is proportional to the real distance between them, so a long gap
 * looks long. The countdown is the thing that carries the ordinary day, so it has to be felt.
 */

/** Position on the Journey, in metres travelled, plus which Landmarks have been read. */
data class TrailState(
    val journey: Journey,
    val metresTravelled: Long,
    val readLandmarkIds: Set<String>,
) {
    val next: Landmark? get() = journey.nextAfter(metresTravelled)
    val metresToNext: Long? get() = next?.let { it.metresFromStart - metresTravelled }
    val reached: List<Landmark> get() = journey.reachedAt(metresTravelled)
    val unread: List<Landmark> get() = reached.filter { it.id !in readLandmarkIds }
    val isComplete: Boolean get() = journey.isComplete(metresTravelled)
}

private val Ahead = Color(0xFF6B6B6B)
private val Here = Color(0xFFE8B33C)
private val Behind = Color(0xFFE6E1D8)
private val Unread = Color(0xFFE8B33C)

@Composable
fun TrailScreen(state: TrailState, onOpenLandmark: (Landmark) -> Unit) {
    Surface(color = Color(0xFF14161A), modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 32.dp),
        ) {
            item { Header(state) }

            // The line runs the way you walk it: what is ahead is above, what is behind is below.
            val ahead = state.journey.landmarks.filter { it.metresFromStart > state.metresTravelled }
            itemsIndexed(ahead.reversed()) { index, landmark ->
                val gapBelow = gapTo(state, landmark, ahead.reversed(), index)
                LandmarkRow(
                    landmark = landmark,
                    reached = false,
                    unread = false,
                    isNext = landmark.id == state.next?.id,
                    metresToHere = landmark.metresFromStart - state.metresTravelled,
                    gapMetres = gapBelow,
                    onClick = null,
                )
            }

            item { YouAreHere(state) }

            itemsIndexed(state.reached) { _, landmark ->
                LandmarkRow(
                    landmark = landmark,
                    reached = true,
                    unread = landmark.id !in state.readLandmarkIds,
                    isNext = false,
                    metresToHere = null,
                    gapMetres = null,
                    onClick = { onOpenLandmark(landmark) },
                )
            }
        }
    }
}

@Composable
private fun Header(state: TrailState) {
    Column(Modifier.padding(bottom = 40.dp)) {
        Text(
            state.journey.name,
            color = Behind,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(state.journey.subtitle, color = Ahead, fontSize = 14.sp)
    }
}

/** The marker at your position, and the countdown that carries the uneventful days. */
@Composable
private fun YouAreHere(state: TrailState) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(14.dp).clip(CircleShape).background(Here))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    "%.1f km walked".format(state.metresTravelled / 1000.0),
                    color = Here,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                )
                val toNext = state.metresToNext
                Text(
                    when {
                        state.isComplete -> "You have reached the end."
                        toNext != null -> "%.1f km to %s".format(toNext / 1000.0, state.next?.name)
                        else -> "Nothing ahead but the last of it."
                    },
                    color = Ahead,
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
                        .background(if (unread) Unread else if (reached) Behind else Ahead)
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
                    unread -> Text("New — tap to read", color = Unread, fontSize = 13.sp)
                    reached -> Text(landmark.standfirst, color = Ahead, fontSize = 13.sp)
                    isNext && metresToHere != null ->
                        Text("%.1f km ahead".format(metresToHere / 1000.0), color = Ahead, fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * The connecting line. Its height is proportional to real distance, so the 16 km of nothing
 * between Heddon and the Portgate looks like 16 km of nothing.
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

private fun gapTo(
    state: TrailState,
    landmark: Landmark,
    ordered: List<Landmark>,
    index: Int,
): Long {
    val below = ordered.getOrNull(index + 1)
    return if (below != null) landmark.metresFromStart - below.metresFromStart
    else landmark.metresFromStart - state.metresTravelled
}
