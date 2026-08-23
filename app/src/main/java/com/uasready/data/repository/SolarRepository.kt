package com.uasready.data.repository

import com.uasready.domain.model.SunData
import java.util.Calendar

interface SolarRepository {
    fun calculateSunData(latitude: Double, longitude: Double, dateEpochMs: Long = System.currentTimeMillis()): SunData
}

class AstronomicalSolarRepository : SolarRepository {

    override fun calculateSunData(
        latitude: Double,
        longitude: Double,
        dateEpochMs: Long
    ): SunData {
        // Evaluate the solar day based on the local time of the device/target location
        val localCal = Calendar.getInstance().apply {
            timeInMillis = dateEpochMs
        }

        val dayOfYear = localCal.get(Calendar.DAY_OF_YEAR)

        // Local midnight epoch (00:00:00.000 local time on the evaluation day)
        val localMidnightCal = Calendar.getInstance().apply {
            timeInMillis = dateEpochMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val localMidnightEpochMs = localMidnightCal.timeInMillis

        // Local timezone offset in minutes from UTC (including DST if active)
        val tzOffsetMinutes = localCal.timeZone.getOffset(dateEpochMs) / (60 * 1000.0)

        // Standard NOAA Equation of Time and Solar Declination
        val bDeg = (360.0 / 365.0) * (dayOfYear - 81)
        val bRad = Math.toRadians(bDeg)

        // Equation of Time (minutes)
        val eotMinutes = 9.87 * Math.sin(2.0 * bRad) - 7.53 * Math.cos(bRad) - 1.5 * Math.sin(bRad)

        // Solar Declination (degrees)
        val declinationDeg = 23.45 * Math.sin(bRad)
        val declinationRad = Math.toRadians(declinationDeg)
        val latRad = Math.toRadians(latitude)

        // Solar Noon in LOCAL minutes from local midnight
        // Solar Noon in UTC minutes from UTC midnight: 720.0 - (4.0 * longitude) - eotMinutes
        // Local Solar Noon = UTC Solar Noon + Local TimeZone Offset
        val solarNoonLocalMinutes = 720.0 - (4.0 * longitude) - eotMinutes + tzOffsetMinutes

        // Official Sunrise/Sunset: zenith = 90.833 degrees
        val sunriseDeltaMin = calculateHourAngleMinutes(latRad, declinationRad, 90.833)
        val sunriseLocalMin = solarNoonLocalMinutes - sunriseDeltaMin
        val sunsetLocalMin = solarNoonLocalMinutes + sunriseDeltaMin

        // Civil Twilight: zenith = 96.0 degrees
        val civilDeltaMin = calculateHourAngleMinutes(latRad, declinationRad, 96.0)
        val dawnLocalMin = solarNoonLocalMinutes - civilDeltaMin
        val duskLocalMin = solarNoonLocalMinutes + civilDeltaMin

        val dawnEpochMs = localMidnightEpochMs + (dawnLocalMin * 60 * 1000).toLong()
        val sunriseEpochMs = localMidnightEpochMs + (sunriseLocalMin * 60 * 1000).toLong()
        val sunsetEpochMs = localMidnightEpochMs + (sunsetLocalMin * 60 * 1000).toLong()
        val duskEpochMs = localMidnightEpochMs + (duskLocalMin * 60 * 1000).toLong()

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
