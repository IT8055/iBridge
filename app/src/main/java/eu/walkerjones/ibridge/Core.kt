package eu.walkerjones.ibridge

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Store {
    private const val NAME = "ibridge"
    private const val LOG_CAP = 200
    private fun p(c: Context) = c.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun topic(c: Context): String = p(c).getString("topic", "") ?: ""
    fun setTopic(c: Context, v: String) = p(c).edit().putString("topic", v.trim()).apply()

    fun server(c: Context): String =
        (p(c).getString("server", "https://ntfy.sh") ?: "https://ntfy.sh").trimEnd('/')

    fun enabled(c: Context): Boolean = p(c).getBoolean("enabled", true)
    fun setEnabled(c: Context, v: Boolean) = p(c).edit().putBoolean("enabled", v).apply()

    fun blocked(c: Context): MutableSet<String> =
        HashSet(p(c).getStringSet("blocked", emptySet()) ?: emptySet())

    fun isBlocked(c: Context, pkg: String): Boolean = blocked(c).contains(pkg)

    fun setBlocked(c: Context, pkg: String, block: Boolean) {
        val set = blocked(c)
        if (block) set.add(pkg) else set.remove(pkg)
        p(c).edit().putStringSet("blocked", set).apply()
    }

    fun quietEnabled(c: Context): Boolean = p(c).getBoolean("quiet_enabled", false)
    fun setQuietEnabled(c: Context, v: Boolean) = p(c).edit().putBoolean("quiet_enabled", v).apply()
    fun quietStart(c: Context): String = p(c).getString("quiet_start", "23:00") ?: "23:00"
    fun quietEnd(c: Context): String = p(c).getString("quiet_end", "07:00") ?: "07:00"

    fun setQuiet(c: Context, start: String, end: String) {
        val re = Regex("^\\d{2}:\\d{2}$")
        val s = if (re.matches(start.trim())) start.trim() else "23:00"
        val e = if (re.matches(end.trim())) end.trim() else "07:00"
        p(c).edit().putString("quiet_start", s).putString("quiet_end", e).apply()
    }

    private fun nowHHmm(): String =
        java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date())

    /** True if the current time falls inside [start, end). Handles windows that wrap past midnight. */
    private fun inWindow(now: String, start: String, end: String): Boolean =
        if (start <= end) now >= start && now < end else now >= start || now < end

    fun inQuietHours(c: Context): Boolean {
        if (!quietEnabled(c)) return false
        return inWindow(nowHHmm(), quietStart(c), quietEnd(c))
    }

    // ---- Per-app schedules ----

    enum class SchedMode { DEFAULT, ALWAYS, WINDOW }

    data class AppSchedule(
        val mode: SchedMode = SchedMode.DEFAULT,
        val start: String = "08:00",
        val end: String = "22:00"
    )

    fun appSchedule(c: Context, pkg: String): AppSchedule {
        val obj = JSONObject(p(c).getString("schedules", "{}") ?: "{}")
        val e = obj.optJSONObject(pkg) ?: return AppSchedule()
        val mode = try {
            SchedMode.valueOf(e.optString("mode", "DEFAULT"))
        } catch (ex: Exception) {
            SchedMode.DEFAULT
        }
        return AppSchedule(mode, e.optString("start", "08:00"), e.optString("end", "22:00"))
    }

    fun setAppSchedule(c: Context, pkg: String, s: AppSchedule) {
        synchronized(Store) {
            val obj = JSONObject(p(c).getString("schedules", "{}") ?: "{}")
            if (s.mode == SchedMode.DEFAULT) {
                obj.remove(pkg)
            } else {
                obj.put(pkg, JSONObject().apply {
                    put("mode", s.mode.name)
                    put("start", s.start)
                    put("end", s.end)
                })
            }
            p(c).edit().putString("schedules", obj.toString()).apply()
        }
    }

    // ---- Message log ----

    enum class Outcome { SENT, FAILED, BLOCKED_OFF, BLOCKED_APP, BLOCKED_SCHEDULE }

    data class LogEntry(
        val pkg: String,
        val label: String,
        val title: String,
        val text: String,
        val ts: Long,
        val outcome: Outcome
    )

    fun addLog(c: Context, entry: LogEntry) {
        synchronized(Store) {
            val arr = JSONArray(p(c).getString("log", "[]") ?: "[]")
            val obj = JSONObject().apply {
                put("pkg", entry.pkg)
                put("label", entry.label)
                put("title", entry.title)
                put("text", entry.text)
                put("ts", entry.ts)
                put("outcome", entry.outcome.name)
            }
            // Newest first; trim to the cap.
            val trimmed = JSONArray()
            trimmed.put(obj)
            val keep = minOf(arr.length(), LOG_CAP - 1)
            for (i in 0 until keep) trimmed.put(arr.get(i))
            p(c).edit().putString("log", trimmed.toString()).apply()
        }
    }

    private fun parseLog(c: Context): List<LogEntry> {
        val arr = JSONArray(p(c).getString("log", "[]") ?: "[]")
        val list = ArrayList<LogEntry>(arr.length())
        for (i in 0 until arr.length()) {
            val e = arr.getJSONObject(i)
            val outcome = try {
                Outcome.valueOf(e.optString("outcome", "SENT"))
            } catch (ex: Exception) {
                Outcome.SENT
            }
            list.add(
                LogEntry(
                    e.optString("pkg"),
                    e.optString("label"),
                    e.optString("title"),
                    e.optString("text"),
                    e.optLong("ts", 0L),
                    outcome
                )
            )
        }
        return list
    }

    /** Whole log, newest first. */
    fun log(c: Context): List<LogEntry> = parseLog(c)

    /** Most recent logged entry for a package, or null. */
    fun lastForApp(c: Context, pkg: String): LogEntry? = parseLog(c).firstOrNull { it.pkg == pkg }

    data class SeenApp(val pkg: String, val label: String, val count: Int)

    fun recordSeen(c: Context, pkg: String, label: String) {
        synchronized(Store) {
            val obj = JSONObject(p(c).getString("seen", "{}") ?: "{}")
            val entry = obj.optJSONObject(pkg) ?: JSONObject()
            entry.put("label", label)
            entry.put("count", entry.optInt("count", 0) + 1)
            obj.put(pkg, entry)
            p(c).edit().putString("seen", obj.toString()).apply()
        }
    }

    fun seenApps(c: Context): List<SeenApp> {
        val obj = JSONObject(p(c).getString("seen", "{}") ?: "{}")
        val list = ArrayList<SeenApp>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val e = obj.getJSONObject(k)
            list.add(SeenApp(k, e.optString("label", k), e.optInt("count", 0)))
        }
        list.sortByDescending { it.count }
        return list
    }

    /**
     * Decides what should happen to a notification from [pkg] and why.
     * Returns [Outcome.SENT] when it should be delivered; otherwise the blocking reason.
     */
    fun evaluate(c: Context, pkg: String): Outcome {
        if (!enabled(c)) return Outcome.BLOCKED_OFF
        if (isBlocked(c, pkg)) return Outcome.BLOCKED_APP
        val sched = appSchedule(c, pkg)
        when (sched.mode) {
            SchedMode.ALWAYS -> {}
            SchedMode.WINDOW ->
                if (!inWindow(nowHHmm(), sched.start, sched.end)) return Outcome.BLOCKED_SCHEDULE
            SchedMode.DEFAULT ->
                if (inQuietHours(c)) return Outcome.BLOCKED_SCHEDULE
        }
        return Outcome.SENT
    }
}

object Ntfy {
    fun publish(
        context: Context,
        title: String,
        body: String,
        priority: Int,
        callback: ((Boolean, String) -> Unit)? = null
    ) {
        val topic = Store.topic(context)
        val server = Store.server(context)
        if (topic.isBlank()) {
            callback?.invoke(false, "No ntfy topic set")
            return
        }
        Thread {
            var ok = false
            var msg = ""
            var conn: HttpURLConnection? = null
            try {
                val json = JSONObject().apply {
                    put("topic", topic)
                    put("title", if (title.isNotEmpty()) title else "Notification")
                    put("message", if (body.isNotEmpty()) body else " ")
                    put("priority", priority.coerceIn(1, 5))
                }.toString()
                conn = (URL(server).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 10000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
                conn.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                ok = code in 200..299
                msg = "HTTP $code"
            } catch (e: Exception) {
                msg = e.message ?: "error"
            } finally {
                conn?.disconnect()
            }
            callback?.invoke(ok, msg)
        }.start()
    }

    fun randomTopic(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        val rnd = java.security.SecureRandom()
        val sb = StringBuilder("ibridge-")
        repeat(12) { sb.append(chars[rnd.nextInt(chars.length)]) }
        return sb.toString()
    }
}
