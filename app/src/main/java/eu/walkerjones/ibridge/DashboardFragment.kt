package eu.walkerjones.ibridge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch

/** Tab 1: master toggle, ntfy topic, and the last 10 sent/blocked messages. */
class DashboardFragment : Fragment() {

    private lateinit var status: TextView
    private lateinit var topicView: TextView
    private lateinit var enabled: MaterialSwitch
    private lateinit var empty: TextView
    private lateinit var adapter: LogAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()
        status = view.findViewById(R.id.status)
        topicView = view.findViewById(R.id.topic)
        enabled = view.findViewById(R.id.enabled)
        empty = view.findViewById(R.id.empty)

        enabled.setOnCheckedChangeListener { _, isChecked -> Store.setEnabled(ctx, isChecked) }

        view.findViewById<Button>(R.id.copy).setOnClickListener {
            val topic = Store.topic(ctx)
            if (topic.isBlank()) {
                Toast.makeText(ctx, "No topic set — add one in Setup", Toast.LENGTH_SHORT).show()
            } else {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("ntfy topic", topic))
                Toast.makeText(ctx, "Topic copied", Toast.LENGTH_SHORT).show()
            }
        }

        adapter = LogAdapter(emptyList()) { showDetail(it) }
        view.findViewById<RecyclerView>(R.id.list).apply {
            layoutManager = LinearLayoutManager(ctx)
            adapter = this@DashboardFragment.adapter
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val ctx = requireContext()
        val granted = isNotificationAccessGranted(ctx)
        val topic = Store.topic(ctx)
        status.text = when {
            !granted -> "✗ Notification access not granted. Open Setup to grant it."
            topic.isBlank() -> "⚠ No ntfy topic set yet. Open Setup to create one."
            else -> "✓ Ready — mirroring to ntfy."
        }
        enabled.isChecked = Store.enabled(ctx)
        topicView.text = topic.ifBlank { "(not set)" }

        val recent = Store.log(ctx).take(10)
        adapter.submit(recent)
        empty.visibility = if (recent.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showDetail(e: Store.LogEntry) {
        val ctx = requireContext()
        val when_ = DateUtils.formatDateTime(
            ctx, e.ts,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
        )
        val body = buildString {
            append(e.outcome.label()).append("  ·  ").append(when_).append("\n\n")
            if (e.title.isNotBlank()) append(e.title).append("\n")
            append(e.text.ifBlank { "(no text)" })
        }
        val blocked = Store.isBlocked(ctx, e.pkg)
        MaterialAlertDialogBuilder(ctx)
            .setTitle(e.label)
            .setMessage(body)
            .setNegativeButton("Close", null)
            .setPositiveButton(if (blocked) "Unblock this app" else "Block this app") { _, _ ->
                Store.setBlocked(ctx, e.pkg, !blocked)
                Toast.makeText(
                    ctx,
                    if (!blocked) "${e.label} blocked" else "${e.label} allowed",
                    Toast.LENGTH_SHORT
                ).show()
                refresh()
            }
            .show()
    }
}

/** True if iBridge's NotificationListenerService has been granted access in system settings. */
fun isNotificationAccessGranted(c: Context): Boolean {
    val flat = Settings.Secure.getString(c.contentResolver, "enabled_notification_listeners")
        ?: return false
    return flat.split(":").any { it.contains(c.packageName) }
}
