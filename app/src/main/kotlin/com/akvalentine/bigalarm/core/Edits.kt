package com.akvalentine.bigalarm.core

/** Rules applied when the user saves an edited alarm. Pure Kotlin, unit-tested. */
object Edits {

    /** True if anything about the alarm differs, other than whether it is switched on or snoozed. */
    fun isChanged(original: Alarm, edited: Alarm): Boolean =
        edited.copy(enabled = original.enabled, snoozedUntil = original.snoozedUntil) != original

    /**
     * What to store when the user saves [edited]. [original] is the alarm as it was when editing
     * began, or null for a new alarm. An inactive alarm that was changed in any way becomes active;
     * saving it untouched leaves it inactive.
     */
    fun onSave(original: Alarm?, edited: Alarm): Alarm =
        if (original != null && !original.enabled && isChanged(original, edited)) {
            edited.copy(enabled = true)
        } else {
            edited
        }
}
