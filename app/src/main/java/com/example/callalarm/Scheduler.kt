package com.example.callalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object Scheduler {
    const val SNOOZE_MS = 2 * 60 * 1000L
    const val MAX_REPEATS = 3

    fun nextTrigger(e: CallEntry): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, e.hour)
            set(Calendar.MINUTE, e.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.timeInMillis
    }

    private fun pending(c: Context, id: Int, attempt: Int): PendingIntent {
        val i = Intent(c, AlarmReceiver::class.java)
            .putExtra("id", id)
            .putExtra("attempt", attempt)
        val code = if (attempt == 0) id else id + 1_000_000
        return PendingIntent.getBroadcast(
            c, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** setAlarmClock fires exactly, even in Doze, and needs no special permission. */
    fun schedule(c: Context, e: CallEntry, at: Long = nextTrigger(e), attempt: Int = 0) {
        val am = c.getSystemService(AlarmManager::class.java)
        val show = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), pending(c, e.id, attempt))
    }

    fun cancelSnooze(c: Context, id: Int) {
        c.getSystemService(AlarmManager::class.java).cancel(pending(c, id, 1))
    }

    fun cancelAll(c: Context, id: Int) {
        val am = c.getSystemService(AlarmManager::class.java)
        am.cancel(pending(c, id, 0))
        am.cancel(pending(c, id, 1))
    }

    fun rescheduleAll(c: Context) {
        Store.all(c).forEach { schedule(c, it) }
    }
}
