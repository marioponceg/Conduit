package io.github.marioponceg.conduit.engine.okhttp

import io.github.marioponceg.conduit.http.Headers
import io.github.marioponceg.conduit.http.HttpMethod
import io.github.marioponceg.conduit.http.HttpRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OkHttpEngineTest {

    private lateinit var server: MockWebServer
    private val engine = OkHttpEngine()

    @BeforeTest
    fun startServer() {
        server = MockWebServer()
        server.start()
    }

    @AfterTest
    fun stopServer() {
        server.shutdown()
    }

    @Test
    fun `executes a GET and returns status code and body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":1}"""))

        val response = engine.execute(HttpRequest(url = server.url("/users/1").toString()))

        assertEquals(200, response.code)
        assertEquals("""{"id":1}""", response.body?.decodeToString())
        assertEquals("GET", server.takeRequest().method)
    }

    @Test
    fun `sends method, headers and body to the server as-is`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201))

        engine.execute(
            HttpRequest(
                url = server.url("/users").toString(),
                method = HttpMethod.POST,
                headers = Headers.of(
                    "Content-Type" to "application/json",
                    "X-Trace" to "abc",
                ),
                body = """{"name":"mario"}""".encodeToByteArray(),
            ),
        )

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("application/json", recorded.getHeader("Content-Type"))
        assertEquals("abc", recorded.getHeader("X-Trace"))
        assertEquals("""{"name":"mario"}""", recorded.body.readUtf8())
    }

    @Test
    fun `maps response headers back including repeated ones`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .addHeader("Set-Cookie", "a=1")
                .addHeader("Set-Cookie", "b=2"),
        )

        val response = engine.execute(HttpRequest(url = server.url("/").toString()))

        assertEquals("application/json", response.headers["Content-Type"])
        assertEquals(listOf("a=1", "b=2"), response.headers.values("Set-Cookie"))
    }

    @Test
    fun `returns non-2xx responses instead of throwing`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("boom"))

        val response = engine.execute(HttpRequest(url = server.url("/").toString()))

        assertEquals(500, response.code)
        assertEquals("boom", response.body?.decodeToString())
    }

    @Test
    fun `throws IOException when the connection fails`() = runTest {
        val url = server.url("/").toString()
        server.shutdown()

        assertFailsWith<IOException> {
            engine.execute(HttpRequest(url = url))
        }
    }

    @Test
    fun `cancelling the coroutine cancels the in-flight call`() = runBlocking {
        val client = OkHttpClient()
        val engine = OkHttpEngine(client)
        server.enqueue(MockResponse().setHeadersDelay(3, TimeUnit.SECONDS))
        val job = launch(Dispatchers.IO) {
            engine.execute(HttpRequest(url = server.url("/").toString()))
        }
        server.takeRequest() // suspends until the request is actually on the wire

        job.cancelAndJoin()

        assertTrue(job.isCancelled)
        // Without structured cancellation the call would keep running for the full 10s delay.
        val deadline = System.currentTimeMillis() + 2_000
        while (client.dispatcher.runningCallsCount() > 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
        }
        assertEquals(0, client.dispatcher.runningCallsCount(), "the HTTP call outlived its coroutine")
    }
}
