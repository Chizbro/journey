package dev.journey.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.journey.data.ExpeditionState
import dev.journey.domain.Journey
import dev.journey.ui.MainActivity
import java.time.LocalTime
import java.time.ZoneId

/**
 * The arrival notification: a doorway, not the content (ADR-0008).
 *
 * One notification naming the furthest thing reached, opening a sequential reveal of everything
 * unread. Not one buzz per Landmark — a poll deferred by hours routinely finds several crossings
 * at once, and four simultaneous notifications is spam that Android stacks into something
 * unreadable.
 */
object Arrivals {

    private const val CHANNEL = "arrivals"
    private const val NOTIFICATION_ID = 1
    const val EXTRA_OPEN_UNREAD = "open_unread"

    /** Outside these hours an arrival waits. A 3am crossing greets the user at breakfast. */
    private val WAKING_FROM: LocalTime = LocalTime.of(8, 0)
    private val WAKING_UNTIL: LocalTime = LocalTime.of(22, 0)

    fun isWakingHour(zone: ZoneId = ZoneId.systemDefault()): Boolean {
        val now = LocalTime.now(zone)
        return !now.isBefore(WAKING_FROM) && now.isBefore(WAKING_UNTIL)
    }

    /**
     * What to announce, if anything.
     *
     * Driven by what is *unread*, not by what this particular sync happened to cross. That is what
     * lets quiet hours work: an arrival skipped at 3am is still pending at 8am and gets announced
     * then, without needing to remember which sync found it.
     */
    fun pending(journey: Journey, state: ExpeditionState): Announcement? {
        val position = state.metresCredited
        val unread = journey.landmarks
            .filter { it.metresFromStart <= position && it.id !in state.readIds }
            .sortedBy { it.metresFromStart }

        val endingPending = journey.isComplete(position) && journey.ending.id !in state.readIds

        val furthestId = when {
            endingPending -> journey.ending.id
            unread.isNotEmpty() -> unread.last().id
            else -> return null
        }
        if (furthestId == state.lastNotifiedId) return null

        val title = if (endingPending) journey.ending.title else "You reached ${unread.last().name}"
        val others = unread.size - if (endingPending) 0 else 1
        val text = when {
            endingPending && unread.isNotEmpty() -> "The end of the line, with ${unread.size} still to read"
            endingPending -> journey.name
            others > 0 -> "${unread.last().standfirst} · and $others more along the way"
            else -> unread.last().standfirst
        }

        return Announcement(furthestId, title, text)
    }

    fun post(context: Context, announcement: Announcement) {
        createChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_UNREAD, true)
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(announcement.title)
            .setContentText(announcement.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(announcement.text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL,
            "Arrivals",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "When you reach somewhere on your journey" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

data class Announcement(val id: String, val title: String, val text: String)
