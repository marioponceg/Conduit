package io.github.marioponceg.conduit.http

/**
 * An HTTP request method.
 *
 * A value class rather than an enum so non-standard methods remain expressible; names are
 * normalized to uppercase at construction, so equality is case-insensitive by design.
 */
@JvmInline
public value class HttpMethod private constructor(public val value: String) {

    public companion object {
        /** Creates a method from [value], normalizing it to its uppercase canonical form. */
        public operator fun invoke(value: String): HttpMethod = HttpMethod(value.uppercase())

        public val GET: HttpMethod = HttpMethod("GET")
        public val POST: HttpMethod = HttpMethod("POST")
        public val PUT: HttpMethod = HttpMethod("PUT")
        public val PATCH: HttpMethod = HttpMethod("PATCH")
        public val DELETE: HttpMethod = HttpMethod("DELETE")
        public val HEAD: HttpMethod = HttpMethod("HEAD")
        public val OPTIONS: HttpMethod = HttpMethod("OPTIONS")
    }
}
