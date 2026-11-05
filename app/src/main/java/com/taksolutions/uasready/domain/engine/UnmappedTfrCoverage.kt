package com.taksolutions.uasready.domain.engine

import com.taksolutions.uasready.domain.model.TfrNotice

/**
 * Some TFRs have no geometry in the TFR feed but are published as polygons in another FAA layer
 * (docs/PLATFORM_REFERENCE.md "US–Mexico border UAS TFRs" and "DC SFRA / FRZ"). While those
 * layers answer, the notice is evaluated there instead of as an unmapped TFR.
 */
enum class TfrCoverage {
    /** FDC 6/9954, 6/9978, 6/9956 — the 1 NM strip in the FAA National Security UAS layer. */
    MEXICO_BORDER,
    /** FDC 5/7224 — DC SFRA / FRZ polygons in FAA Boundary_Airspace. */
    DC_SFRA,
    /** No other source; only the NOTAM text can tell the pilot whether it applies. */
    NONE
}

object UnmappedTfrCoverage {

    fun of(notice: TfrNotice): TfrCoverage {
        val header = notice.textHeader.uppercase()
        val is997 = notice.cfrSection?.trim()?.startsWith("99.7") == true
        return when {
            is997 && (header.contains("MEXICO BORDER") || notice.description.uppercase().contains("MEXICO BORDER")) ->
                TfrCoverage.MEXICO_BORDER
            is997 && header.contains("SPECIAL SECURITY INSTRUCTIONS") && header.contains("UNMANNED AIRCRAFT") &&
                header.contains("SPECIAL FLIGHT RULES AREA") && header.contains("WASHINGTON") ->
                TfrCoverage.DC_SFRA
            else -> TfrCoverage.NONE
        }
    }

    /**
     * States the notice may cover. The TFR list gives only one (6/9954 CA+AZ is listed "AZ"), so the
     * NOTAM text states are added; the DC SFRA extends into Maryland and Virginia.
     */
    fun statesOf(notice: TfrNotice): Set<String> {
        val states = (listOf(notice.state) + notice.textStates).map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toMutableSet()
        when (of(notice)) {
            TfrCoverage.DC_SFRA -> states += setOf("DC", "MD", "VA")
            // Text states are exact for the border notices; if the header could not be read, assume every border state.
            TfrCoverage.MEXICO_BORDER -> if (notice.textStates.isEmpty()) states += setOf("CA", "AZ", "NM", "TX")
            TfrCoverage.NONE -> Unit
        }
        return states
    }
}
