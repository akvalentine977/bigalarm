package com.akvalentine.bigalarm.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditsTest {

    private val inactive = Alarm(id = 7, name = "Take meds", hour = 8, minute = 30, enabled = false, createdAt = 1)

    @Test
    fun untouchedInactiveAlarmStaysInactive() {
        assertFalse(Edits.isChanged(inactive, inactive.copy()))
        assertFalse(Edits.onSave(inactive, inactive.copy()).enabled)
    }

    @Test
    fun anyChangeActivatesAnInactiveAlarm() {
        val edits = listOf(
            inactive.copy(name = "Take vitamins"),
            inactive.copy(hour = 9),
            inactive.copy(minute = 45),
            inactive.copy(repeat = Repeat(RepeatType.DAILY)),
            inactive.copy(repeat = inactive.repeat.copy(intervalDays = 3)),
            inactive.copy(vibrate = !inactive.vibrate),
            inactive.copy(sound = !inactive.sound),
            inactive.copy(snoozeMinutes = 5),
            inactive.copy(ringUntilDismissed = true),
        )
        for (edited in edits) {
            assertTrue("expected a change: $edited", Edits.isChanged(inactive, edited))
            val saved = Edits.onSave(inactive, edited)
            assertTrue("expected enabled: $edited", saved.enabled)
            // Nothing else is altered by the rule.
            assertEquals(edited.copy(enabled = true), saved)
        }
    }

    @Test
    fun changeThenRevertLeavesItInactive() {
        val reverted = inactive.copy(hour = 9).copy(hour = 8)
        assertFalse(Edits.onSave(inactive, reverted).enabled)
    }

    @Test
    fun activeAlarmIsLeftAlone() {
        val active = inactive.copy(enabled = true)
        val edited = active.copy(hour = 10)
        assertEquals(edited, Edits.onSave(active, edited))
        assertEquals(active, Edits.onSave(active, active.copy()))
    }

    @Test
    fun newAlarmIsLeftAlone() {
        val fresh = Alarm(name = "New", hour = 6, minute = 0)
        assertEquals(fresh, Edits.onSave(null, fresh))
    }
}
