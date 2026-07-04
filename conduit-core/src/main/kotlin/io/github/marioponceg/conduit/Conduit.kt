package io.github.marioponceg.conduit

import io.github.marioponceg.conduit.engine.ConduitEngine
import io.github.marioponceg.conduit.interceptor.ConduitInterceptor

/**
 * Builds a [ConduitClient]:
 *
 * ```
 * val client = conduit {
 *     engine = OkHttpEngine()
 *     baseUrl = "https://api.example.com"
 *     interceptors += authInterceptor
 * }
 * ```
 */
public fun conduit(configure: ConduitBuilder.() -> Unit): ConduitClient =
    ConduitBuilder().apply(configure).build()

/** Mutable configuration collected by [conduit]; [engine] is the only required piece. */
public class ConduitBuilder internal constructor() {

    /** The transport this client runs on, e.g. `OkHttpEngine()`. Required. */
    public var engine: ConduitEngine? = null

    /** Base URL that requests with relative URLs are resolved against. */
    public var baseUrl: String? = null

    /** Interceptors, run in list order around the engine. */
    public val interceptors: MutableList<ConduitInterceptor> = mutableListOf()

    internal fun build(): ConduitClient {
        val engine = checkNotNull(engine) {
            "Conduit needs an engine: set `engine = ...` (e.g. OkHttpEngine from conduit-engine-okhttp)"
        }
        return ConduitClient(engine = engine, baseUrl = baseUrl, interceptors = interceptors.toList())
    }
}
