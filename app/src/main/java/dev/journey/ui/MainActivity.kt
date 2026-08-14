package dev.journey.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.journey.content.HADRIANS_WALL
import dev.journey.data.ExpeditionState
import dev.journey.data.ExpeditionStore
import dev.journey.sync.Announcement
import dev.journey.sync.Arrivals
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
 * The app syncs every time it comes to the foreground.
 *
 * That is what makes a lazy background poll acceptable (ADR-0005): the worker exists only to catch
 * arrivals while the app is closed, so however far Android defers it, what the user sees when they
 * look is correct.
 *
 * It must be ON_RESUME rather than a one-shot effect. Walking with the app already open in the
 * background and then returning to it is the single most likely way a user checks their progress,
 * and it does not re-run onCreate.
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
            var diagnostics by remember { mutableStateOf<String?>(null) }
            // Set when the activity was launched from an arrival notification.
            var openUnread by remember {
                mutableStateOf(intent?.getBooleanExtra(Arrivals.EXTRA_OPEN_UNREAD, false) == true)
            }

            suspend fun refresh() {
                granted = runCatching {
                    HealthConnectClient.getOrCreate(context)
                        .permissionController
                        .getGrantedPermissions()
                        .containsAll(SyncEngine.REQUIRED_PERMISSIONS)
                }.getOrDefault(false)
                if (store.load() != null && granted) runCatching { engine.sync() }
                state = store.load()
                loaded = true
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                PermissionController.createRequestPermissionResultContract()
            ) { scope.launch { refresh() } }

            // Android 13+ needs this before an arrival notification can be shown at all.
            val notificationLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }

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
                            context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
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

            // Existing installs never saw the onboarding prompt, so ask here too.
            LaunchedEffect(loaded) {
                if (loaded && state != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val ok = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!ok) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // Sync on every resume, not once on create.
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) scope.launch { refresh() }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
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

            // Arriving from a notification opens the queue at the first unread thing, which is
            // ADR-0008's doorway: the notification names the furthest, the app reads them in order.
            LaunchedEffect(openUnread, current.readIds, current.metresCredited) {
                if (openUnread) {
                    trail.unread.firstOrNull()?.let { first ->
                        state = store.update { it.copy(readIds = it.readIds + first.id) }
                        screen = Screen.Reading(first)
                    }
                    openUnread = false
                }
            }

            when (val s = screen) {
                is Screen.Trail -> TrailScreen(
                    state = trail,
                    onOpen = { entry ->
                        scope.launch { state = store.update { it.copy(readIds = it.readIds + entry.id) } }
                        screen = Screen.Reading(entry)
                    },
                    onOpenAbout = { screen = Screen.About },
                    onOpenSettings = { screen = Screen.Settings },
                    problem = if (granted) null else
                        "Not accruing — Health Connect access is incomplete. Tap to fix.",
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
                    onClose = { screen = Screen.Trail; message = null; diagnostics = null },
                    message = message,
                    diagnostics = diagnostics,
                    onDiagnose = { scope.launch { diagnostics = engine.diagnose() } },
                    onFixPermissions = { permissionLauncher.launch(SyncEngine.REQUIRED_PERMISSIONS) },
                    onTestNotification = {
                        Arrivals.post(
                            context,
                            Announcement(
                                id = "test",
                                title = "Test arrival",
                                text = "If you can see this, notifications work. Real arrivals " +
                                    "only fire between 8am and 10pm.",
                            ),
                        )
                    },
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
