package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class SolarCalculatorTest {

    @Test
    fun testCalculateSolarTimes_SanFrancisco() {
        // San Francisco: 37.7749 N, -122.4194 W
        // Test date: Autumn (October 6, 2026)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles")).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 6)
            set(Calendar.HOUR_OF_DAY, 12)
        }

        val solarTimes = SolarCalculator.calculateSolarTimes(cal, 37.7749, -122.4194)

        assertNotNull("Sunrise should be calculated", solarTimes.sunriseMillis)
        assertNotNull("Sunset should be calculated", solarTimes.sunsetMillis)

        val sunriseCal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles")).apply {
            timeInMillis = solarTimes.sunriseMillis!!
        }
        val sunsetCal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles")).apply {
            timeInMillis = solarTimes.sunsetMillis!!
        }

        // SF Sunrise in early October is ~7:00-7:15 AM PDT
        assertEquals(7, sunriseCal.get(Calendar.HOUR_OF_DAY))
        assertTrue(sunriseCal.get(Calendar.MINUTE) in 0..30)

        // SF Sunset in early October is ~18:40-18:55 (6:40-6:55 PM) PDT
        assertEquals(18, sunsetCal.get(Calendar.HOUR_OF_DAY))
        assertTrue(sunsetCal.get(Calendar.MINUTE) in 35..60)

        assertTrue("Sunrise must occur before sunset on same day", solarTimes.sunriseMillis!! < solarTimes.sunsetMillis!!)
    }

    @Test
    fun testCalculateSolarTimes_London() {
        // London: 51.5074 N, -0.1278 W
        // Test date: Summer solstice (June 21, 2026)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/London")).apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JUNE)
            set(Calendar.DAY_OF_MONTH, 21)
            set(Calendar.HOUR_OF_DAY, 12)
        }

        val solarTimes = SolarCalculator.calculateSolarTimes(cal, 51.5074, -0.1278)

        assertNotNull(solarTimes.sunriseMillis)
        assertNotNull(solarTimes.sunsetMillis)

        val sunriseCal = Calendar.getInstance(TimeZone.getTimeZone("Europe/London")).apply {
            timeInMillis = solarTimes.sunriseMillis!!
        }
        val sunsetCal = Calendar.getInstance(TimeZone.getTimeZone("Europe/London")).apply {
            timeInMillis = solarTimes.sunsetMillis!!
        }

        // London summer solstice sunrise is ~4:43 AM BST (hour 4)
        assertEquals(4, sunriseCal.get(Calendar.HOUR_OF_DAY))
        // London summer solstice sunset is ~21:21 (9:21 PM) BST (hour 21)
        assertEquals(21, sunsetCal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun testCalculateNextSolarTriggerMillis_futureEvent() {
        val now = System.currentTimeMillis()
        val lat = 37.7749
        val lng = -122.4194

        val nextSunrise = SolarCalculator.calculateNextSolarTriggerMillis(
            nowMillis = now,
            lat = lat,
            lng = lng,
            isSunrise = true,
            offsetMinutes = 0
        )
        assertNotNull(nextSunrise)
        assertTrue("Next sunrise must be in the future", nextSunrise!! > now)

        val nextSunset = SolarCalculator.calculateNextSolarTriggerMillis(
            nowMillis = now,
            lat = lat,
            lng = lng,
            isSunrise = false,
            offsetMinutes = -15 // 15 minutes before sunset
        )
        assertNotNull(nextSunset)
        assertTrue("Next sunset must be in the future", nextSunset!! > now)
    }
}
