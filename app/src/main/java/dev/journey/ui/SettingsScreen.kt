package dev.journey.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.journey.data.ExpeditionState
import dev.journey.data.SyncWarning
import java.time.Duration
import java.time.Instant

/**
 * Height, and the backup.
 *
 * Export is not a convenience feature. With no account and no server, and with Health Connect
 * only serving 30 days from first permission grant, this file is the only copy of the user's
 * progress that will ever exist (ADR-0006). Restore is a first-class path, not a debug affordance.
 */
@Composable
fun SettingsScreen(
    state: ExpeditionState,
    onSetHeight: (Int) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClose: () -> Unit,
    message: String?,
    diagnostics: String?,
    onDiagnose: () -> Unit,
    onFixPermissions: () -> Unit,
    onTestNotification: () -> Unit,
) {
    var height by remember { mutableStateOf(state.heightCm.toString()) }
    val cm = height.toIntOrNull()
    val heightOk = cm != null && cm in 100..250

    Surface(color = Ink, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 48.dp)
        ) {
            Text("Settings", color = Behind, fontSize = 28.sp, fontWeight = FontWeight.Medium)

            Spacer(Modifier.height(32.dp))
            Text("Height", color = Ahead, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = height,
                onValueChange = { height = it.filter(Char::isDigit).take(3) },
                label = { Text("cm") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Stride %.2f m per step".format(state.metresPerStep),
                color = Ahead,
                fontSize = 13.sp,
            )
            if (heightOk && cm != state.heightCm) {
                Text(
                    "Save height",
                    color = Here,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSetHeight(cm!!) }
                        .padding(vertical = 12.dp),
                )
            }

            Spacer(Modifier.height(40.dp))
            Text("Backup", color = Ahead, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Your progress lives only on this phone, and it cannot be rebuilt from Health " +
                    "Connect. If you lose the app, an export is the only way back.",
                color = Body,
                fontSize = 15.sp,
                lineHeight = 24.sp,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Export",
                color = Here,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onExport).padding(vertical = 12.dp),
            )
            Text(
                "Import (replaces everything)",
                color = Here,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onImport).padding(vertical = 12.dp),
            )
            message?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Body, fontSize = 14.sp)
            }

            Spacer(Modifier.height(40.dp))
            Text("Sync", color = Ahead, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            SyncStatus(state)

            Spacer(Modifier.height(12.dp))
            Text(
                "Why isn't it updating?",
                color = Here,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onDiagnose).padding(vertical = 12.dp),
            )
            Text(
                "Send a test arrival",
                color = Here,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onTestNotification).padding(vertical = 12.dp),
            )
            Text(
                "Re-request Health Connect permissions",
                color = Here,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onFixPermissions).padding(vertical = 12.dp),
            )
            diagnostics?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = Body, fontSize = 13.sp, lineHeight = 20.sp)
            }

            Spacer(Modifier.height(40.dp))
            Text(
                "Back to the trail",
                color = Here,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClose).padding(vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun SyncStatus(state: ExpeditionState) {
    val now = Instant.now()
    val ago = Duration.between(state.syncedThrough, now)
    Text(
        "Last credited " + when {
            ago.toHours() < 1 -> "${ago.toMinutes()} minutes ago"
            ago.toDays() < 1 -> "${ago.toHours()} hours ago"
            else -> "${ago.toDays()} days ago"
        },
        color = Body,
        fontSize = 15.sp,
    )
    when (val warning = state.syncWarning(now)) {
        is SyncWarning.GoingStale -> {
            Spacer(Modifier.height(8.dp))
            Text(
                "Not synced for ${warning.daysSinceSync} days. Health Connect only keeps 30 days " +
                    "available to us — after that the missing distance cannot be recovered. " +
                    "${warning.daysRemaining} days left.",
                color = Here,
                fontSize = 14.sp,
                lineHeight = 22.sp,
            )
        }
        is SyncWarning.DataLost -> {
            Spacer(Modifier.height(8.dp))
            Text(
                "Not synced for ${warning.daysSinceSync} days. Some distance is beyond Health " +
                    "Connect's 30-day window and cannot be recovered. Sync from here on will work " +
                    "normally.",
                color = Here,
                fontSize = 14.sp,
                lineHeight = 22.sp,
            )
        }
        null -> Unit
    }
}
