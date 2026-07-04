package io.github.marioponceg.conduit.interceptor

import io.github.marioponceg.conduit.engine.ConduitEngine
import io.github.marioponceg.conduit.http.HttpMethod
import io.github.marioponceg.conduit.http.HttpRequest
import io.github.marioponceg.conduit.http.HttpResponse
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class RetryInterceptorTest {

    private val request = HttpRequest(url = "https://api.example.com/users")

    private fun engineOf(vararg outcomes: Any): Pair<ConduitEngine, () -> Int> {
        var calls = 0
        val engine = ConduitEngine {
            val outcome = outcomes[minOf(calls, outcomes.lastIndex)]
            calls++
            when (outcome) {
                is HttpResponse -> outcome
                is IOException -> throw outcome
                else -> error("unexpected outcome $outcome")
            }
        }
        return engine to { calls }
    }

    @Test
    fun `retries a transport failure until it succeeds`() = runTest {
        val (engine, calls) = engineOf(IOException("reset"), HttpResponse(code = 200))
        val pipeline = InterceptorPipeline(listOf(RetryInterceptor()), engine)

        val response = pipeline.execute(request)

        assertEquals(200, response.code)
        assertEquals(2, calls())
    }

    @Test
    fun `retries retryable status codes until success`() = runTest {
        val (engine, calls) = engineOf(HttpResponse(code = 503), HttpResponse(code = 200))
        val pipeline = InterceptorPipeline(listOf(RetryInterceptor()), engine)

        val response = pipeline.execute(request)

        assertEquals(200, response.code)
        assertEquals(2, calls())
    }

    @Test
    fun `does not retry non-transient statuses`() = runTest {
        val (engine, calls) = engineOf(HttpResponse(code = 404), HttpResponse(code = 200))
        val pipeline = InterceptorPipeline(listOf(RetryInterceptor()), engine)

        val response = pipeline.execute(request)

        assertEquals(404, response.code)
        assertEquals(1, calls())
    }

    @Test
    fun `returns the last response when attempts are exhausted`() = runTest {
        val (engine, calls) = engineOf(HttpResponse(code = 503))
        val pipeline = InterceptorPipeline(listOf(RetryInterceptor(maxAttempts = 3)), engine)

        val response = pipeline.execute(request)

        assertEquals(503, response.code)
        assertEquals(3, calls())
    }

    @Test
    fun `rethrows the last transport failure when attempts are exhausted`() = runTest {
        val (engine, calls) = engineOf(IOException("reset"))
        val pipeline = InterceptorPipeline(listOf(RetryInterceptor(maxAttempts = 3)), engine)

        assertFailsWith<IOException> { pipeline.execute(request) }
        assertEquals(3, calls())
    }

    @Test
    fun `does not retry non-idempotent methods by default`() = runTest {
        val (engine, calls) = engineOf(HttpResponse(code = 503), HttpResponse(code = 200))
        val pipeline = InterceptorPipeline(listOf(RetryInterceptor()), engine)

        val response = pipeline.execute(
            HttpRequest(url = request.url, method = HttpMethod.POST),
        )

        assertEquals(503, response.code)
        assertEquals(1, calls())
    }

    @Test
    fun `retries non-idempotent methods when explicitly opted in`() = runTest {
        val (engine, calls) = engineOf(HttpResponse(code = 503), HttpResponse(code = 200))
        val pipeline = InterceptorPipeline(
            listOf(RetryInterceptor(retryNonIdempotent = true)),
            engine,
        )

        val response = pipeline.execute(
            HttpRequest(url = request.url, method = HttpMethod.POST),
        )

        assertEquals(200, response.code)
        assertEquals(2, calls())
    }

    @Test
    fun `waits with exponential backoff and jitter between attempts`() = runTest {
        val (engine, _) = engineOf(HttpResponse(code = 503))
        val pipeline = InterceptorPipeline(
            listOf(
                RetryInterceptor(
                    maxAttempts = 3,
                    initialDelay = 200.milliseconds,
                    maxDelay = 10.seconds,
                ),
            ),
            engine,
        )

        pipeline.execute(request)

        // Two waits: attempt 1 -> base 200ms, attempt 2 -> base 400ms; jitter keeps each
        // in [base/2, base], so total virtual time must land in [300, 600] ms.
        assertTrue(currentTime in 300..600, "total backoff was ${currentTime}ms")
    }

    @Test
    fun `caps each backoff wait at maxDelay`() = runTest {
        val (engine, _) = engineOf(HttpResponse(code = 503))
        val pipeline = InterceptorPipeline(
            listOf(
                RetryInterceptor(
                    maxAttempts = 4,
                    initialDelay = 1.seconds,
                    maxDelay = 1.seconds,
                ),
            ),
            engine,
        )

        pipeline.execute(request)

        // Three waits, each capped at 1s before jitter: total in [1500, 3000] ms.
        assertTrue(currentTime in 1500..3000, "total backoff was ${currentTime}ms")
    }
}
