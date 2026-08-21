package com.uasready.data.repository

import com.uasready.domain.model.SunData
import java.util.Calendar
import java.util.TimeZone

interface SolarRepository {
    fun calculateSunData(latitude: Double, longitude: Double, dateEpochMs: Long = System.currentTimeMillis()): SunData
}

class AstronomicalSolarRepository : SolarRepository {

    override fun calculateSunData(
        latitude: Double,
        longitude: Double,
        dateEpochMs: Long
    ): SunData {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = dateEpochMs
        }

        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)

        val midnightCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val midnightEpochMs = midnightCal.timeInMillis

        // Standard NOAA Equation of Time and Solar Noon Calculation
        val bDeg = (360.0 / 365.0) * (dayOfYear - 81)
        val bRad = Math.toRadians(bDeg)

        // Equation of Time (minutes)
        val eotMinutes = 9.87 * Math.sin(2.0 * bRad) - 7.53 * Math.cos(bRad) - 1.5 * Math.sin(bRad)

        // Solar Declination (degrees)
        val declinationDeg = 23.45 * Math.sin(bRad)
        val declinationRad = Math.toRadians(declinationDeg)
        val latRad = Math.toRadians(latitude)

        // Solar Noon in UTC minutes from UTC midnight
        val solarNoonUtcMinutes = 720.0 - (4.0 * longitude) - eotMinutes

        // Official Sunrise/Sunset: zenith = 90.833 degrees
        val sunriseDeltaMin = calculateHourAngleMinutes(latRad, declinationRad, 90.833)
        val sunriseUtcMin = solarNoonUtcMinutes - sunriseDeltaMin
        val sunsetUtcMin = solarNoonUtcMinutes + sunriseDeltaMin

        // Civil Twilight: zenith = 96.0 degrees
        val civilDeltaMin = calculateHourAngleMinutes(latRad, declinationRad, 96.0)
        val dawnUtcMin = solarNoonUtcMinutes - civilDeltaMin
        val duskUtcMin = solarNoonUtcMinutes + civilDeltaMin

        val dawnEpochMs = midnightEpochMs + (dawnUtcMin * 60 * 1000).toLong()
        val sunriseEpochMs = midnightEpochMs + (sunriseUtcMin * 60 * 1000).toLong()
        val sunsetEpochMs = midnightEpochMs + (sunsetUtcMin * 60 * 1000).toLong()
        val duskEpochMs = midnightEpochMs + (duskUtcMin * 60 * 1000).toLong()

        val isDaylight = dateEpochMs in sunriseEpochMs..sunsetEpochMs
        val isTwilight = (dateEpochMs in dawnEpochMs until sunriseEpochMs) || (dateEpochMs in sunsetEpochMs..duskEpochMs)
        val daylightRemainingMin = if (dateEpochMs < sunsetEpochMs) {
            ((sunsetEpochMs - dateEpochMs) / (60 * 1000)).coerceAtLeast(0)
        } else {
            0L
        }

        return SunData(
            civilDawnEpochMs = dawnEpochMs,
            sunriseEpochMs = sunriseEpochMs,
            sunsetEpochMs = sunsetEpochMs,
            civilDuskEpochMs = duskEpochMs,
            isDaylight = isDaylight,
            daylightRemainingMinutes = daylightRemainingMin,
            isCivilTwilight = isTwilight,
            sourceName = "NOAA Astronomical Algorithm"
        )
    }

    private fun calculateHourAngleMinutes(latRad: Double, decRad: Double, zenithDeg: Double): Double {
        val zenithRad = Math.toRadians(zenithDeg)
        val cosH = (Math.cos(zenithRad) - (Math.sin(latRad) * Math.sin(decRad))) /
                (Math.cos(latRad) * Math.cos(decRad))

        val clampedCosH = cosH.coerceIn(-1.0, 1.0)
        val hDeg = Math.toDegrees(Math.acos(clampedCosH))
        // 1 degree of hour angle = 4 minutes of time
        return hDeg * 4.0
    }
}
