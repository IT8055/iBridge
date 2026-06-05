package eu.walkerjones.ibridge

import android.content.Context
import androidx.core.content.ContextCompat

/** Short human label for a message outcome, e.g. for chips and rows. */
fun Store.Outcome.label(): String = when (this) {
    Store.Outcome.SENT -> "Sent"
    Store.Outcome.FAILED -> "Failed"
    Store.Outcome.BLOCKED_OFF -> "Blocked (off)"
    Store.Outcome.BLOCKED_APP -> "Blocked (app)"
    Store.Outcome.BLOCKED_SCHEDULE -> "Blocked (schedule)"
}

/** Colour used to tint the outcome chip/text. */
fun Store.Outcome.color(c: Context): Int = ContextCompat.getColor(
    c,
    when (this) {
        Store.Outcome.SENT -> R.color.outcome_sent
        Store.Outcome.FAILED -> R.color.outcome_failed
        else -> R.color.outcome_blocked
    }
)
