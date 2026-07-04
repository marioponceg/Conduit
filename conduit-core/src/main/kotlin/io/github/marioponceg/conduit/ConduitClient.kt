package io.github.marioponceg.conduit

import io.github.marioponceg.conduit.converter.BodyConverter
import io.github.marioponceg.conduit.engine.ConduitEngine
import io.github.marioponceg.conduit.http.HttpRequest
import io.github.marioponceg.conduit.http.HttpResponse
import io.github.marioponceg.conduit.interceptor.ConduitInterceptor
import io.github.marioponceg.conduit.interceptor.InterceptorPipeline
import io.github.marioponceg.conduit.result.ConduitResult
import java.io.IOException

/** A configured Conduit client. Create one with [conduit]. */
public class ConduitClient internal constructor(
    engine: ConduitEngine,
    private val baseUrl: String?,
    interceptors: List<ConduitInterceptor>,
    @PublishedApi internal val converter: BodyConverter?,
) {

    private val pipeline: ConduitEngine = InterceptorPipeline(interceptors, engine)

    /**
     * Executes [request] through the interceptor pipeline and the engine, mapping the raw
     * outcome to a [ConduitResult]. Never throws for HTTP or transport errors; cancellation
     * propagates as usual.
     */
    public suspend fun execute(request: HttpRequest): ConduitResult<HttpResponse> {
        val response = try {
            pipeline.execute(request.resolvedAgainst(baseUrl))
        } catch (e: IOException) {
            // Only transport failures become values; CancellationException is deliberately
            // not caught so structured cancellation keeps working through the client.
            return ConduitResult.Failure.Network(e)
        }
        return if (response.isSuccessful) {
            ConduitResult.Success(response)
        } else {
            ConduitResult.Failure.Http(code = response.code, body = response.body?.decodeToString())
        }
    }
}

private fun HttpRequest.resolvedAgainst(baseUrl: String?): HttpRequest {
    val isAbsolute = url.startsWith("http://") || url.startsWith("https://")
    if (baseUrl == null || isAbsolute) return this
    return HttpRequest(
        url = baseUrl.trimEnd('/') + "/" + url.trimStart('/'),
        method = method,
        headers = headers,
        body = body,
    )
}
