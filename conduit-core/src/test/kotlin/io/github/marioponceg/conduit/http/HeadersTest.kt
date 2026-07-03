package io.github.marioponceg.conduit.http

import kotlin.test.Test
import kotlin.test.assertEquals

class HeadersTest {

    @Test
    fun `get looks up header names case-insensitively`() {
        val headers = Headers.of("Content-Type" to "application/json")

        assertEquals("application/json", headers["content-type"])
        assertEquals("application/json", headers["CONTENT-TYPE"])
    }

    @Test
    fun `repeated headers keep every value while get returns the first`() {
        val headers = Headers.of(
            "Set-Cookie" to "a=1",
            "set-cookie" to "b=2",
            "Accept" to "application/json",
        )

        assertEquals(listOf("a=1", "b=2"), headers.values("SET-COOKIE"))
        assertEquals("a=1", headers["Set-Cookie"])
        assertEquals(setOf("Set-Cookie", "Accept"), headers.names())
    }

    @Test
    fun `of accepts a list of pairs preserving order`() {
        val headers = Headers.of(listOf("Set-Cookie" to "a=1", "Set-Cookie" to "b=2"))

        assertEquals(listOf("a=1", "b=2"), headers.values("Set-Cookie"))
    }
}
