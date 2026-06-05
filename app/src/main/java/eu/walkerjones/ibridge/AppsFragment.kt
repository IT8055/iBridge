package eu.walkerjones.ibridge

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch

/** Tab 2: per-app allow/block, per-app schedules, plus global quiet-hours default. */
class AppsFragment : Fragment() {

    private enum class Status { ALL, ALLOWED, BLOCKED }

    private val timeRe = Regex("^\\d{2}:\\d{2}$")

    private lateinit var adapter: AppsAdapter
    private lateinit var empty: TextView
    private lateinit var searchView: EditText
    private lateinit var sortSpinner: Spinner
    private lateinit var statusGroup: ChipGroup

    private var sortIndex = 0
    private var status = Status.ALL

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_apps, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()

        // ---- Default quiet hours ----
        val quietEnabled = view.findViewById<MaterialSwitch>(R.id.quiet_enabled)
        val quietStart = view.findViewById<EditText>(R.id.quiet_start)
        val quietEnd = view.findViewById<EditText>(R.id.quiet_end)
        quietEnabled.isChecked = Store.quietEnabled(ctx)
        quietStart.setText(Store.quietStart(ctx))
        quietEnd.setText(Store.quietEnd(ctx))
        view.findViewById<Button>(R.id.save_quiet).setOnClickListener {
            Store.setQuietEnabled(ctx, quietEnabled.isChecked)
            Store.setQuiet(ctx, quietStart.text.toString(), quietEnd.text.toString())
            Toast.makeText(ctx, "Quiet hours saved", Toast.LENGTH_SHORT).show()
            refresh()
        }

        // ---- Search ----
        searchView = view.findViewById(R.id.search)
        searchView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = refresh()
        })

        // ---- Sort ----
        sortSpinner = view.findViewById(R.id.sort)
        sortSpinner.adapter = ArrayAdapter(
            ctx,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Name (A–Z)", "Most messages", "Recently active", "Blocked first")
        )
        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                sortIndex = pos
                refresh()
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        // ---- Status filter ----
        statusGroup = view.findViewById(R.id.status_filter)
        statusGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            status = when (checkedIds.firstOrNull()) {
                R.id.chip_allowed -> Status.ALLOWED
                R.id.chip_blocked -> Status.BLOCKED
                else -> Status.ALL
            }
            refresh()
        }

        // ---- List ----
        empty = view.findViewById(R.id.empty)
        adapter = AppsAdapter(emptyList(),
            onToggleAllowed = { pkg, allowed ->
                Store.setBlocked(ctx, pkg, !allowed)
                refresh()
            },
            onSchedule = { showScheduleDialog(it) }
        )
        view.findViewById<RecyclerView>(R.id.list).apply {
            layoutManager = LinearLayoutManager(ctx)
            adapter = this@AppsFragment.adapter
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val ctx = context ?: return
        val query = searchView.text.toString().trim().lowercase()

        var rows = Store.seenApps(ctx).map { app ->
            AppRow(
                pkg = app.pkg,
                label = app.label,
                count = app.count,
                last = Store.lastForApp(ctx, app.pkg),
                blocked = Store.isBlocked(ctx, app.pkg),
                sched = Store.appSchedule(ctx, app.pkg)
            )
        }

        if (query.isNotEmpty()) {
            rows = rows.filter {
                it.label.lowercase().contains(query) || it.pkg.lowercase().contains(query)
            }
        }
        rows = when (status) {
            Status.ALLOWED -> rows.filter { !it.blocked }
            Status.BLOCKED -> rows.filter { it.blocked }
            Status.ALL -> rows
        }
        rows = when (sortIndex) {
            1 -> rows.sortedByDescending { it.count }
            2 -> rows.sortedByDescending { it.last?.ts ?: 0L }
            3 -> rows.sortedWith(compareByDescending<AppRow> { it.blocked }.thenBy { it.label.lowercase() })
            else -> rows.sortedBy { it.label.lowercase() }
        }

        adapter.submit(rows)
        empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showScheduleDialog(row: AppRow) {
        val ctx = requireContext()
        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_schedule, null)
        val modeDefault = view.findViewById<android.widget.RadioButton>(R.id.mode_default)
        val modeAlways = view.findViewById<android.widget.RadioButton>(R.id.mode_always)
        val modeWindow = view.findViewById<android.widget.RadioButton>(R.id.mode_window)
        val windowRow = view.findViewById<View>(R.id.window_row)
        val start = view.findViewById<EditText>(R.id.start)
        val end = view.findViewById<EditText>(R.id.end)

        start.setText(row.sched.start)
        end.setText(row.sched.end)
        when (row.sched.mode) {
            Store.SchedMode.ALWAYS -> modeAlways.isChecked = true
            Store.SchedMode.WINDOW -> modeWindow.isChecked = true
            Store.SchedMode.DEFAULT -> modeDefault.isChecked = true
        }
        fun syncWindow() {
            windowRow.visibility = if (modeWindow.isChecked) View.VISIBLE else View.GONE
        }
        syncWindow()
        modeDefault.setOnClickListener { syncWindow() }
        modeAlways.setOnClickListener { syncWindow() }
        modeWindow.setOnClickListener { syncWindow() }

        MaterialAlertDialogBuilder(ctx)
            .setTitle("Schedule — ${row.label}")
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val mode = when {
                    modeAlways.isChecked -> Store.SchedMode.ALWAYS
                    modeWindow.isChecked -> Store.SchedMode.WINDOW
                    else -> Store.SchedMode.DEFAULT
                }
                val s = start.text.toString().trim()
                val e = end.text.toString().trim()
                if (mode == Store.SchedMode.WINDOW && !(timeRe.matches(s) && timeRe.matches(e))) {
                    Toast.makeText(ctx, "Use HH:mm times, e.g. 08:00", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                Store.setAppSchedule(ctx, row.pkg, Store.AppSchedule(mode, s, e))
                refresh()
            }
            .show()
    }
}
