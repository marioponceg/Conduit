package io.github.marioponceg.conduit.http

/**
 * An immutable multimap of HTTP headers. Name lookups are case-insensitive per RFC 9110;
 * the original casing and entry order are preserved for the wire.
 */
public class Headers private constructor(
    private val entries: List<Pair<String, String>>,
) {

    /** Returns the value of the first header named [name] (case-insensitive), or `null`. */
    public operator fun get(name: String): String? =
        entries.firstOrNull { it.first.equals(name, ignoreCase = true) }?.second

    /** Returns every value of the headers named [name] (case-insensitive), in entry order. */
    public fun values(name: String): List<String> =
        entries.filter { it.first.equals(name, ignoreCase = true) }.map { it.second }

    /** Returns the distinct header names, keeping the casing of each name's first occurrence. */
    public fun names(): Set<String> =
        entries.map { it.first }.distinctBy { it.lowercase() }.toSet()

    /** Returns a copy with [header] appended after the existing entries. */
    public operator fun plus(header: Pair<String, String>): Headers = Headers(entries + header)

    public companion object {
        /** Creates headers from name-value [pairs], keeping their order. */
        public fun of(vararg pairs: Pair<String, String>): Headers = Headers(pairs.toList())

        /** Creates headers from a list of name-value [pairs], keeping their order. */
        public fun of(pairs: List<Pair<String, String>>): Headers = Headers(pairs.toList())
    }
}
