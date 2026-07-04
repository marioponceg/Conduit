package io.github.marioponceg.conduit.serialization.kotlinx

import io.github.marioponceg.conduit.converter.BodyConverter
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KType

/**
 * [BodyConverter] backed by kotlinx.serialization's JSON format.
 *
 * @param json the configuration to use; pass a custom instance to tweak behavior
 * (e.g. `Json { ignoreUnknownKeys = true }`).
 */
public class KotlinxJsonConverter(
    private val json: Json = Json,
) : BodyConverter {

    override val contentType: String = "application/json"

    override fun decode(bytes: ByteArray, type: KType): Any? =
        json.decodeFromString(json.serializersModule.serializer(type), bytes.decodeToString())

    override fun encode(value: Any?, type: KType): ByteArray =
        json.encodeToString(json.serializersModule.serializer(type), value).encodeToByteArray()
}
