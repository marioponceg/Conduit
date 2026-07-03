package io.github.marioponceg.conduit.result

/**
 * The outcome of a Conduit network call.
 *
 * Errors are modeled as values rather than thrown exceptions, so callers can handle every
 * outcome exhaustively with a `when` expression.
 */
public sealed interface ConduitResult<out T> {

    /** The call completed successfully and produced [value]. */
    public data class Success<out T>(val value: T) : ConduitResult<T>

    /**
     * The call did not produce a usable value. Grouping all error cases under one type lets
     * callers handle "any failure" as a single `when` branch when they don't care about the kind.
     */
    public sealed interface Failure : ConduitResult<Nothing> {

        /** The server answered with a non-2xx status [code]; [body] is the raw error body, if any. */
        public data class Http(val code: Int, val body: String?) : Failure

        /** The call never produced an HTTP response (DNS, connection, timeout, …); see [cause]. */
        public data class Network(val cause: java.io.IOException) : Failure

        /** The response arrived but its body could not be (de)serialized; see [cause]. */
        public data class Serialization(val cause: Throwable) : Failure
    }
}

/** Returns the value if this is a [ConduitResult.Success], or `null` otherwise. */
public fun <T> ConduitResult<T>.getOrNull(): T? = when (this) {
    is ConduitResult.Success -> value
    is ConduitResult.Failure -> null
}

/** Returns this as a [ConduitResult.Failure] of any kind, or `null` if it is a success. */
public fun <T> ConduitResult<T>.failureOrNull(): ConduitResult.Failure? = when (this) {
    is ConduitResult.Success -> null
    is ConduitResult.Failure -> this
}
