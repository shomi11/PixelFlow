package com.example.util

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.*

/**
 * High-precision astronomical solar calculator based on standard NOAA solar equations.
 * Calculates exact sunrise and sunset times for any coordinate on Earth.
 */
object SolarCalculator {

    data class SolarTimes(
        val sunriseMillis: Long?,
        val sunsetMillis: Long?
    )

    /**
     * Calculates sunrise and sunset epoch milliseconds for a given calendar date, latitude, and longitude.
     *
     * @param calendar Local calendar date
     * @param latitude North positive, South negative (-90.0 to 90.0)
     * @param longitude East positive, West negative (-180.0 to 180.0)
     * @param zenith Astronomical zenith (default 90.8333° for official sunrise/sunset with atmospheric refraction)
     */
    fun calculateSolarTimes(
        calendar: Calendar,
        latitude: Double,
        longitude: Double,
        zenith: Double = 90.8333
    ): SolarTimes {
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

        val sunriseUtc = computeSolarTimeUtc(dayOfYear, latitude, longitude, zenith, isSunrise = true)
        val sunsetUtc = computeSolarTimeUtc(dayOfYear, latitude, longitude, zenith, isSunrise = false)

        val sunriseMillis = sunriseUtc?.let { utcHours ->
            calendarToMillis(calendar, utcHours)
        }
        val sunsetMillis = sunsetUtc?.let { utcHours ->
            calendarToMillis(calendar, utcHours)
        }

        return SolarTimes(sunriseMillis, sunsetMillis)
    }

    private fun computeSolarTimeUtc(
        dayOfYear: Int,
        lat: Double,
        lng: Double,
        zenith: Double,
        isSunrise: Boolean
    ): Double? {
        val lngHour = lng / 15.0
        val t = if (isSunrise) {
            dayOfYear + ((6.0 - lngHour) / 24.0)
        } else {
            dayOfYear + ((18.0 - lngHour) / 24.0)
        }

        // Sun's mean anomaly in degrees
        val m = (0.9856 * t) - 3.289
        val mRad = Math.toRadians(m)

        // Sun's true longitude in degrees
        var l = m + (1.916 * sin(mRad)) + (0.020 * sin(2.0 * mRad)) + 282.634
        l = normalizeDegrees(l)
        val lRad = Math.toRadians(l)

        // Sun's right ascension in degrees
        var ra = Math.toDegrees(atan(0.91764 * tan(lRad)))
        ra = normalizeDegrees(ra)

        // Right ascension quadrant adjustment
        val lQuadrant = floor(l / 90.0) * 90.0
        val raQuadrant = floor(ra / 90.0) * 90.0
        ra += (lQuadrant - raQuadrant)
        val raHours = ra / 15.0

        // Sun's declination
        val sinDec = 0.39782 * sin(lRad)
        val cosDec = cos(asin(sinDec))

        // Sun's local hour angle
        val latRad = Math.toRadians(lat)
        val zenithRad = Math.toRadians(zenith)
        val cosH = (cos(zenithRad) - (sinDec * sin(latRad))) / (cosDec * cos(latRad))

        if (cosH > 1.0) {
            // Sun never rises at this location on this date (polar night)
            return null
        }
        if (cosH < -1.0) {
            // Sun never sets at this location on this date (midnight sun)
            return null
        }

        val hDeg = if (isSunrise) {
            360.0 - Math.toDegrees(acos(cosH))
        } else {
            Math.toDegrees(acos(cosH))
        }
        val hHours = hDeg / 15.0

        // Local mean time
        val tLocal = hHours + raHours - (0.06571 * t) - 6.622

        // UTC time in hours
        val ut = tLocal - lngHour
        return normalizeHours(ut)
    }

    private fun normalizeDegrees(deg: Double): Double {
        var result = deg % 360.0
        if (result < 0) result += 360.0
        return result
    }

    private fun normalizeHours(hours: Double): Double {
        var result = hours % 24.0
        if (result < 0) result += 24.0
        return result
    }

    private fun calendarToMillis(localCalendar: Calendar, utcHours: Double): Long {
        val hour = utcHours.toInt().coerceIn(0, 23)
        val minuteDouble = (utcHours - hour) * 60.0
        val minute = minuteDouble.toInt().coerceIn(0, 59)
        val second = ((minuteDouble - minute) * 60.0).toInt().coerceIn(0, 59)

        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, localCalendar.get(Calendar.YEAR))
            set(Calendar.MONTH, localCalendar.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, localCalendar.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, 0)
        }

        var millis = utcCal.timeInMillis
        val localCheck = Calendar.getInstance(localCalendar.timeZone).apply {
            timeInMillis = millis
        }

        val targetYear = localCalendar.get(Calendar.YEAR)
        val targetDayOfYear = localCalendar.get(Calendar.DAY_OF_YEAR)

        val checkYear = localCheck.get(Calendar.YEAR)
        val checkDayOfYear = localCheck.get(Calendar.DAY_OF_YEAR)

        if (checkYear < targetYear || (checkYear == targetYear && checkDayOfYear < targetDayOfYear)) {
            millis += 24 * 3600 * 1000L
        } else if (checkYear > targetYear || (checkYear == targetYear && checkDayOfYear > targetDayOfYear)) {
            millis -= 24 * 3600 * 1000L
        }

        return millis
    }

    /**
     * Calculates the next upcoming epoch timestamp (in milliseconds) for sunrise or sunset,
     * accounting for optional offset in minutes and days-of-week filter.
     */
    fun calculateNextSolarTriggerMillis(
        nowMillis: Long,
        lat: Double,
        lng: Double,
        isSunrise: Boolean,
        offsetMinutes: Int = 0,
        activeDays: Set<Int> = emptySet()
    ): Long? {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
        }

        val offsetMillis = offsetMinutes * 60 * 1000L

        // Search the next 14 days for the soonest matching solar trigger
        for (dayOffset in 0..14) {
            val candidateCal = (calendar.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }

            val solarTimes = calculateSolarTimes(candidateCal, lat, lng)
            val eventMillis = if (isSunrise) solarTimes.sunriseMillis else solarTimes.sunsetMillis

            if (eventMillis != null) {
                val targetMillis = eventMillis + offsetMillis
                if (targetMillis > nowMillis) {
                    val eventCal = Calendar.getInstance().apply { timeInMillis = targetMillis }
                    val dayOfWeek = eventCal.get(Calendar.DAY_OF_WEEK)
                    if (activeDays.isEmpty() || activeDays.contains(dayOfWeek)) {
                        return targetMillis
                    }
                }
            }
        }
        return null
    }
}
