package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimeParserTest {

    @Test
    fun testParseToHourMinute_9pmFormats() {
        assertEquals(Pair(21, 0), TimeParser.parseToHourMinute("9pm"))
        assertEquals(Pair(21, 0), TimeParser.parseToHourMinute("9 PM"))
        assertEquals(Pair(21, 0), TimeParser.parseToHourMinute("9:00 PM"))
        assertEquals(Pair(21, 0), TimeParser.parseToHourMinute("9:00pm"))
        assertEquals(Pair(21, 0), TimeParser.parseToHourMinute("21:00"))
        assertEquals(Pair(21, 30), TimeParser.parseToHourMinute("9:30 PM"))
        assertEquals(Pair(21, 30), TimeParser.parseToHourMinute("9:30pm"))
        assertEquals(Pair(21, 30), TimeParser.parseToHourMinute("21:30"))
    }

    @Test
    fun testParseToHourMinute_morningAndNoon() {
        assertEquals(Pair(9, 0), TimeParser.parseToHourMinute("9:00 AM"))
        assertEquals(Pair(9, 0), TimeParser.parseToHourMinute("9am"))
        assertEquals(Pair(0, 0), TimeParser.parseToHourMinute("12:00 AM"))
        assertEquals(Pair(12, 0), TimeParser.parseToHourMinute("12:00 PM"))
        assertEquals(Pair(8, 30), TimeParser.parseToHourMinute("08:30"))
    }

    @Test
    fun testFormat24And12Hour() {
        assertEquals("21:00", TimeParser.format24Hour(21, 0))
        assertEquals("9:00 PM", TimeParser.format12Hour(21, 0))
        assertEquals("12:00 AM", TimeParser.format12Hour(0, 0))
        assertEquals("12:00 PM", TimeParser.format12Hour(12, 0))
        assertEquals("8:30 AM", TimeParser.format12Hour(8, 30))
    }

    @Test
    fun testFormatFriendlyPreview() {
        assertEquals("9:00 PM (21:00)", TimeParser.formatFriendlyPreview("9pm"))
        assertEquals("9:00 PM (21:00)", TimeParser.formatFriendlyPreview("21:00"))
        assertEquals("9:30 PM (21:30)", TimeParser.formatFriendlyPreview("9:30 pm"))
        assertEquals("Invalid time", TimeParser.formatFriendlyPreview("invalid"))
    }

    @Test
    fun testInvalidInputs() {
        assertNull(TimeParser.parseToHourMinute("invalid"))
        assertNull(TimeParser.parseToHourMinute(""))
        assertNull(TimeParser.parseToHourMinute(null))
        assertNull(TimeParser.parseToHourMinute("25:00"))
        assertNull(TimeParser.parseToHourMinute("9:65 PM"))
    }
}
