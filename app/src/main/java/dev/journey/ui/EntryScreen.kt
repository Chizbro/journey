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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.journey.domain.Ending
import dev.journey.domain.Landmark
import dev.journey.domain.Source

/**
 * Something to read: a Landmark you have passed, or the end of the Journey.
 *
 * One shape for both, because the ending is not a different kind of event — it is the last thing
 * on the line.
 */
data class Entry(
    val id: String,
    val title: String,
    /** Distance marker, or null for the ending — the ending is not at a distance, it is the end. */
    val marker: String?,
    val note: String?,
    val body: String,
    val sources: List<Source>,
)

fun Landmark.toEntry() = Entry(
    id = id,
    title = name,
    marker = "%.0f km".format(metresFromStart / 1000.0),
    note = if (offRoute) "A short detour from the path" else null,
    body = body,
    sources = sources,
)

fun Ending.toEntry() = Entry(
    id = id,
    title = title,
    marker = null,
    note = null,
    body = body,
    sources = sources,
)

/**
 * The reading view. Also ADR-0008's sequential reveal: opened at the first unread entry, with
 * [hasNext] walking through the rest in order. One screen, two entry points — a tap on the Trail,
 * or an arrival notification.
 */
@Composable
fun EntryScreen(
    entry: Entry,
    hasNext: Boolean,
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(color = Ink, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 48.dp)
        ) {
            entry.marker?.let {
                Text(it, color = Here, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
            }
            Text(entry.title, color = Behind, fontSize = 32.sp, fontWeight = FontWeight.Medium)
            entry.note?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = Ahead, fontSize = 13.sp)
            }
            Spacer(Modifier.height(28.dp))
            Text(entry.body, color = Body, fontSize = 17.sp, lineHeight = 28.sp)

            Spacer(Modifier.height(40.dp))
            Sources(entry.sources)

            Spacer(Modifier.height(40.dp))
            Text(
                if (hasNext) "Next →" else "Back to the trail",
                color = Here,
                fontSize = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (hasNext) onNext() else onClose() }
                    .padding(vertical = 12.dp),
            )
        }
    }
}

/**
 * Citation and invitation in one block. Every Landmark has these — a writer may choose their own
 * tone but not decline to say where the facts came from.
 */
@Composable
fun Sources(sources: List<Source>) {
    val uris = LocalUriHandler.current
    Column {
        Text("Where this comes from", color = Ahead, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        sources.forEach { source ->
            Text(
                source.title,
                color = Link,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uris.openUri(source.url) }
                    .padding(vertical = 6.dp),
            )
        }
    }
}

internal val Ink = Color(0xFF14161A)
internal val Ahead = Color(0xFF6B6B6B)
internal val Here = Color(0xFFE8B33C)
internal val Behind = Color(0xFFE6E1D8)
internal val Body = Color(0xFFCFC9BF)
internal val Link = Color(0xFF9FB8C8)
