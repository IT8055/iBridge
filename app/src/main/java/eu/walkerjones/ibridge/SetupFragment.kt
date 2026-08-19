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

/** Tab 3: ntfy topic and server setup, notification-access grant, and a test send. */
class SetupFragment : Fragment() {

    private lateinit var topic: EditText
    private lateinit var server: EditText
    private lateinit var token: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_setup, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()
        topic = view.findViewById(R.id.topic)
        server = view.findViewById(R.id.server)
        token = view.findViewById(R.id.token)
        topic.setText(Store.topic(ctx))
        server.setText(Store.server(ctx))
        token.setText(Store.token(ctx))

        view.findViewById<Button>(R.id.generate).setOnClickListener {
            topic.setText(Ntfy.randomTopic())
        }
        view.findViewById<Button>(R.id.copy).setOnClickListener {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("ntfy topic", topic.text.toString()))
            Toast.makeText(ctx, "Topic copied", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<Button>(R.id.reset_server).setOnClickListener {
            server.setText(Store.DEFAULT_SERVER)
            token.setText("")
            save(ctx, quiet = true)
            Toast.makeText(ctx, "Back to ${Store.DEFAULT_SERVER}", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<Button>(R.id.save).setOnClickListener { save(ctx) }
        view.findViewById<Button>(R.id.share).setOnClickListener {
            val where = Store.normalizeServer(server.text.toString())
            val serverNote =
                if (where == Store.DEFAULT_SERVER) "on the default server (ntfy.sh)"
                else "on the server $where (change the server in the app's settings before adding it)"
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Get my notifications on your iPhone/Watch: install the free 'ntfy' app, " +
                        "then subscribe to topic '" + topic.text.toString() + "' " + serverNote + "."
                )
            }
            startActivity(Intent.createChooser(send, "Share topic"))
        }
        view.findViewById<Button>(R.id.grant).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        view.findViewById<Button>(R.id.test).setOnClickListener {
            if (!save(ctx, quiet = true)) return@setOnClickListener
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

    /** Persists the three fields. Returns false (and complains) if the server URL is unusable. */
    private fun save(ctx: Context, quiet: Boolean = false): Boolean {
        val problem = Store.validateServer(server.text.toString())
        if (problem != null) {
            Toast.makeText(ctx, problem, Toast.LENGTH_LONG).show()
            return false
        }
        Store.setTopic(ctx, topic.text.toString())
        Store.setServer(ctx, server.text.toString())
        Store.setToken(ctx, token.text.toString())
        server.setText(Store.server(ctx))
        if (Store.server(ctx).startsWith("http://")) {
            Toast.makeText(
                ctx,
                "Saved. Warning: http:// sends your notifications unencrypted.",
                Toast.LENGTH_LONG
            ).show()
        } else if (!quiet) {
            Toast.makeText(ctx, "Saved", Toast.LENGTH_SHORT).show()
        }
        return true
    }
}
