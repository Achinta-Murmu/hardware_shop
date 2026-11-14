package com.galla.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Small vibrations that confirm a touch, the way Google's own apps do.
 * Android only plays them when "Touch feedback" is switched on in the phone's settings,
 * so there is no separate switch inside Galla.
 */
class Haptics(private val feedback: HapticFeedback) {
    /** A light tick: picking a chip, a tab, a segment, a slider step. */
    fun tick() = feedback.performHapticFeedback(HapticFeedbackType.SegmentTick)

    /** A soft click for buttons. */
    fun click() = feedback.performHapticFeedback(HapticFeedbackType.ContextClick)

    /** A clear "done": a trip started, a change confirmed. */
    fun confirm() = feedback.performHapticFeedback(HapticFeedbackType.Confirm)

    /** A "no": something could not be done, or was undone. */
    fun reject() = feedback.performHapticFeedback(HapticFeedbackType.Reject)

    /** Switches and stars: a different feel for on and off. */
    fun toggle(on: Boolean) =
        feedback.performHapticFeedback(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
}

@Composable
fun rememberHaptics(): Haptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { Haptics(feedback) }
}
