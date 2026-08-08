package dev.journey.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.journey.domain.Landmark

/**
 * A single Landmark's entry. The payoff the whole reward loop exists to deliver.
 *
 * This is also the sequential reveal from ADR-0008: opened at the first unread Landmark, with
 * [hasNext] walking the user through the rest in order. One screen, two entry points — tapping a
 * Landmark on the Trail, or following an arrival notification.
 */
@Composable
fun LandmarkScreen(
    landmark: Landmark,
    hasNext: Boolean,
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(color = Color(0xFF14161A), modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 48.dp)
        ) {
            Text(
                "%.0f km".format(landmark.metresFromStart / 1000.0),
                color = Color(0xFFE8B33C),
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                landmark.name,
                color = Color(0xFFE6E1D8),
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
            )
            if (landmark.offRoute) {
                Spacer(Modifier.height(4.dp))
                Text("A short detour from the path", color = Color(0xFF6B6B6B), fontSize = 13.sp)
            }
            Spacer(Modifier.height(28.dp))
            Text(
                landmark.body,
                color = Color(0xFFCFC9BF),
                fontSize = 17.sp,
                lineHeight = 28.sp,
            )
            Spacer(Modifier.height(48.dp))
            Text(
                if (hasNext) "Next →" else "Back to the trail",
                color = Color(0xFFE8B33C),
                fontSize = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (hasNext) onNext() else onClose() }
                    .padding(vertical = 12.dp),
            )
        }
    }
}
