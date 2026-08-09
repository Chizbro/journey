package dev.journey.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.journey.content.HADRIANS_WALL

/**
 * Phase 2 of the plan, standing on placeholder state.
 *
 * Progress is a hardcoded constant until the sync engine lands. This renders the Trail and the
 * content so the shape can be judged; nothing here reads Health Connect yet.
 *
 * To review the ending, set PLACEHOLDER_METRES to 135_000.
 */
private const val PLACEHOLDER_METRES = 68_500L

private sealed interface Screen {
    data object Trail : Screen
    data object About : Screen
    data class Reading(val entry: Entry) : Screen
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var screen by remember { mutableStateOf<Screen>(Screen.Trail) }

            val state = TrailState(
                journey = HADRIANS_WALL,
                metresTravelled = PLACEHOLDER_METRES,
                readIds = HADRIANS_WALL.landmarks
                    .filter { it.metresFromStart < 64_000 }
                    .map { it.id }
                    .toSet(),
            )

            when (val current = screen) {
                is Screen.Trail -> TrailScreen(
                    state = state,
                    onOpen = { screen = Screen.Reading(it) },
                    onOpenAbout = { screen = Screen.About },
                )

                is Screen.About -> AboutScreen(
                    journey = state.journey,
                    onClose = { screen = Screen.Trail },
                )

                is Screen.Reading -> {
                    // Sequential reveal: walk the unread queue in order, then back to the trail.
                    val queue = state.unread
                    val index = queue.indexOfFirst { it.id == current.entry.id }
                    EntryScreen(
                        entry = current.entry,
                        hasNext = index >= 0 && index < queue.lastIndex,
                        onNext = {
                            queue.getOrNull(index + 1)?.let { screen = Screen.Reading(it) }
                        },
                        onClose = { screen = Screen.Trail },
                    )
                }
            }
        }
    }
}
