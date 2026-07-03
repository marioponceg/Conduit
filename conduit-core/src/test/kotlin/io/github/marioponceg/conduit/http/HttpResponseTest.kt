package io.github.marioponceg.conduit.http

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HttpResponseTest {

    @Test
    fun `exposes status code, headers and body`() {
        val body = """{"id":1}""".encodeToByteArray()

        val response = HttpResponse(
            code = 200,
            headers = Headers.of("Content-Type" to "application/json"),
            body = body,
        )

        assertEquals(200, response.code)
        assertEquals("application/json", response.headers["Content-Type"])
        assertContentEquals(body, response.body)
    }

    @Test
    fun `isSuccessful is true exactly for 2xx codes`() {
        assertTrue(HttpResponse(code = 200).isSuccessful)
        assertTrue(HttpResponse(code = 204).isSuccessful)
        assertTrue(HttpResponse(code = 299).isSuccessful)
        assertFalse(HttpResponse(code = 199).isSuccessful)
        assertFalse(HttpResponse(code = 301).isSuccessful)
        assertFalse(HttpResponse(code = 404).isSuccessful)
        assertFalse(HttpResponse(code = 500).isSuccessful)
    }
}
