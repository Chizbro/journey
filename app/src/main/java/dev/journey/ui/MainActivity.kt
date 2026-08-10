package dev.journey.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import dev.journey.content.HADRIANS_WALL
import dev.journey.data.ExpeditionState
import dev.journey.data.ExpeditionStore
import dev.journey.sync.SyncEngine
import dev.journey.sync.SyncWorker
import kotlinx.coroutines.launch
import java.time.Instant

private sealed interface Screen {
    data object Trail : Screen
    data object About : Screen
    data object Settings : Screen
    data class Reading(val entry: Entry) : Screen
}

/**
 * Opening the app always syncs before it renders.
 *
 * That is what makes a lazy background poll acceptable (ADR-0005): the background worker exists
 * only to catch arrivals while the app is closed, so however far Android defers it, what the user
 * sees when they look is correct.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val store = remember { ExpeditionStore(context) }
            val engine = remember { SyncEngine(context, store, HADRIANS_WALL) }

            var state by remember { mutableStateOf<ExpeditionState?>(null) }
            var loaded by remember { mutableStateOf(false) }
            var granted by remember { mutableStateOf(false) }
            var screen by remember { mutableStateOf<Screen>(Screen.Trail) }
            var message by remember { mutableStateOf<String?>(null) }

            val permissionLauncher = rememberLauncherForActivityResult(
                PermissionController.createRequestPermissionResultContract()
            ) { result -> granted = result.containsAll(SyncEngine.REQUIRED_PERMISSIONS) }

            val exportLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                scope.launch {
                    val text = store.export()
                    if (text == null) {
                        message = "Nothing to export yet."
                    } else {
                        runCatching {
                            context.contentResolver.openOutputStream(uri)?.use {
                                it.write(text.toByteArray())
                            }
                        }.onSuccess { message = "Exported. Keep it somewhere off this phone." }
                            .onFailure { message = "Export failed: ${it.message}" }
                    }
                }
            }

            val importLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                scope.launch {
                    val text = runCatching {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                    }.getOrNull()
                    if (text == null) {
                        message = "Could not read that file."
                    } else {
                        store.import(text)
                            .onSuccess { state = it; message = "Imported %.1f km.".format(it.metresCredited / 1000.0) }
                            .onFailure { message = "Not a valid export: ${it.message}" }
                    }
                }
            }

            // Load, check permissions, then sync — in that order, every time the app opens.
            LaunchedEffect(Unit) {
                granted = runCatching {
                    HealthConnectClient.getOrCreate(context)
                        .permissionController
                        .getGrantedPermissions()
                        .containsAll(SyncEngine.REQUIRED_PERMISSIONS)
                }.getOrDefault(false)
                state = store.load()
                if (state != null && granted) {
                    runCatching { engine.sync() }
                    state = store.load()
                }
                loaded = true
            }

            if (!loaded) return@setContent

            val current = state
            if (current == null) {
                OnboardingScreen(
                    journey = HADRIANS_WALL,
                    permissionsGranted = granted,
                    onRequestPermissions = { permissionLauncher.launch(SyncEngine.REQUIRED_PERMISSIONS) },
                    onBegin = { heightCm ->
                        scope.launch {
                            state = store.begin(HADRIANS_WALL.id, heightCm, Instant.now())
                            SyncWorker.schedule(context)
                        }
                    },
                )
                return@setContent
            }

            val trail = TrailState(
                journey = HADRIANS_WALL,
                metresTravelled = current.metresCredited,
                readIds = current.readIds,
            )

            when (val s = screen) {
                is Screen.Trail -> TrailScreen(
                    state = trail,
                    onOpen = { entry ->
                        // Opening an entry is what marks it read, so the unread queue drains as
                        // it is actually read rather than when the app happens to notice.
                        scope.launch { state = store.update { it.copy(readIds = it.readIds + entry.id) } }
                        screen = Screen.Reading(entry)
                    },
                    onOpenAbout = { screen = Screen.About },
                    onOpenSettings = { screen = Screen.Settings },
                )

                is Screen.About -> AboutScreen(
                    journey = HADRIANS_WALL,
                    onClose = { screen = Screen.Trail },
                )

                is Screen.Settings -> SettingsScreen(
                    state = current,
                    onSetHeight = { cm ->
                        scope.launch { state = store.update { it.copy(heightCm = cm) } }
                    },
                    onExport = { exportLauncher.launch("journey-${HADRIANS_WALL.id}.json") },
                    onImport = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    onClose = { screen = Screen.Trail; message = null },
                    message = message,
                )

                is Screen.Reading -> {
                    val queue = trail.unread
                    val index = queue.indexOfFirst { it.id == s.entry.id }
                    EntryScreen(
                        entry = s.entry,
                        hasNext = index >= 0 && index < queue.lastIndex,
                        onNext = {
                            queue.getOrNull(index + 1)?.let { nextEntry ->
                                scope.launch {
                                    state = store.update { it.copy(readIds = it.readIds + nextEntry.id) }
                                }
                                screen = Screen.Reading(nextEntry)
                            }
                        },
                        onClose = { screen = Screen.Trail },
                    )
                }
            }
        }
    }
}
