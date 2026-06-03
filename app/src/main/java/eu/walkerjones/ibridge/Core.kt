package eu.walkerjones.ibridge

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Store {
    private const val NAME = "ibridge"
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

    fun inQuietHours(c: Context): Boolean {
        if (!quietEnabled(c)) return false
        val now = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date())
        val s = quietStart(c)
        val e = quietEnd(c)
        return if (s <= e) now >= s && now < e else now >= s || now < e
    }

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

    fun shouldSend(c: Context, pkg: String): Boolean {
        if (!enabled(c)) return false
        if (isBlocked(c, pkg)) return false
        if (inQuietHours(c)) return false
        return true
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
