package eu.walkerjones.ibridge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Setup guide"

        val topic = findViewById<EditText>(R.id.topic)
        topic.setText(Store.topic(this))

        findViewById<Button>(R.id.generate).setOnClickListener {
            topic.setText(Ntfy.randomTopic())
        }
        findViewById<Button>(R.id.copy).setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("ntfy topic", topic.text.toString()))
            Toast.makeText(this, "Topic copied", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.save).setOnClickListener {
            Store.setTopic(this, topic.text.toString())
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            finish()
        }
        findViewById<Button>(R.id.share).setOnClickListener {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Get my notifications on your iPhone/Watch: install the free 'ntfy' app, " +
                        "then subscribe to topic '" + topic.text.toString() + "' on the default server (ntfy.sh)."
                )
            }
            startActivity(Intent.createChooser(send, "Share topic"))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
