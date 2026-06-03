package eu.walkerjones.ibridge

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val n = sbn.notification ?: return
        val extras = n.extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (sbn.packageName == packageName) return
        if (n.flags and Notification.FLAG_ONGOING_EVENT != 0) return
        if (title.isEmpty() && text.isEmpty()) return

        val appName = try {
            val pm = packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
        } catch (e: Exception) {
            sbn.packageName
        }

        Store.recordSeen(this, sbn.packageName, appName)
        if (!Store.shouldSend(this, sbn.packageName)) return

        @Suppress("DEPRECATION")
        val priority = when {
            n.priority >= Notification.PRIORITY_HIGH -> 4
            n.priority <= Notification.PRIORITY_LOW -> 2
            else -> 3
        }

        Ntfy.publish(this, title, text, priority)
    }
}
