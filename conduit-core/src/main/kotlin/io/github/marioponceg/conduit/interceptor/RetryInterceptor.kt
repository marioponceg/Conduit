package io.github.marioponceg.conduit.interceptor

import io.github.marioponceg.conduit.http.HttpMethod
import io.github.marioponceg.conduit.http.HttpResponse
import kotlinx.coroutines.delay
import java.io.IOException
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Retries transient failures with exponential backoff and jitter.
 *
 * A retry is attempted when the engine throws an [IOException] or the response status is in
 * [retryableStatusCodes] (transient-by-nature codes by default — a 404 is never transient).
 * When attempts run out, the last response is returned (or the last transport failure
 * rethrown) so the client's normal mapping applies.
 *
 * Non-idempotent methods (POST, PATCH, …) are **not** retried unless [retryNonIdempotent] is
 * explicitly enabled: replaying them can duplicate side effects. Waits use exponential backoff
 * — `initialDelay × 2^(attempt-1)`, capped at [maxDelay] — with jitter in `[50%, 100%]` of the
 * computed wait to avoid synchronized client stampedes. Waiting suspends via [delay], so it is
 * cancellable and runs on virtual time in tests.
 */
public class RetryInterceptor(
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val retryableStatusCodes: Set<Int> = DEFAULT_RETRYABLE_STATUS_CODES,
    private val retryNonIdempotent: Boolean = false,
    private val initialDelay: Duration = DEFAULT_INITIAL_DELAY,
    private val maxDelay: Duration = DEFAULT_MAX_DELAY,
    private val random: Random = Random,
) : ConduitInterceptor {

    override suspend fun intercept(chain: ConduitInterceptor.Chain): HttpResponse {
        if (!retryNonIdempotent && chain.request.method !in IDEMPOTENT_METHODS) {
            return chain.proceed(chain.request)
        }
        var attempt = 1
        while (true) {
            try {
                val response = chain.proceed(chain.request)
                if (response.code !in retryableStatusCodes || attempt == maxAttempts) {
                    return response
                }
            } catch (e: IOException) {
                if (attempt == maxAttempts) throw e
            }
            delay(backoffFor(attempt))
            attempt++
        }
    }

    private fun backoffFor(attempt: Int): Duration {
        val base = (initialDelay * (1 shl (attempt - 1))).coerceAtMost(maxDelay)
        val jitterFactor = JITTER_FLOOR + random.nextDouble() * (1.0 - JITTER_FLOOR)
        return base * jitterFactor
    }

    public companion object {
        private const val DEFAULT_MAX_ATTEMPTS = 3
        private const val JITTER_FLOOR = 0.5
        private val DEFAULT_INITIAL_DELAY = 200.milliseconds
        private val DEFAULT_MAX_DELAY = 10.seconds

        /** Transient-by-nature statuses: timeout, rate limit, and gateway-style 5xx. */
        public val DEFAULT_RETRYABLE_STATUS_CODES: Set<Int> = setOf(408, 429, 500, 502, 503, 504)

        private val IDEMPOTENT_METHODS = setOf(
            HttpMethod.GET,
            HttpMethod.HEAD,
            HttpMethod.PUT,
            HttpMethod.DELETE,
            HttpMethod.OPTIONS,
        )
    }
}
