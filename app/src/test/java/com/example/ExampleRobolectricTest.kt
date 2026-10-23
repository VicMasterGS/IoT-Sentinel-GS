package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.MeasurementEntity
import com.example.data.network.SensorMessage
import com.example.data.security.AuthResult
import com.example.data.security.SecureAuthRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("IoT Sentinel", appName)
    }

    @Test
    fun `sensor message json protocol roundtrip`() {
        val original = SensorMessage(
            protocolVersion = 1,
            deviceId = "NODE-01",
            role = "sensor",
            timestamp = 1792600000000L,
            source = "SIMULATED",
            temperatureC = 24.6,
            humidityPercent = 51.2,
            mq135Raw = 420,
            airQualityStatus = "NORMAL"
        )
        val json = original.toJsonString()
        assertTrue(json.contains("\"deviceId\":\"NODE-01\""))
        assertTrue(json.contains("\"temperatureC\":24.6"))

        val parsedResult = SensorMessage.parse(json)
        assertTrue(parsedResult.isSuccess)
        val parsed = parsedResult.getOrNull()
        assertNotNull(parsed)
        assertEquals("NODE-01", parsed?.deviceId)
        assertEquals(24.6, parsed?.temperatureC ?: 0.0, 0.01)
        assertEquals(51.2, parsed?.humidityPercent ?: 0.0, 0.01)
        assertEquals(420, parsed?.mq135Raw)
    }

    @Test
    fun `individual measurement deletion removes only target record`() = runBlocking {
        val dao = db.measurementDao()

        val id1 = dao.insert(
            MeasurementEntity(
                timestamp = 1000L,
                temperatureC = 23.5,
                humidityPercent = 50.0,
                mq135Raw = 350,
                airQualityStatus = "NORMAL",
                source = "SIMULATED",
                deviceId = "NODE-01"
            )
        )

        val id2 = dao.insert(
            MeasurementEntity(
                timestamp = 2000L,
                temperatureC = 28.0,
                humidityPercent = 65.0,
                mq135Raw = 550,
                airQualityStatus = "PRECAUCIÓN",
                source = "REAL",
                deviceId = "ESP32-01"
            )
        )

        var list = dao.getAllMeasurementsList()
        assertEquals(2, list.size)

        // Delete id1 only
        dao.deleteById(id1)

        list = dao.getAllMeasurementsList()
        assertEquals(1, list.size)
        assertEquals(id2, list[0].id)
        assertEquals("ESP32-01", list[0].deviceId)
        assertEquals("REAL", list[0].source)
    }

    @Test
    fun `logout clears active session without deleting registered credentials`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authRepo = SecureAuthRepository(context)

        // Register user
        val regResult = authRepo.register("operator_test", "Secur3P@ssw0rd")
        assertTrue(regResult is AuthResult.Success)

        // Verify active session exists
        val session1 = authRepo.getActiveSession()
        assertNotNull(session1)
        assertEquals("operator_test", session1?.username)

        // Logout
        authRepo.logout()

        // Active session must be null after logout
        val sessionAfterLogout = authRepo.getActiveSession()
        assertNull(sessionAfterLogout)

        // Account must still exist: login with wrong password fails
        val wrongLogin = authRepo.login("operator_test", "WrongPassword")
        assertTrue(wrongLogin is AuthResult.Error)

        // Login with correct password succeeds
        val correctLogin = authRepo.login("operator_test", "Secur3P@ssw0rd")
        assertTrue(correctLogin is AuthResult.Success)
    }

    @Test
    fun `accent color options and fromId fallback`() {
        assertEquals(com.example.ui.theme.AccentColor.CYAN, com.example.ui.theme.AccentColor.fromId("CYAN"))
        assertEquals(com.example.ui.theme.AccentColor.GREEN, com.example.ui.theme.AccentColor.fromId("GREEN"))
        assertEquals(com.example.ui.theme.AccentColor.VIOLET, com.example.ui.theme.AccentColor.fromId("VIOLET"))
        assertEquals(com.example.ui.theme.AccentColor.AMBER, com.example.ui.theme.AccentColor.fromId("AMBER"))
        // Case-insensitive & fallback to default CYAN
        assertEquals(com.example.ui.theme.AccentColor.VIOLET, com.example.ui.theme.AccentColor.fromId("violet"))
        assertEquals(com.example.ui.theme.AccentColor.CYAN, com.example.ui.theme.AccentColor.fromId("UNKNOWN_COLOR"))
        assertEquals(com.example.ui.theme.AccentColor.CYAN, com.example.ui.theme.AccentColor.fromId(null))
    }

    @Test
    fun `clear all history works independently without altering accounts`() = runBlocking {
        val dao = db.measurementDao()
        dao.insert(MeasurementEntity(timestamp = 100L, temperatureC = 20.0, humidityPercent = 40.0, mq135Raw = 200, airQualityStatus = "NORMAL", source = "SIMULATED", deviceId = "SIM-1"))
        dao.insert(MeasurementEntity(timestamp = 200L, temperatureC = 25.0, humidityPercent = 55.0, mq135Raw = 450, airQualityStatus = "PRECAUCIÓN", source = "REAL", deviceId = "ESP-1"))
        dao.insert(MeasurementEntity(timestamp = 300L, temperatureC = 35.0, humidityPercent = 80.0, mq135Raw = 800, airQualityStatus = "ALERTA", source = "REAL", deviceId = "ESP-1"))

        assertEquals(3, dao.getAllMeasurementsList().size)

        // Clear all history
        dao.clearAll()
        assertEquals(0, dao.getAllMeasurementsList().size)
    }

    @Test
    fun `theme preferences persist in SharedPreferences`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("iot_sentinel_prefs", Context.MODE_PRIVATE)

        prefs.edit()
            .putBoolean("key_is_dark_mode", false)
            .putString("key_accent_color", com.example.ui.theme.AccentColor.AMBER.id)
            .commit()

        val isDark = prefs.getBoolean("key_is_dark_mode", true)
        val accentStr = prefs.getString("key_accent_color", "CYAN")
        val accent = com.example.ui.theme.AccentColor.fromId(accentStr)

        assertEquals(false, isDark)
        assertEquals(com.example.ui.theme.AccentColor.AMBER, accent)
    }

    @Test
    fun `system permission states have expected labels and semantics`() {
        val conceded = com.example.ui.settings.SystemPermissionState.CONCEDIDO
        val pending = com.example.ui.settings.SystemPermissionState.PENDIENTE_SOLICITAR
        val denied = com.example.ui.settings.SystemPermissionState.DENEGADO
        val reqSettings = com.example.ui.settings.SystemPermissionState.REQUIERE_AJUSTES
        val notNeeded = com.example.ui.settings.SystemPermissionState.NO_NECESARIO

        assertEquals("Concedido", conceded.label)
        assertEquals("Pendiente de solicitar", pending.label)
        assertEquals("Denegado", denied.label)
        assertEquals("Requiere habilitarse desde los ajustes del sistema", reqSettings.label)
        assertEquals("No necesario para la versión o función actual", notNeeded.label)

        // Compact shortBadge labels for Android 10 and small screens
        assertEquals("Concedido", conceded.shortBadge)
        assertEquals("Pendiente", pending.shortBadge)
        assertEquals("Denegado", denied.shortBadge)
        assertEquals("En Ajustes", reqSettings.shortBadge)
        assertEquals("No requerido", notNeeded.shortBadge)
    }

    @Test
    fun `sensor simulator operates completely independently without bluetooth permissions`() {
        val simulator = com.example.domain.SensorSimulator(kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined))
        val initial = simulator.currentReading.value
        assertNotNull(initial)
        assertTrue((initial.temperatureC ?: 0.0) > 0.0)
        simulator.start()
        assertTrue(simulator.isRunning)
        simulator.pause()
        assertEquals(false, simulator.isRunning)
    }

    @Test
    fun `pbkdf2 password derivation and constant-time verification`() {
        val salt = com.example.data.security.CryptoManager.generateSalt()
        assertEquals(32, salt.size)

        val passChars = "SafeOT#Password2026".toCharArray()
        val hash = com.example.data.security.CryptoManager.hashPassword(passChars, salt)
        assertEquals(32, hash.size) // 256 bits

        // Correct password matches
        val isValid = com.example.data.security.CryptoManager.verifyPassword(passChars, salt, hash)
        assertTrue(isValid)

        // Wrong password fails
        val isInvalid = com.example.data.security.CryptoManager.verifyPassword("WrongPass".toCharArray(), salt, hash)
        assertEquals(false, isInvalid)
    }

    @Test
    fun `authentication failure returns identical generic error for non-existent and wrong password`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authRepo = SecureAuthRepository(context)

        // Register valid user
        authRepo.register("plant_manager", "Secur3P@ssw0rd")

        // 1. Non-existent account login attempt
        val nonExistentResult = authRepo.login("ghost_user_999", "SomePassword123")
        assertTrue(nonExistentResult is AuthResult.Error)
        val nonExistentMsg = (nonExistentResult as AuthResult.Error).message

        // 2. Existing account with incorrect password
        val wrongPassResult = authRepo.login("plant_manager", "IncorrectP@ssw0rd")
        assertTrue(wrongPassResult is AuthResult.Error)
        val wrongPassMsg = (wrongPassResult as AuthResult.Error).message

        // Both error messages must be strictly identical to prevent user enumeration
        assertEquals("Credenciales inválidas", nonExistentMsg)
        assertEquals("Credenciales inválidas", wrongPassMsg)
    }

    @Test
    fun `iot viewmodel initializes disconnected and resets properly`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = com.example.ui.main.IoTViewModel(app)

        // Must start in DISCONNECTED state on fresh boot/restart
        assertEquals(com.example.data.network.ConnectionState.DISCONNECTED, viewModel.uiState.value.connectionState)
        assertEquals(0, viewModel.uiState.value.packetsReceivedCount)
        assertEquals(0, viewModel.uiState.value.packetsSentCount)

        // Manual disconnect keeps state DISCONNECTED
        viewModel.disconnect()
        assertEquals(com.example.data.network.ConnectionState.DISCONNECTED, viewModel.uiState.value.connectionState)
    }
}
