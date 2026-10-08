package com.example.callalarm

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class CallAlarmActivity : Activity() {
    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val e = Store.find(this, intent.getIntExtra("id", -1))
        if (e == null) { finish(); return }

        // The ringing now happens here, so stop the notification's own sound.
        getSystemService(NotificationManager::class.java).cancel(e.id)

        // Direct mode: place the call immediately without showing the Call / Cancel screen.
        if (e.direct &&
            checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        ) {
            placeCall(e)
            return
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0E1612"))
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }
        root.addView(TextView(this).apply {
            text = "Time to call"
            setTextColor(Color.parseColor("#93A79C")); textSize = 18f; gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = e.name
            setTextColor(Color.WHITE); textSize = 40f; gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(4))
        })
        root.addView(TextView(this).apply {
            text = e.number
            setTextColor(Color.parseColor("#93A79C")); textSize = 20f; gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(48))
        })

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val call = Button(this).apply {
            text = "Call"; textSize = 20f
            setTextColor(Color.parseColor("#04210F"))
            setBackgroundColor(Color.parseColor("#2FD374"))
            setOnClickListener { placeCall(e) }
        }
        val cancel = Button(this).apply {
            text = "Cancel"; textSize = 20f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#C8432F"))
            setOnClickListener { respond(e); finish() }
        }
        val lp = { LinearLayout.LayoutParams(0, dp(72), 1f).apply { setMargins(dp(8), 0, dp(8), 0) } }
        row.addView(call, lp()); row.addView(cancel, lp())
        root.addView(row)
        setContentView(root)

        startRing()
        // If you do nothing, stop after 1 minute. The repeat alarm will ring again in 2 minutes.
        handler.postDelayed({ stopRing(); finish() }, 60_000)
    }

    private fun placeCall(e: CallEntry) {
        respond(e)
        val uri = Uri.parse("tel:" + Uri.encode(e.number))
        val granted = checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        startActivity(
            Intent(if (granted) Intent.ACTION_CALL else Intent.ACTION_DIAL, uri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    /** You answered: stop the sound and the repeat alarms. */
    private fun respond(e: CallEntry) {
        stopRing()
        Scheduler.cancelSnooze(this, e.id)
        getSystemService(NotificationManager::class.java).cancel(e.id)
        if (!e.daily) Store.remove(this, e.id)
    }

    private fun startRing() {
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@CallAlarmActivity, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {}
        try {
            getSystemService(Vibrator::class.java)
                .vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 400), 0))
        } catch (_: Exception) {}
    }

    private fun stopRing() {
        handler.removeCallbacksAndMessages(null)
        try { player?.stop(); player?.release() } catch (_: Exception) {}
        player = null
        try { getSystemService(Vibrator::class.java).cancel() } catch (_: Exception) {}
    }

    override fun onDestroy() {
        stopRing()
        super.onDestroy()
    }
}
