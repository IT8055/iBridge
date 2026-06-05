package eu.walkerjones.ibridge

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/** Renders [Store.LogEntry] rows for the dashboard's recent-messages list. */
class LogAdapter(
    private var items: List<Store.LogEntry>,
    private val onClick: (Store.LogEntry) -> Unit
) : RecyclerView.Adapter<LogAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val label: TextView = view.findViewById(R.id.app_label)
        val title: TextView = view.findViewById(R.id.title)
        val outcome: TextView = view.findViewById(R.id.outcome)
        val time: TextView = view.findViewById(R.id.time)
    }

    fun submit(list: List<Store.LogEntry>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = items[position]
        holder.label.text = e.label
        holder.title.text = e.title.ifBlank { e.text }.ifBlank { "(no text)" }
        holder.outcome.text = e.outcome.label()
        holder.outcome.setTextColor(e.outcome.color(holder.itemView.context))
        holder.time.text = DateUtils.getRelativeTimeSpanString(
            e.ts, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
        )
        holder.itemView.setOnClickListener { onClick(e) }
    }
}
