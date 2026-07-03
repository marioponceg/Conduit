package io.github.marioponceg.conduit.http

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HttpRequestTest {

    @Test
    fun `defaults to a GET with no headers and no body`() {
        val request = HttpRequest(url = "https://api.example.com/users")

        assertEquals(HttpMethod.GET, request.method)
        assertEquals("https://api.example.com/users", request.url)
        assertEquals(emptySet(), request.headers.names())
        assertNull(request.body)
    }

    @Test
    fun `carries method, headers and body when provided`() {
        val body = """{"name":"mario"}""".encodeToByteArray()

        val request = HttpRequest(
            url = "https://api.example.com/users",
            method = HttpMethod.POST,
            headers = Headers.of("Content-Type" to "application/json"),
            body = body,
        )

        assertEquals(HttpMethod.POST, request.method)
        assertEquals("application/json", request.headers["Content-Type"])
        assertContentEquals(body, request.body)
    }
}
