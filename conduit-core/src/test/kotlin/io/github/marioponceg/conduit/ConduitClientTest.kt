package io.github.marioponceg.conduit

import io.github.marioponceg.conduit.engine.ConduitEngine
import io.github.marioponceg.conduit.http.Headers
import io.github.marioponceg.conduit.http.HttpRequest
import io.github.marioponceg.conduit.http.HttpResponse
import io.github.marioponceg.conduit.interceptor.ConduitInterceptor
import io.github.marioponceg.conduit.result.ConduitResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ConduitClientTest {

    @Test
    fun `building a client without an engine fails with a clear message`() {
        val failure = assertFailsWith<IllegalStateException> {
            conduit { }
        }

        assertTrue(failure.message!!.contains("engine"))
    }

    @Test
    fun `a 2xx response maps to Success carrying the response`() = runTest {
        val client = conduit {
            engine = ConduitEngine { HttpResponse(code = 200, body = "ok".encodeToByteArray()) }
        }

        val result = client.execute(HttpRequest(url = "https://api.example.com/users"))

        val success = result as ConduitResult.Success
        assertEquals(200, success.value.code)
        assertEquals("ok", success.value.body?.decodeToString())
    }

    @Test
    fun `a non-2xx response maps to Failure_Http with code and decoded body`() = runTest {
        val client = conduit {
            engine = ConduitEngine { HttpResponse(code = 404, body = "not found".encodeToByteArray()) }
        }

        val result = client.execute(HttpRequest(url = "https://api.example.com/users/9"))

        val failure = result as ConduitResult.Failure.Http
        assertEquals(404, failure.code)
        assertEquals("not found", failure.body)
    }

    @Test
    fun `a transport failure maps to Failure_Network with its cause`() = runTest {
        val cause = IOException("connection reset")
        val client = conduit {
            engine = ConduitEngine { throw cause }
        }

        val result = client.execute(HttpRequest(url = "https://api.example.com/users"))

        assertEquals(cause, (result as ConduitResult.Failure.Network).cause)
    }

    @Test
    fun `cancellation propagates instead of becoming a Failure`() = runTest {
        val client = conduit {
            engine = ConduitEngine { throw CancellationException("cancelled") }
        }

        assertFailsWith<CancellationException> {
            client.execute(HttpRequest(url = "https://api.example.com/users"))
        }
    }

    @Test
    fun `interceptors configured in the DSL run in the pipeline`() = runTest {
        val client = conduit {
            engine = ConduitEngine { received ->
                HttpResponse(code = if (received.headers["X-Auth"] == "yes") 200 else 401)
            }
            interceptors += ConduitInterceptor { chain ->
                chain.proceed(
                    HttpRequest(url = chain.request.url, headers = Headers.of("X-Auth" to "yes")),
                )
            }
        }

        val result = client.execute(HttpRequest(url = "https://api.example.com/users"))

        assertTrue(result is ConduitResult.Success)
    }

    @Test
    fun `relative urls resolve against baseUrl normalizing slashes`() = runTest {
        val seen = mutableListOf<String>()
        val client = conduit {
            engine = ConduitEngine { received ->
                seen += received.url
                HttpResponse(code = 200)
            }
            baseUrl = "https://api.example.com/v1/"
        }

        client.execute(HttpRequest(url = "/users"))
        client.execute(HttpRequest(url = "users/7"))

        assertEquals(
            listOf("https://api.example.com/v1/users", "https://api.example.com/v1/users/7"),
            seen,
        )
    }

    @Test
    fun `absolute urls bypass baseUrl`() = runTest {
        var seen: String? = null
        val client = conduit {
            engine = ConduitEngine { received ->
                seen = received.url
                HttpResponse(code = 200)
            }
            baseUrl = "https://api.example.com/v1"
        }

        client.execute(HttpRequest(url = "https://other.example.org/health"))

        assertEquals("https://other.example.org/health", seen)
    }
}
