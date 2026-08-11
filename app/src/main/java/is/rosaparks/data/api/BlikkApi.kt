package `is`.rosaparks.data.api

import `is`.rosaparks.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * Client for the worker's Blikk payment probe (`/api/blikk/test-payment`).
 *
 * Deliberately thin and deliberately debug-only: this exercises the Blikk Ecom
 * rail end to end from a real phone, it is not the top-up feature. The
 * architecture question of whose account the money lands in is still open
 * (docs/greidslugatt/DECISION.md §3.1), so nothing here builds a balance, a
 * ledger, or any persistence.
 *
 * No payer identifiers are sent. Payments are anonymous and the payer
 * identifies on Blikk's own hosted page, so no kennitala passes through the
 * app or the worker.
 */

private val BASE_URL = BuildConfig.API_BASE_URL
private const val TIMEOUT_MS = 15_000

private val json = Json { ignoreUnknownKeys = true }

/**
 * Pre-registered payer details, standing in for what a real signup flow would
 * hold on file. [bban] empty means "identified only": Blikk is told who is
 * paying but the payment still goes through the ordinary create, which is the
 * furthest we can get until Blikk enables Direct Debtor on the sales channel
 * (both Straumbogi channels answered 403 on 2026-08-09).
 */
@Serializable
data class BlikkDebtor(
    val kennitala: String,
    val bban: String = "",
    val name: String = "",
)

@Serializable
private data class CreatePaymentRequest(
    val amount: Int,
    val debtor: BlikkDebtor? = null,
)

@Serializable
private data class ValidateAccountRequest(
    val kennitala: String,
    val bban: String,
)

/** Response from `POST /api/blikk/validate-account` — a zero-funds check that
 * the account exists and belongs to the kennitala. Moves no money. */
@Serializable
data class BlikkValidationResponse(
    val valid: Boolean = false,
    val corporate: Boolean = false,
    val message: String = "",
    val substituteExternalId: String = "",
)

/** Response from `POST /api/blikk/test-payment`. */
@Serializable
data class BlikkPaymentResponse(
    /** "anonymous", "identified" or "direct-debtor" — which path actually ran. */
    val mode: String = "",
    val id: String,
    val status: String,
    /** Hosted payment page. Empty for banks doing back-channel push SCA (e.g.
     * Íslandsbanki) — then there is nothing to open and the payer gets a push
     * from their own banking app instead. */
    val scaRedirectUrl: String = "",
    val message: String = "",
    val amount: Int = 0,
    val expiresInSeconds: Int = 0,
)

/** Response from `GET /api/blikk/test-payment/{id}`. */
@Serializable
data class BlikkStatusResponse(
    val id: String,
    val status: String,
    val paid: Boolean = false,
    val terminal: Boolean = false,
    val scaRedirectUrl: String = "",
    /** Blikk's human-readable reason, e.g. "greiðsla rann út á tíma". */
    val message: String = "",
)

@Serializable
private data class BlikkErrorResponse(
    val error: String = "unknown",
    /** Blikk's own message, passed through by the worker. A 403 here reads
     * "sales channel does not allow direct debtor payments". */
    val detail: String = "",
)

/**
 * Reads the worker's JSON error body so the screen can show why a call failed
 * instead of a bare status code — the whole point of a probe surface is that
 * failures are legible. `blikk_not_configured` (501) means the worker has no
 * BLIKK_API_KEY bound, which is the expected state of the deployed worker.
 */
private fun HttpURLConnection.errorCode(): String =
    runCatching {
        val body = errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        val parsed = json.decodeFromString<BlikkErrorResponse>(body)
        val reason =
            runCatching {
                // Blikk's detail is itself a JSON document; lift its message.
                Regex("\"detail\":\"([^\"]+)\"").find(parsed.detail)?.groupValues?.get(1)
            }.getOrNull()
        listOfNotNull(parsed.error, reason).joinToString(": ")
    }.getOrDefault("http_$responseCode")

private fun HttpURLConnection.postJson(body: String) {
    requestMethod = "POST"
    setRequestProperty("Content-Type", "application/json")
    connectTimeout = TIMEOUT_MS
    readTimeout = TIMEOUT_MS
    doOutput = true
    outputStream.bufferedWriter().use { it.write(body) }
}

suspend fun createBlikkTestPayment(
    amountKr: Int,
    debtor: BlikkDebtor? = null,
): BlikkPaymentResponse =
    withContext(Dispatchers.IO) {
        val conn = URL("$BASE_URL/api/blikk/test-payment").openConnection() as HttpURLConnection
        try {
            conn.postJson(json.encodeToString(CreatePaymentRequest(amountKr, debtor)))
            check(conn.responseCode in 200..299) { conn.errorCode() }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<BlikkPaymentResponse>(text)
        } finally {
            conn.disconnect()
        }
    }

suspend fun validateBlikkAccount(
    kennitala: String,
    bban: String,
): BlikkValidationResponse =
    withContext(Dispatchers.IO) {
        val conn = URL("$BASE_URL/api/blikk/validate-account").openConnection() as HttpURLConnection
        try {
            conn.postJson(json.encodeToString(ValidateAccountRequest(kennitala, bban)))
            check(conn.responseCode in 200..299) { conn.errorCode() }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<BlikkValidationResponse>(text)
        } finally {
            conn.disconnect()
        }
    }

suspend fun fetchBlikkPaymentStatus(paymentId: String): BlikkStatusResponse =
    withContext(Dispatchers.IO) {
        val conn =
            URL("$BASE_URL/api/blikk/test-payment/$paymentId").openConnection() as HttpURLConnection
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        try {
            check(conn.responseCode in 200..299) { conn.errorCode() }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<BlikkStatusResponse>(text)
        } finally {
            conn.disconnect()
        }
    }
