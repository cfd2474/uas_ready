package com.taksolutions.uasready.domain.model

import kotlinx.serialization.Serializable

/**
 * One FAA "National Security UAS Flight Restrictions" polygon (14 CFR 99.7 special security
 * instructions): bases, ports of entry, the US–Mexico border strip. See docs/PLATFORM_REFERENCE.md.
 */
@Serializable
data class NationalSecurityUasSite(
    /** FAA_ID, e.g. "20261016-DHS-ZAB-2". */
    val faaId: String,
    /** Human name built from Base / Facility. */
    val name: String,
    /** Proponent + branch, e.g. "DHS CBP". */
    val agency: String,
    val floorText: String,
    val ceilingText: String,
    /** Waiver / point-of-contact text from the FAA layer. */
    val contact: String,
    /**
     * One of the three US–Mexico border strips (FDC 6/9954, 6/9978, 6/9956). Their FAA polygons are
     * narrower than the NOTAM's 1 NM in places, so the evaluator widens them (docs/PLATFORM_REFERENCE.md).
     */
    val isMexicoBorderStrip: Boolean = false,
    /** Outer rings (lat, lon); may be simplified (map display and proximity only). */
    val polygons: List<List<Pair<Double, Double>>> = emptyList()
)

/** Where the launch point sits relative to the Washington DC SFRA (14 CFR 93.335 + FDC 5/7224). */
@Serializable
enum class DcSfraZone {
    /** Inside the DC Flight Restricted Zone: UAS prohibited without a TSA/FAA airspace waiver. */
    FRZ,
    /** Inside the DC SFRA but outside the FRZ: UAS allowed only under the NOTAM's conditions. */
    SFRA
}

/** National-security UAS restrictions around a launch point. Produced only when every query answered. */
data class SecurityAirspaceSnapshot(
    /** Sites whose full-resolution polygon contains the launch point (server-side intersection). */
    val sitesAtLaunchPoint: List<NationalSecurityUasSite>,
    /** Sites near the launch point, with simplified geometry. */
    val nearbySites: List<NationalSecurityUasSite>,
    val dcSfraZone: DcSfraZone?,
    val fetchedEpochMs: Long
)
