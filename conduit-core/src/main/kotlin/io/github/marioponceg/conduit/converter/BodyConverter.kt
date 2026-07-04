package io.github.marioponceg.conduit.converter

import kotlin.reflect.KType

/**
 * The seam between Conduit and a serialization library: converts HTTP bodies to and from
 * user types. Implementations (e.g. the kotlinx.serialization JSON converter) know the wire
 * format; core does not.
 *
 * Conversion failures should throw the implementation's native exceptions — the client maps
 * them to `ConduitResult.Failure.Serialization` at its single mapping point.
 */
public interface BodyConverter {

    /** Media type this converter produces, set as `Content-Type` on encoded request bodies. */
    public val contentType: String

    /** Decodes [bytes] into a value of [type]. */
    public fun decode(bytes: ByteArray, type: KType): Any?

    /** Encodes [value] of static [type] into body bytes. */
    public fun encode(value: Any?, type: KType): ByteArray
}
