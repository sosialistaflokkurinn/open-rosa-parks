package `is`.rosaparks.data.api

import `is`.rosaparks.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private val BASE_URL = BuildConfig.API_BASE_URL
private const val TIMEOUT_MS = 10_000

private val json = Json { ignoreUnknownKeys = true }

suspend fun fetchZones(): ZonesResponse =
    withContext(Dispatchers.IO) {
        val conn = URL("$BASE_URL/api/zones").openConnection() as HttpURLConnection
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        try {
            check(conn.responseCode in 200..299) { "HTTP ${conn.responseCode}" }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<ZonesResponse>(text)
        } finally {
            conn.disconnect()
        }
    }

suspend fun fetchGarages(): GaragesResponse =
    withContext(Dispatchers.IO) {
        val conn = URL("$BASE_URL/api/garages").openConnection() as HttpURLConnection
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        try {
            check(conn.responseCode in 200..299) { "HTTP ${conn.responseCode}" }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<GaragesResponse>(text)
        } finally {
            conn.disconnect()
        }
    }

@Serializable
data class StartSessionRequest(
    val plate: String,
    val zoneId: String,
)

@Serializable
data class StartSessionResponse(
    val sessionId: String,
    val plate: String,
    val zoneId: String,
    val startTime: String,
    val status: String,
)

@Serializable
data class StopSessionResponse(
    val sessionId: String,
    val plate: String,
    val zoneId: String,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Int,
    val status: String,
)

suspend fun startParkingSession(
    plate: String,
    zoneId: String,
): StartSessionResponse =
    withContext(Dispatchers.IO) {
        val conn = URL("$BASE_URL/api/parking/start").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        conn.doOutput = true
        try {
            val body = json.encodeToString(StartSessionRequest(plate, zoneId))
            conn.outputStream.bufferedWriter().use { it.write(body) }
            check(conn.responseCode in 200..299) { "HTTP ${conn.responseCode}" }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<StartSessionResponse>(text)
        } finally {
            conn.disconnect()
        }
    }

private val UUID_REGEX = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE)

/**
 * Response from `GET /api/parking/active?plate=…`. Two shapes:
 *
 * 1. Active session for plate:
 *    `{sessionId, plate, zoneId, startTime, elapsedMinutes, status: "active"}`
 *
 * 2. No active session:
 *    `{session: null, status: "no_active_session"}`
 *
 * All session fields are nullable so a single class covers both. Caller
 * should check `status == "active"` before reading the session fields.
 */
@Serializable
data class ActiveSessionResponse(
    val sessionId: String? = null,
    val plate: String? = null,
    val zoneId: String? = null,
    val startTime: String? = null,
    val elapsedMinutes: Int? = null,
    val status: String,
)

suspend fun fetchActiveSession(plate: String): ActiveSessionResponse =
    withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(plate, "UTF-8")
        val conn = URL("$BASE_URL/api/parking/active?plate=$encoded").openConnection() as HttpURLConnection
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        try {
            check(conn.responseCode in 200..299) { "HTTP ${conn.responseCode}" }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<ActiveSessionResponse>(text)
        } finally {
            conn.disconnect()
        }
    }

suspend fun stopParkingSession(sessionId: String): StopSessionResponse =
    withContext(Dispatchers.IO) {
        require(UUID_REGEX.matches(sessionId)) { "Invalid session ID format" }
        val conn =
            URL("$BASE_URL/api/parking/stop/$sessionId").openConnection() as HttpURLConnection
        conn.requestMethod = "DELETE"
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        try {
            check(conn.responseCode in 200..299) { "HTTP ${conn.responseCode}" }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<StopSessionResponse>(text)
        } finally {
            conn.disconnect()
        }
    }
