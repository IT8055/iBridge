package eu.walkerjones.ibridge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment

/** Tab 3: ntfy topic setup, notification-access grant, and a test send. */
class SetupFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_setup, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()
        val topic = view.findViewById<EditText>(R.id.topic)
        topic.setText(Store.topic(ctx))

        view.findViewById<Button>(R.id.generate).setOnClickListener {
            topic.setText(Ntfy.randomTopic())
        }
        view.findViewById<Button>(R.id.copy).setOnClickListener {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("ntfy topic", topic.text.toString()))
            Toast.makeText(ctx, "Topic copied", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<Button>(R.id.save).setOnClickListener {
            Store.setTopic(ctx, topic.text.toString())
            Toast.makeText(ctx, "Saved", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<Button>(R.id.share).setOnClickListener {
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
        view.findViewById<Button>(R.id.grant).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        view.findViewById<Button>(R.id.test).setOnClickListener {
            Store.setTopic(ctx, topic.text.toString())
            Ntfy.publish(ctx, "Test from iBridge", "If this reaches your watch, it works!", 4) { ok, msg ->
                activity?.runOnUiThread {
                    Toast.makeText(
                        ctx,
                        if (ok) "Sent OK ($msg)" else "Failed: $msg",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
