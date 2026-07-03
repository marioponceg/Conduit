package io.github.marioponceg.conduit.interceptor

import io.github.marioponceg.conduit.http.HttpRequest
import io.github.marioponceg.conduit.http.HttpResponse

/**
 * Observes, mutates, short-circuits or retries an HTTP exchange, chain-of-responsibility style.
 *
 * Interceptors operate on the raw engine contract — [HttpResponse] for any status code,
 * `IOException` flowing through for transport failures — so a retry interceptor can catch and
 * re-[proceed][Chain.proceed], and a cache interceptor can return without proceeding at all.
 * The mapping to `ConduitResult` happens once, after the whole pipeline.
 */
public fun interface ConduitInterceptor {

    /**
     * Handles the exchange represented by [chain]: inspect [Chain.request], call
     * [Chain.proceed] zero or more times, and return the response for this exchange.
     */
    public suspend fun intercept(chain: Chain): HttpResponse

    /** The interceptor's view of the in-flight exchange. */
    public interface Chain {

        /** The request as it arrives at this interceptor. */
        public val request: HttpRequest

        /** Passes [request] down the chain and returns the resulting response. */
        public suspend fun proceed(request: HttpRequest): HttpResponse
    }
}
