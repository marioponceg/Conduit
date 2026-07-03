package io.github.marioponceg.conduit.http

/**
 * An immutable HTTP response, engine-agnostic. Like [HttpRequest], deliberately not a
 * `data` class because of the `ByteArray` body.
 */
public class HttpResponse(
    public val code: Int,
    public val headers: Headers = Headers.of(),
    public val body: ByteArray? = null,
) {

    /** True exactly when [code] is in the 2xx range. */
    public val isSuccessful: Boolean get() = code in SUCCESSFUL_RANGE

    private companion object {
        private val SUCCESSFUL_RANGE = 200..299
    }
}
