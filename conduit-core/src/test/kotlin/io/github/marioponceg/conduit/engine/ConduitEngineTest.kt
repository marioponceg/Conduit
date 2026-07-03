package io.github.marioponceg.conduit.engine

import io.github.marioponceg.conduit.http.HttpRequest
import io.github.marioponceg.conduit.http.HttpResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ConduitEngineTest {

    @Test
    fun `an engine executes a request and returns the raw response`() = runTest {
        val engine = ConduitEngine { request ->
            HttpResponse(code = if (request.url.endsWith("/users")) 200 else 404)
        }

        val response = engine.execute(HttpRequest(url = "https://api.example.com/users"))

        assertEquals(200, response.code)
    }
}
