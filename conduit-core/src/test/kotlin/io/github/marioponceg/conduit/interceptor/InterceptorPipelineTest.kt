package io.github.marioponceg.conduit.interceptor

import io.github.marioponceg.conduit.engine.ConduitEngine
import io.github.marioponceg.conduit.http.Headers
import io.github.marioponceg.conduit.http.HttpRequest
import io.github.marioponceg.conduit.http.HttpResponse
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

class InterceptorPipelineTest {

    private val request = HttpRequest(url = "https://api.example.com/users")

    @Test
    fun `with no interceptors it delegates to the engine untouched`() = runTest {
        val engine = ConduitEngine { HttpResponse(code = 200) }

        val pipeline = InterceptorPipeline(interceptors = emptyList(), engine = engine)

        assertEquals(200, pipeline.execute(request).code)
    }

    @Test
    fun `an interceptor can mutate the request before it reaches the engine`() = runTest {
        val engine = ConduitEngine { received ->
            HttpResponse(code = if (received.headers["Authorization"] == "Bearer token") 200 else 401)
        }
        val auth = ConduitInterceptor { chain ->
            chain.proceed(
                HttpRequest(
                    url = chain.request.url,
                    method = chain.request.method,
                    headers = Headers.of("Authorization" to "Bearer token"),
                    body = chain.request.body,
                ),
            )
        }

        val response = InterceptorPipeline(listOf(auth), engine).execute(request)

        assertEquals(200, response.code)
    }

    @Test
    fun `interceptors run in list order around the engine`() = runTest {
        val events = mutableListOf<String>()
        val engine = ConduitEngine {
            events += "engine"
            HttpResponse(code = 200)
        }
        val first = ConduitInterceptor { chain ->
            events += "first-in"
            chain.proceed(chain.request).also { events += "first-out" }
        }
        val second = ConduitInterceptor { chain ->
            events += "second-in"
            chain.proceed(chain.request).also { events += "second-out" }
        }

        InterceptorPipeline(listOf(first, second), engine).execute(request)

        assertEquals(
            listOf("first-in", "second-in", "engine", "second-out", "first-out"),
            events,
        )
    }

    @Test
    fun `an interceptor can short-circuit without reaching the engine`() = runTest {
        var engineCalls = 0
        val engine = ConduitEngine {
            engineCalls++
            HttpResponse(code = 200)
        }
        val cache = ConduitInterceptor { HttpResponse(code = 304) }

        val response = InterceptorPipeline(listOf(cache), engine).execute(request)

        assertEquals(304, response.code)
        assertEquals(0, engineCalls)
    }

    @Test
    fun `an interceptor can retry by proceeding again after an IOException`() = runTest {
        var attempts = 0
        val engine = ConduitEngine {
            attempts++
            if (attempts == 1) throw IOException("connection reset") else HttpResponse(code = 200)
        }
        val retry = ConduitInterceptor { chain ->
            try {
                chain.proceed(chain.request)
            } catch (_: IOException) {
                chain.proceed(chain.request)
            }
        }

        val response = InterceptorPipeline(listOf(retry), engine).execute(request)

        assertEquals(200, response.code)
        assertEquals(2, attempts)
    }
}
