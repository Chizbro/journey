package dev.journey.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.journey.content.HADRIANS_WALL
import dev.journey.domain.Landmark

/**
 * Phase 2 of the plan, standing on placeholder state.
 *
 * Progress is a hardcoded constant until the sync engine lands. This renders the Trail and the
 * content so the shape can be judged; nothing here reads Health Connect yet.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var open by remember { mutableStateOf<Landmark?>(null) }

            // PLACEHOLDER — replaced by the Expedition's real progress in Phase 1.
            // Sitting just past Sycamore Gap, with the last two Landmarks unread.
            val state = TrailState(
                journey = HADRIANS_WALL,
                metresTravelled = 68_500,
                readLandmarkIds = HADRIANS_WALL.landmarks
                    .filter { it.metresFromStart < 64_000 }
                    .map { it.id }
                    .toSet(),
            )

            when (val landmark = open) {
                null -> TrailScreen(state) { open = it }
                else -> LandmarkScreen(
                    landmark = landmark,
                    hasNext = false,
                    onNext = {},
                    onClose = { open = null },
                )
            }
        }
    }
}
