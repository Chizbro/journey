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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.journey.domain.Journey

/**
 * The Journey overview: what this route is, why it is in the catalogue, and who wrote it.
 *
 * Reached from the Trail header, because it describes the whole line rather than a point on it.
 * The byline lives here: voice varies between Journeys by design (ADR pending), and that only
 * reads as personality rather than carelessness if the writer is named.
 */
@Composable
fun AboutScreen(journey: Journey, onClose: () -> Unit) {
    Surface(color = Ink, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 48.dp)
        ) {
            Text(journey.name, color = Behind, fontSize = 32.sp, fontWeight = FontWeight.Medium)
            Text(journey.subtitle, color = Ahead, fontSize = 14.sp)

            Spacer(Modifier.height(32.dp))
            Text(journey.about.background, color = Body, fontSize = 17.sp, lineHeight = 28.sp)

            Spacer(Modifier.height(32.dp))
            Text("Why this one", color = Ahead, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            Text(journey.about.whyThisOne, color = Body, fontSize = 17.sp, lineHeight = 28.sp)

            Spacer(Modifier.height(32.dp))
            Text("Written by", color = Ahead, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            Text(
                journey.author.name,
                color = Behind,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(6.dp))
            Text(journey.author.bio, color = Body, fontSize = 15.sp, lineHeight = 24.sp)

            Spacer(Modifier.height(40.dp))
            Sources(journey.about.sources)

            Spacer(Modifier.height(40.dp))
            Text(
                "Back to the trail",
                color = Here,
                fontSize = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClose)
                    .padding(vertical = 12.dp),
            )
        }
    }
}
