package io.github.marioponceg.conduit.engine

import io.github.marioponceg.conduit.http.HttpRequest
import io.github.marioponceg.conduit.http.HttpResponse
import java.io.IOException

/**
 * The seam between Conduit and an actual HTTP client. Implementations (e.g. the OkHttp engine)
 * only bridge the transport; they apply no Conduit policy.
 *
 * The contract is deliberately raw so that error mapping lives in core, uniformly for every
 * engine: implementations must return the [HttpResponse] for **any** HTTP status code, and
 * throw only [IOException] (transport failures) or [kotlinx.coroutines.CancellationException].
 * Core turns those into the public [io.github.marioponceg.conduit.result.ConduitResult].
 *
 * A `fun interface` so tests and callers can stub an engine with a single lambda.
 */
public fun interface ConduitEngine {

    /**
     * Executes [request] and returns its response, whatever the status code.
     *
     * @throws IOException if the request could not produce an HTTP response.
     */
    public suspend fun execute(request: HttpRequest): HttpResponse
}
