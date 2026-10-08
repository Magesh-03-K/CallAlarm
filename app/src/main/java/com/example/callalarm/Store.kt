package com.example.callalarm

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CallEntry(
    val id: Int,
    val name: String,
    val number: String,
    val hour: Int,
    val minute: Int,
    val daily: Boolean,
    val direct: Boolean = false
)

object Store {
    private const val PREFS = "calls"

    fun all(c: Context): MutableList<CallEntry> {
        val out = mutableListOf<CallEntry>()
        val raw = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("list", "[]") ?: "[]"
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                CallEntry(
                    o.getInt("id"), o.getString("name"), o.getString("number"),
                    o.getInt("hour"), o.getInt("minute"), o.getBoolean("daily"),
                    o.optBoolean("direct", false)
                )
            )
        }
        return out
    }

    private fun write(c: Context, list: List<CallEntry>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject().put("id", it.id).put("name", it.name).put("number", it.number)
                    .put("hour", it.hour).put("minute", it.minute).put("daily", it.daily)
                    .put("direct", it.direct))
        }
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("list", arr.toString()).apply()
    }

    fun add(c: Context, e: CallEntry) {
        val l = all(c)
        l.add(e)
        write(c, l)
    }

    fun remove(c: Context, id: Int) {
        write(c, all(c).filter { it.id != id })
    }

    fun find(c: Context, id: Int): CallEntry? = all(c).firstOrNull { it.id == id }

    fun nextId(c: Context): Int {
        val p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val n = p.getInt("next", 1)
        p.edit().putInt("next", n + 1).apply()
        return n
    }
}
