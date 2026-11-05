package com.taksolutions.uasready.ui.presentation

import com.taksolutions.uasready.data.repository.PersistentPilotRepository
import com.taksolutions.uasready.data.repository.PilotAuthorityStore
import com.taksolutions.uasready.domain.model.PilotAuthorityType
import org.junit.Assert.*
import org.junit.Test

class OfflineHonestyTest {

    // --- Home cards never invent values ---

    @Test
    fun weatherCardWithoutObservationSaysNoLiveData() {
        val text = OverviewCardText.weather(assessment = null, observation = null)
        assertEquals(OverviewCardText.NO_LIVE_DATA, text.primary)
        assertFalse(text.primary.contains("°F"))
        assertFalse(text.secondary.contains("SM"))
    }

    @Test
    fun gnssAndSpaceWeatherCardsWithoutDataSayNoLiveData() {
        assertEquals(OverviewCardText.NO_LIVE_DATA, OverviewCardText.gnss(null, null).primary)
        assertEquals(OverviewCardText.NO_LIVE_DATA, OverviewCardText.spaceWeather(null, null).primary)
    }

    @Test
    fun daylightCardWithoutAssessmentDoesNotClaimDaylight() {
        assertNotEquals("Full Daylight", OverviewCardText.daylight(null).primary)
    }

    // --- Freshness ---

    @Test
    fun freshnessFromSourceResults() {
        assertEquals(TelemetryFreshness.LIVE, TelemetryFreshness.of(listOf(true, true, true)))
        assertEquals(TelemetryFreshness.PARTIAL, TelemetryFreshness.of(listOf(true, false, true)))
        assertEquals(TelemetryFreshness.OFFLINE, TelemetryFreshness.of(listOf(false, false)))
        assertEquals(TelemetryFreshness.OFFLINE, TelemetryFreshness.of(emptyList()))
    }

    @Test
    fun offlineChipIsAlertAndNeverGreenZeroMinutes() {
        val neverLive = FreshnessChipText.of(TelemetryFreshness.OFFLINE, null)
        assertEquals(ChipSeverity.ALERT, neverLive.severity)
        assertEquals("Offline • no live data", neverLive.text)

        val wasLive = FreshnessChipText.of(TelemetryFreshness.OFFLINE, 12)
        assertEquals(ChipSeverity.ALERT, wasLive.severity)
        assertEquals("Offline • live data 12m ago", wasLive.text)
    }

    @Test
    fun liveChipTurnsCautionAfterFiveMinutes() {
        assertEquals(ChipSeverity.OK, FreshnessChipText.of(TelemetryFreshness.LIVE, 0).severity)
        assertEquals("Last update • 0m", FreshnessChipText.of(TelemetryFreshness.LIVE, 0).text)
        assertEquals(ChipSeverity.CAUTION, FreshnessChipText.of(TelemetryFreshness.LIVE, 5).severity)
        assertEquals("Last update • 2h", FreshnessChipText.of(TelemetryFreshness.LIVE, 125).text)
    }

    @Test
    fun partialAndPendingChipsAreCaution() {
        assertEquals(ChipSeverity.CAUTION, FreshnessChipText.of(TelemetryFreshness.PARTIAL, 0).severity)
        assertEquals(ChipSeverity.CAUTION, FreshnessChipText.of(null, null).severity)
    }

    // --- Pilot authority persistence ---

    private class FakeStore(var value: PilotAuthorityType? = null) : PilotAuthorityStore {
        override fun load() = value
        override fun save(type: PilotAuthorityType) { value = type }
    }

    @Test
    fun firstLaunchHasNoConfirmedSelection() {
        val repo = PersistentPilotRepository(FakeStore())
        assertFalse(repo.hasConfirmedSelection)
    }

    @Test
    fun selectionSurvivesRelaunch() {
        val store = FakeStore()
        PersistentPilotRepository(store).setAuthority(PilotAuthorityType.PUBLIC_COA)

        val relaunched = PersistentPilotRepository(store)
        assertTrue(relaunched.hasConfirmedSelection)
        assertEquals(PilotAuthorityType.PUBLIC_COA, relaunched.pilotState.value.activeAuthority)
    }

    @Test
    fun changingAuthorityIsSaved() {
        val store = FakeStore(PilotAuthorityType.PUBLIC_COA)
        val repo = PersistentPilotRepository(store)
        repo.setAuthority(PilotAuthorityType.PART_107)
        assertEquals(PilotAuthorityType.PART_107, store.value)
        assertEquals(PilotAuthorityType.PART_107, repo.pilotState.value.activeAuthority)
    }
}
