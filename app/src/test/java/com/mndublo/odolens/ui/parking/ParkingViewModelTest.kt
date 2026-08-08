package com.mndublo.odolens.ui.parking

import android.graphics.Bitmap
import com.mndublo.odolens.api.ParkingTicketData
import com.mndublo.odolens.data.ParkingSettingsSource
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** In-memory [ParkingSettingsSource] recording writes for assertions. */
internal class FakeParkingSettings : ParkingSettingsSource {
    val geminiApiKeyFlow = MutableStateFlow("")
    val use12hFlow = MutableStateFlow(false)
    val expiryFlow = MutableStateFlow(0L)
    val alarmTimeFlow = MutableStateFlow("")
    val spotFlow = MutableStateFlow("")
    val startFlow = MutableStateFlow("")
    val freeFlow = MutableStateFlow(0)
    val offsetFlow = MutableStateFlow(60)

    var savedOffsetMinutes: Int? = null
    var savedTimer: SavedTimer? = null
    var cleared = false

    data class SavedTimer(
        val expiryMs: Long,
        val alarmTime: String,
        val spotNote: String,
        val startTime: String,
        val freeDurationMinutes: Int,
        val offsetMinutes: Int
    )

    override val geminiApiKey: Flow<String> get() = geminiApiKeyFlow
    override val notificationOffsetMinutes: Flow<Int> get() = offsetFlow
    override val use12HourFormat: Flow<Boolean> get() = use12hFlow
    override val parkingExpiryMs: Flow<Long> get() = expiryFlow
    override val parkingAlarmTime: Flow<String> get() = alarmTimeFlow
    override val parkingSpotNote: Flow<String> get() = spotFlow
    override val parkingStartTime: Flow<String> get() = startFlow
    override val parkingFreeDuration: Flow<Int> get() = freeFlow
    override val parkingOffsetMinutes: Flow<Int> get() = offsetFlow

    override suspend fun saveNotificationOffsetMinutes(minutes: Int) {
        savedOffsetMinutes = minutes
        offsetFlow.value = minutes
    }

    override suspend fun saveParkingTimer(
        expiryMs: Long,
        alarmTime: String,
        spotNote: String,
        startTime: String,
        freeDurationMinutes: Int,
        offsetMinutes: Int
    ) {
        savedTimer = SavedTimer(expiryMs, alarmTime, spotNote, startTime, freeDurationMinutes, offsetMinutes)
        expiryFlow.value = expiryMs
        alarmTimeFlow.value = alarmTime
        spotFlow.value = spotNote
        startFlow.value = startTime
        freeFlow.value = freeDurationMinutes
        offsetFlow.value = offsetMinutes
    }

    override suspend fun clearParkingTimer() {
        cleared = true
        expiryFlow.value = 0L
        alarmTimeFlow.value = ""
        spotFlow.value = ""
        startFlow.value = ""
        freeFlow.value = 0
        offsetFlow.value = 60
    }
}

/** Fake [ParkingAlarmScheduler] with configurable behavior. */
internal open class FakeScheduler : ParkingAlarmScheduler {
    var scheduled = false
    var scheduledAlarmMs = 0L
    var extendResult = true
    var cancelParkingCalls = 0
    var cancelExpiryCalls = 0
    var instantConfirmations = 0

    override suspend fun scheduleParkingAlarm(
        startTime: String,
        freeDurationMinutes: Int,
        offsetMinutes: Int,
        spotNote: String,
        use12h: Boolean
    ): Long {
        scheduled = true
        scheduledAlarmMs = System.currentTimeMillis() + 3_600_000L
        return scheduledAlarmMs
    }

    override fun scheduleExpiryAlarm(expiryMs: Long, spotNote: String, use12h: Boolean) {}

    override fun cancelParkingAlarm() {
        cancelParkingCalls++
    }

    override fun cancelExpiryAlarm() {
        cancelExpiryCalls++
    }

    override fun showInstantConfirmation(
        startTime: String,
        freeDurationMinutes: Int,
        offsetMinutes: Int,
        scheduledAlarmTimeStr: String,
        spotNote: String,
        use12h: Boolean
    ) {
        instantConfirmations++
    }

    override suspend fun extendTimer(additionalMinutes: Int): Boolean = extendResult

    override suspend fun cancelTimer() {}
}

internal class ThrowingScheduler : FakeScheduler() {
    override suspend fun scheduleParkingAlarm(
        startTime: String,
        freeDurationMinutes: Int,
        offsetMinutes: Int,
        spotNote: String,
        use12h: Boolean
    ): Long = throw RuntimeException("boom")
}

internal class FakeParser : ParkingTicketParser {
    var result: Result<ParkingTicketData> = Result.failure(Exception("no result"))
    override suspend fun parse(apiKey: String, bitmap: Bitmap?): Result<ParkingTicketData> = result
}

class ParkingViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class Fixture(
        val vm: ParkingViewModel,
        val settings: FakeParkingSettings,
        val scheduler: FakeScheduler,
        val parser: FakeParser,
        val nowMs: Long
    )

    private fun fixture(
        settings: FakeParkingSettings = FakeParkingSettings(),
        scheduler: FakeScheduler = FakeScheduler(),
        parser: FakeParser = FakeParser(),
        nowMs: Long = System.currentTimeMillis() - 24 * 3600_000L
    ): Fixture = Fixture(
        ParkingViewModel(settings, scheduler, parser, now = { nowMs }),
        settings,
        scheduler,
        parser,
        nowMs
    )

    @Test
    fun `scheduleAlarm persists timer and signals success`() = runTest {
        val f = fixture()
        f.vm.onStartTimeChange("09:30")
        f.vm.onFreeDurationChange("60")
        f.vm.onSpotNoteChange("2F 21")

        f.vm.scheduleAlarm()

        val saved = f.settings.savedTimer
        assertNotNull(saved)
        saved?.let {
            assertEquals("09:30", it.startTime)
            assertEquals(60, it.freeDurationMinutes)
            assertEquals("2F 21", it.spotNote)
            assertEquals(60, it.offsetMinutes)
            assertTrue(it.expiryMs > 0L)
        }
        assertTrue(f.scheduler.scheduled)
        assertEquals(1, f.scheduler.instantConfirmations)
        assertTrue(f.vm.uiState.value.alarmJustScheduled)
        assertEquals(null, f.vm.uiState.value.errorMessage)
        f.vm.viewModelScope.cancel()
    }

    @Test
    fun `scheduleAlarm failure sets error and failed flag`() = runTest {
        val f = fixture(scheduler = ThrowingScheduler())
        f.vm.onStartTimeChange("09:30")
        f.vm.onFreeDurationChange("60")
        f.vm.scheduleAlarm()

        assertTrue(f.vm.uiState.value.scheduleFailed)
        assertTrue(f.vm.uiState.value.errorMessage.orEmpty().contains("Failed to schedule alarm"))
        f.vm.viewModelScope.cancel()
    }

    @Test
    fun `scheduleAlarm with expired free window shows error and does not schedule`() = runTest {
        // Reproduced bug scenario: started 30 min ago with 30 min free -> the window has
        // already elapsed. A real clock is used so the past window is genuinely in the past.
        val f = fixture(nowMs = System.currentTimeMillis())
        f.vm.onQuickStart(30)
        f.vm.onFreeDurationChange("30")

        f.vm.scheduleAlarm()

        assertNotNull(f.vm.uiState.value.errorMessage)
        assertTrue(f.vm.uiState.value.errorMessage.orEmpty().contains("already expired"))
        assertTrue(f.vm.uiState.value.scheduleFailed)
        assertFalse(f.scheduler.scheduled)
        assertEquals(null, f.settings.savedTimer)
        f.vm.viewModelScope.cancel()
    }

    @Test
    fun `resetTimer cancels alarms and resets fields`() = runTest {
        val f = fixture()
        f.settings.expiryFlow.value = System.currentTimeMillis() + 3_600_000L
        assertTrue(f.vm.uiState.value.isTimerRunning)

        f.vm.resetTimer()

        assertTrue(f.settings.cleared)
        assertEquals(1, f.scheduler.cancelParkingCalls)
        assertEquals(1, f.scheduler.cancelExpiryCalls)
        assertEquals("00:00", f.vm.uiState.value.startTimeInput)
        assertEquals("0", f.vm.uiState.value.freeDurationInput)
        assertEquals("", f.vm.uiState.value.parkingSpotNoteInput)
        assertEquals("", f.vm.uiState.value.countdownText)
        assertFalse(f.vm.uiState.value.isTimerRunning)
        f.vm.viewModelScope.cancel()
    }

    @Test
    fun `extendTimer success does not set failure flag`() = runTest {
        val f = fixture()
        f.scheduler.extendResult = true
        f.vm.extendTimer(2)
        assertFalse(f.vm.uiState.value.extendFailed)
        f.vm.viewModelScope.cancel()
    }

    @Test
    fun `extendTimer at cap sets failure flag`() = runTest {
        val f = fixture()
        f.scheduler.extendResult = false
        f.vm.extendTimer(1)
        assertTrue(f.vm.uiState.value.extendFailed)
        f.vm.consumeExtendFailed()
        assertFalse(f.vm.uiState.value.extendFailed)
        f.vm.viewModelScope.cancel()
    }

    @Test
    fun `parseTicket success fills form fields`() = runTest {
        val f = fixture()
        f.parser.result = Result.success(ParkingTicketData(startTime = "10:15", freeDurationMinutes = 90))
        f.vm.parseTicket(null) // bitmap is only passed to the (fake) parser, never touched

        assertEquals("10:15", f.vm.uiState.value.startTimeInput)
        assertEquals("90", f.vm.uiState.value.freeDurationInput)
        assertFalse(f.vm.uiState.value.isAiLoading)
        f.vm.viewModelScope.cancel()
    }

    @Test
    fun `parseTicket failure sets error message`() = runTest {
        val f = fixture()
        f.parser.result = Result.failure(Exception("nope"))
        f.vm.parseTicket(null)

        assertEquals("AI Parse failed: nope", f.vm.uiState.value.errorMessage)
        assertFalse(f.vm.uiState.value.isAiLoading)
        f.vm.viewModelScope.cancel()
    }

    @Test
    fun `countdown starts when a timer is active`() = runTest {
        val f = fixture()
        f.settings.expiryFlow.value = f.nowMs + 2 * 3600_000L

        assertTrue(
            "expected HH:mm:ss countdown, was '${f.vm.uiState.value.countdownText}'",
            f.vm.uiState.value.countdownText.matches(Regex("\\d{2}:\\d{2}:\\d{2}"))
        )
        f.vm.viewModelScope.cancel()
    }

    @Test
    fun `progress fraction recomputes when the free duration lands after expiry`() = runTest {
        val f = fixture()
        // Expiry arrives before the persisted free duration (separate settings flows).
        f.settings.expiryFlow.value = f.nowMs + 30 * 60_000L
        // Zero duration -> no window -> full ring.
        assertEquals(1f, f.vm.uiState.value.timerProgressFraction)

        // Duration lands: 30 min remaining of a 60 min window -> ~0.5 remaining.
        f.settings.freeFlow.value = 60
        val fraction = f.vm.uiState.value.timerProgressFraction
        assertTrue(
            "expected ~0.5 remaining, was $fraction",
            fraction in 0.4f..0.6f
        )
        f.vm.viewModelScope.cancel()
    }

    @Test
    fun `offset selection persists`() = runTest {
        val f = fixture()
        f.vm.onOffsetSelected(30)

        assertEquals(30, f.settings.savedOffsetMinutes)
        assertEquals(30, f.vm.uiState.value.warningOffsetMinutes)
        assertFalse(f.vm.uiState.value.isCustomOffsetSelected)
        f.vm.viewModelScope.cancel()
    }
}
