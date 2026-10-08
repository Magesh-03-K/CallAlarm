package com.example.callalarm

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar

class MainActivity : Activity() {
    private var hour = 9
    private var minute = 0
    private var timeChosen = false

    private lateinit var nameEt: EditText
    private lateinit var numEt: EditText
    private lateinit var timeBtn: Button
    private lateinit var dailyCb: CheckBox
    private lateinit var listBox: LinearLayout
    private lateinit var banner: LinearLayout

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun fmt(h: Int, m: Int): String {
        val ap = if (h >= 12) "PM" else "AM"
        val hh = if (h % 12 == 0) 12 else h % 12
        return String.format("%d:%02d %s", hh, m, ap)
    }

    private fun button(label: String, bg: String, fg: String = "#FFFFFF", onClick: () -> Unit) =
        Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.parseColor(fg))
            setBackgroundColor(Color.parseColor(bg))
            setOnClickListener { onClick() }
        }

    private fun field(hint: String) = EditText(this).apply {
        this.hint = hint
        setSingleLine(true)
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(40))
        }
        col.addView(TextView(this).apply {
            text = "Call Alarm"; textSize = 32f; setTextColor(Color.parseColor("#14231C"))
        })
        col.addView(TextView(this).apply {
            text = "Pick who to call and when. At that time it rings and you tap Call."
            textSize = 15f; setTextColor(Color.parseColor("#5D6F66"))
            setPadding(0, dp(4), 0, dp(16))
        })

        banner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#FFF1CC"))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            visibility = View.GONE
            addView(TextView(context).apply {
                text = "Allow full-screen alarms so the call screen can appear over your lock screen."
                setTextColor(Color.parseColor("#4A3B00"))
            })
            addView(button("Open setting", "#14231C") {
                if (Build.VERSION.SDK_INT >= 34) {
                    startActivity(
                        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName"))
                    )
                }
            })
        }
        col.addView(banner)

        nameEt = field("Name")
        numEt = field("Phone number").apply { inputType = android.text.InputType.TYPE_CLASS_PHONE }
        col.addView(nameEt)
        col.addView(numEt)
        col.addView(button("Pick from contacts", "#E3E9E6", "#14231C") {
            @Suppress("DEPRECATION")
            startActivityForResult(
                Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI), 7
            )
        })

        timeBtn = button("Choose time", "#E3E9E6", "#14231C") {
            TimePickerDialog(this, { _, h, m ->
                hour = h; minute = m; timeChosen = true
                timeBtn.text = "Call at " + fmt(h, m)
            }, hour, minute, false).show()
        }
        col.addView(timeBtn)

        dailyCb = CheckBox(this).apply { text = "Repeat every day" }
        col.addView(dailyCb)

        col.addView(button("Schedule call", "#12A150") { askMode() })
        col.addView(button("Test in 10 seconds", "#14231C") { test() })

        col.addView(TextView(this).apply {
            text = "Scheduled calls"; textSize = 20f
            setTextColor(Color.parseColor("#14231C"))
            setPadding(0, dp(28), 0, dp(8))
        })
        listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(listBox)

        setContentView(ScrollView(this).apply { addView(col) })

        val perms = mutableListOf(Manifest.permission.CALL_PHONE)
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        requestPermissions(perms.toTypedArray(), 1)
    }

    override fun onResume() {
        super.onResume()
        refresh()
        val needs = Build.VERSION.SDK_INT >= 34 &&
            !getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        banner.visibility = if (needs) View.VISIBLE else View.GONE
    }

    private fun askMode() {
        val n = nameEt.text.toString().trim()
        val p = numEt.text.toString().trim()
        if (n.isEmpty() || p.isEmpty() || !timeChosen) {
            Toast.makeText(this, "Enter a name, number and time", Toast.LENGTH_SHORT).show()
            return
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("How should this call happen?")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .create()

        box.addView(button("Call directly (no confirmation)", "#D32F2F") {
            dialog.dismiss(); save(true)
        })
        box.addView(button("Ask for confirmation", "#12A150") {
            dialog.dismiss(); save(false)
        })
        dialog.show()
    }
    private fun save(direct: Boolean) {
        val n = nameEt.text.toString().trim()
        val p = numEt.text.toString().trim()
        if (n.isEmpty() || p.isEmpty() || !timeChosen) {
            Toast.makeText(this, "Enter a name, number and time", Toast.LENGTH_SHORT).show()
            return
        }
        val e = CallEntry(Store.nextId(this), n, p, hour, minute, dailyCb.isChecked, direct)
        Store.add(this, e)
        Scheduler.schedule(this, e)
        Toast.makeText(this, "Call set for " + fmt(hour, minute), Toast.LENGTH_SHORT).show()
        nameEt.setText(""); numEt.setText("")
        refresh()
    }

    private fun test() {
        val n = nameEt.text.toString().trim()
        val p = numEt.text.toString().trim()
        if (n.isEmpty() || p.isEmpty()) {
            Toast.makeText(this, "Enter a name and number first", Toast.LENGTH_SHORT).show()
            return
        }
        val at = System.currentTimeMillis() + 10_000
        val cal = Calendar.getInstance().apply { timeInMillis = at }
        val e = CallEntry(
            Store.nextId(this), n, p,
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false
        )
        Store.add(this, e)
        Scheduler.schedule(this, e, at)
        Toast.makeText(this, "Ringing in 10 seconds. You can lock the phone.", Toast.LENGTH_LONG).show()
        refresh()
    }

    private fun refresh() {
        listBox.removeAllViews()
        val items = Store.all(this).sortedWith(compareBy({ it.hour }, { it.minute }))
        if (items.isEmpty()) {
            listBox.addView(TextView(this).apply {
                text = "No calls yet. Add one above."
                setTextColor(Color.parseColor("#5D6F66"))
            })
            return
        }
        items.forEach { e ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(10), 0, dp(10))
            }
            row.addView(TextView(this).apply {
                text = fmt(e.hour, e.minute); textSize = 20f
                setTextColor(Color.parseColor("#14231C"))
                minWidth = dp(100)
            })
            row.addView(TextView(this).apply {
                text = e.name + "\n" + e.number + (if (e.daily) "  (daily)" else "") + (if (e.direct) "  (direct)" else "")
                setTextColor(Color.parseColor("#5D6F66"))
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(button("Delete", "#C8432F") {
                Scheduler.cancelAll(this, e.id)
                Store.remove(this, e.id)
                refresh()
            })
            listBox.addView(row)
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 7 && resultCode == RESULT_OK && data?.data != null) {
            val proj = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            contentResolver.query(data.data!!, proj, null, null, null)?.use {
                if (it.moveToFirst()) {
                    nameEt.setText(it.getString(0))
                    numEt.setText(it.getString(1))
                }
            }
        }
    }
}
