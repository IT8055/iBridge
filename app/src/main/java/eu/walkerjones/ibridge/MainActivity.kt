package eu.walkerjones.ibridge

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val topic = findViewById<EditText>(R.id.topic)
        val enabled = findViewById<Switch>(R.id.enabled)

        topic.setText(Store.topic(this))
        enabled.isChecked = Store.enabled(this)
        enabled.setOnCheckedChangeListener { _, isChecked -> Store.setEnabled(this, isChecked) }

        findViewById<Button>(R.id.save).setOnClickListener {
            Store.setTopic(this, topic.text.toString())
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.setup).setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java))
        }
        findViewById<Button>(R.id.filters).setOnClickListener {
            startActivity(Intent(this, FilterActivity::class.java))
        }
        findViewById<Button>(R.id.grant).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        findViewById<Button>(R.id.test).setOnClickListener {
            Store.setTopic(this, topic.text.toString())
            Ntfy.publish(this, "Test from iBridge", "If this reaches your watch, it works!", 4) { ok, msg ->
                runOnUiThread {
                    Toast.makeText(this, if (ok) "Sent OK ($msg)" else "Failed: $msg", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val status = findViewById<TextView>(R.id.status)
        val granted = isAccessGranted()
        val hasTopic = Store.topic(this).isNotBlank()
        status.text = when {
            !granted -> "✗ Notification access NOT granted. Tap “Grant notification access”."
            !hasTopic -> "⚠ No ntfy topic set yet. Open “Setup guide” to create one."
            else -> "✓ Ready — mirroring to topic “" + Store.topic(this) + "”."
        }
    }

    private fun isAccessGranted(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return flat.split(":").any { it.contains(packageName) }
    }
}
