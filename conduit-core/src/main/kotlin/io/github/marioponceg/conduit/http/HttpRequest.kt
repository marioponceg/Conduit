package io.github.marioponceg.conduit.http

/**
 * An immutable HTTP request, engine-agnostic.
 *
 * Deliberately not a `data` class: [body] is a `ByteArray`, whose reference-based `equals`
 * would make generated structural equality misleading. Requests are compared by identity,
 * like the requests of the HTTP clients Conduit abstracts over.
 */
public class HttpRequest(
    public val url: String,
    public val method: HttpMethod = HttpMethod.GET,
    public val headers: Headers = Headers.of(),
    public val body: ByteArray? = null,
)
