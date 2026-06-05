package eu.walkerjones.ibridge

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch

/** One row in the Apps list: an app plus its derived state. */
data class AppRow(
    val pkg: String,
    val label: String,
    val count: Int,
    val last: Store.LogEntry?,
    val blocked: Boolean,
    val sched: Store.AppSchedule
)

class AppsAdapter(
    private var items: List<AppRow>,
    private val onToggleAllowed: (pkg: String, allowed: Boolean) -> Unit,
    private val onSchedule: (AppRow) -> Unit
) : RecyclerView.Adapter<AppsAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
        val label: TextView = view.findViewById(R.id.label)
        val meta: TextView = view.findViewById(R.id.meta)
        val schedule: TextView = view.findViewById(R.id.schedule)
        val blockSwitch: MaterialSwitch = view.findViewById(R.id.block_switch)
        val scheduleBtn: Button = view.findViewById(R.id.schedule_btn)
    }

    fun submit(list: List<AppRow>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = items[position]
        val ctx = holder.itemView.context

        holder.label.text = row.label

        val parts = ArrayList<String>()
        parts.add(if (row.count == 1) "1 msg" else "${row.count} msgs")
        row.last?.let { e ->
            val snippet = e.title.ifBlank { e.text }.ifBlank { "(no text)" }
            val time = DateUtils.getRelativeTimeSpanString(
                e.ts, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
            )
            parts.add("last $time: $snippet")
        }
        holder.meta.text = parts.joinToString("  ·  ")

        holder.schedule.text = when (row.sched.mode) {
            Store.SchedMode.ALWAYS -> "Always allowed"
            Store.SchedMode.WINDOW -> "Only ${row.sched.start}–${row.sched.end}"
            Store.SchedMode.DEFAULT -> ""
        }
        holder.schedule.visibility =
            if (row.sched.mode == Store.SchedMode.DEFAULT) View.GONE else View.VISIBLE

        holder.icon.setImageDrawable(
            try {
                ctx.packageManager.getApplicationIcon(row.pkg)
            } catch (e: Exception) {
                ctx.packageManager.defaultActivityIcon
            }
        )

        // Switch ON = allowed (mirroring). Detach listener while setting state to avoid feedback.
        holder.blockSwitch.setOnCheckedChangeListener(null)
        holder.blockSwitch.isChecked = !row.blocked
        holder.blockSwitch.setOnCheckedChangeListener { _, isChecked ->
            onToggleAllowed(row.pkg, isChecked)
        }

        holder.scheduleBtn.setOnClickListener { onSchedule(row) }
    }
}
