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

    /**
     * What to announce, if anything.
     *
     * Driven by what is *unread*, not by what this particular sync happened to cross. The worker
     * therefore never has to remember which sync found what: an arrival that could not be shown —
     * notifications off at the time — is simply still pending on the next poll.
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

    /**
     * Posts the notification, and reports whether it was actually shown.
     *
     * The return value is not decoration. The caller records the announced id so the worker does
     * not re-announce the same arrival every fifteen minutes — and recording that for a
     * notification the system dropped burns the arrival for good. It stays marked as announced
     * while never having been seen, so turning notifications on afterwards does not bring it
     * back; the user hears nothing until they cross something new.
     *
     * Nothing here throws when notifications are off. Android 13+ drops the post silently, so the
     * state has to be asked for rather than inferred from a failure.
     */
    fun post(context: Context, announcement: Announcement): Boolean {
        createChannel(context)
        if (!canPost(context)) return false

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

        return runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }.isSuccess
    }

    /**
     * Notifications can be off at the app level — the runtime permission denied, or the whole app
     * switched off in system settings — or left on there and silenced at the channel. Both look
     * identical from `notify()`, which is to say they look like success.
     */
    private fun canPost(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        val channel = context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(CHANNEL)
        return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
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
