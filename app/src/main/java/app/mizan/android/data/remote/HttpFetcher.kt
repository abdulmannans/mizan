package app.mizan.android.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Public market endpoints are best effort: retry with backoff, never block the UI thread, and let
 * the caller keep the last good Room row when a source is down.
 */
@Singleton
class HttpFetcher @Inject constructor(private val client: OkHttpClient) {

    suspend fun get(url: String, accept: String = "application/json"): String =
        withContext(Dispatchers.IO) {
            var lastError: IOException? = null
            var backoffMillis = 1_000L
            repeat(MAX_ATTEMPTS) { attempt ->
                try {
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", accept)
                        .header("Accept-Language", "en-IN,en;q=0.9")
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw IOException("HTTP ${response.code} for $url")
                        }
                        return@withContext response.body.string()
                    }
                } catch (error: IOException) {
                    lastError = error
                    if (attempt < MAX_ATTEMPTS - 1) {
                        delay(backoffMillis)
                        backoffMillis *= 2
                    }
                }
            }
            throw lastError ?: IOException("Failed to fetch $url")
        }

    private companion object {
        const val MAX_ATTEMPTS = 3

        // Yahoo rejects requests without a browser-style User-Agent.
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0 Mobile Safari/537.36 Mizan/1.0"
    }
}
