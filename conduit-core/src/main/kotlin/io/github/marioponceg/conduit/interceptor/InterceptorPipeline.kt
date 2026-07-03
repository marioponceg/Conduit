package io.github.marioponceg.conduit.interceptor

import io.github.marioponceg.conduit.engine.ConduitEngine
import io.github.marioponceg.conduit.http.HttpRequest
import io.github.marioponceg.conduit.http.HttpResponse

/**
 * A [ConduitEngine] decorator that runs [interceptors] in order around [engine].
 *
 * Being an engine itself keeps the pipeline composable: whatever accepts an engine accepts an
 * intercepted engine, and pipelines can even wrap other pipelines.
 */
public class InterceptorPipeline(
    private val interceptors: List<ConduitInterceptor>,
    private val engine: ConduitEngine,
) : ConduitEngine {

    override suspend fun execute(request: HttpRequest): HttpResponse = proceedFrom(0, request)

    private suspend fun proceedFrom(index: Int, request: HttpRequest): HttpResponse =
        if (index == interceptors.size) {
            engine.execute(request)
        } else {
            interceptors[index].intercept(ChainAt(index, request))
        }

    private inner class ChainAt(
        private val index: Int,
        override val request: HttpRequest,
    ) : ConduitInterceptor.Chain {

        override suspend fun proceed(request: HttpRequest): HttpResponse =
            proceedFrom(index + 1, request)
    }
}
