package io.github.marioponceg.conduit.result

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConduitResultTest {

    @Test
    fun `success exposes the wrapped value`() {
        val result: ConduitResult<String> = ConduitResult.Success("body")

        assertEquals("body", (result as ConduitResult.Success).value)
    }

    @Test
    fun `http failure exposes status code and error body`() {
        val result: ConduitResult<String> = ConduitResult.Failure.Http(code = 404, body = "not found")

        val failure = result as ConduitResult.Failure.Http
        assertEquals(404, failure.code)
        assertEquals("not found", failure.body)
    }

    @Test
    fun `network failure exposes the io exception that caused it`() {
        val cause = java.io.IOException("connection reset")

        val result: ConduitResult<String> = ConduitResult.Failure.Network(cause)

        assertEquals(cause, (result as ConduitResult.Failure.Network).cause)
    }

    @Test
    fun `serialization failure exposes the exception that caused it`() {
        val cause = IllegalArgumentException("malformed json")

        val result: ConduitResult<String> = ConduitResult.Failure.Serialization(cause)

        assertEquals(cause, (result as ConduitResult.Failure.Serialization).cause)
    }

    @Test
    fun `getOrNull returns the value on success`() {
        val result: ConduitResult<String> = ConduitResult.Success("body")

        assertEquals("body", result.getOrNull())
    }

    @Test
    fun `getOrNull returns null on failure`() {
        val result: ConduitResult<String> = ConduitResult.Failure.Http(code = 500, body = null)

        assertNull(result.getOrNull())
    }

    @Test
    fun `failureOrNull returns the typed failure regardless of its kind`() {
        val failure = ConduitResult.Failure.Network(java.io.IOException("timeout"))
        val result: ConduitResult<String> = failure

        assertEquals(failure, result.failureOrNull())
    }

    @Test
    fun `failureOrNull returns null on success`() {
        val result: ConduitResult<String> = ConduitResult.Success("body")

        assertNull(result.failureOrNull())
    }
}
