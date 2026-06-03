package eu.walkerjones.ibridge

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class FilterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "App filters"

        val quietEnabled = findViewById<CheckBox>(R.id.quiet_enabled)
        val quietStart = findViewById<EditText>(R.id.quiet_start)
        val quietEnd = findViewById<EditText>(R.id.quiet_end)
        quietEnabled.isChecked = Store.quietEnabled(this)
        quietStart.setText(Store.quietStart(this))
        quietEnd.setText(Store.quietEnd(this))

        findViewById<Button>(R.id.save_quiet).setOnClickListener {
            Store.setQuietEnabled(this, quietEnabled.isChecked)
            Store.setQuiet(this, quietStart.text.toString(), quietEnd.text.toString())
            Toast.makeText(this, "Quiet hours saved", Toast.LENGTH_SHORT).show()
        }

        buildList()
    }

    private fun buildList() {
        val container = findViewById<LinearLayout>(R.id.list)
        container.removeAllViews()
        val apps = Store.seenApps(this)
        if (apps.isEmpty()) {
            val tv = TextView(this)
            tv.text = "No notifications captured yet. Once apps notify you they'll appear here to block or allow."
            tv.setPadding(0, 24, 0, 0)
            container.addView(tv)
            return
        }
        for (app in apps) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 18, 0, 18)
            }
            val label = TextView(this).apply {
                text = app.label + "\n" + app.pkg + "  ·  " + app.count + " msgs"
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val btn = Button(this)
            btn.text = if (Store.isBlocked(this, app.pkg)) "Blocked" else "Allowed"
            btn.setOnClickListener {
                val nowBlocked = !Store.isBlocked(this, app.pkg)
                Store.setBlocked(this, app.pkg, nowBlocked)
                btn.text = if (nowBlocked) "Blocked" else "Allowed"
            }
            row.addView(label)
            row.addView(btn)
            container.addView(row)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
