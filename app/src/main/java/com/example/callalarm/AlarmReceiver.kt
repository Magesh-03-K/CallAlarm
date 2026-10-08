package com.example.callalarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val id = i.getIntExtra("id", -1)
        val attempt = i.getIntExtra("attempt", 0)
        val e = Store.find(c, id) ?: return

        // Next day's alarm for repeating calls.
        if (attempt == 0 && e.daily) Scheduler.schedule(c, e)

        // If you ignore this alarm, ring again in 2 minutes (up to 3 times).
        if (attempt < Scheduler.MAX_REPEATS) {
            Scheduler.schedule(c, e, System.currentTimeMillis() + Scheduler.SNOOZE_MS, attempt + 1)
        }
        ring(c, e)
    }

    private fun ring(c: Context, e: CallEntry) {
        val nm = c.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CH, "Call alarms", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 600, 400, 600, 400)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)

        val open = Intent(c, CallAlarmActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra("id", e.id)
        val pi = PendingIntent.getActivity(
            c, e.id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val n = Notification.Builder(c, CH)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle("Time to call " + e.name)
            .setContentText(e.number)
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000)
            .build()
        n.flags = n.flags or Notification.FLAG_INSISTENT
        nm.notify(e.id, n)
    }

    companion object {
        const val CH = "call_alarms"
    }
}
