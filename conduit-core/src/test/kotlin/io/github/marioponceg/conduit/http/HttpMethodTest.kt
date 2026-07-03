package io.github.marioponceg.conduit.http

import kotlin.test.Test
import kotlin.test.assertEquals

class HttpMethodTest {

    @Test
    fun `standard methods expose their canonical name`() {
        assertEquals("GET", HttpMethod.GET.value)
        assertEquals("POST", HttpMethod.POST.value)
        assertEquals("PUT", HttpMethod.PUT.value)
        assertEquals("PATCH", HttpMethod.PATCH.value)
        assertEquals("DELETE", HttpMethod.DELETE.value)
        assertEquals("HEAD", HttpMethod.HEAD.value)
        assertEquals("OPTIONS", HttpMethod.OPTIONS.value)
    }

    @Test
    fun `custom methods are normalized to uppercase`() {
        assertEquals(HttpMethod.GET, HttpMethod("get"))
    }
}
