package io.github.marioponceg.conduit.serialization.kotlinx

import io.github.marioponceg.conduit.conduit
import io.github.marioponceg.conduit.engine.ConduitEngine
import io.github.marioponceg.conduit.execute
import io.github.marioponceg.conduit.http.HttpRequest
import io.github.marioponceg.conduit.http.HttpResponse
import io.github.marioponceg.conduit.result.ConduitResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinxJsonConverterTest {

    @Serializable
    data class User(val id: Int, val name: String)

    private val converter = KotlinxJsonConverter()

    @Test
    fun `decodes a serializable class from json bytes`() {
        val decoded = converter.decode("""{"id":1,"name":"mario"}""".encodeToByteArray(), typeOf<User>())

        assertEquals(User(1, "mario"), decoded)
    }

    @Test
    fun `decodes generic types like lists`() {
        val decoded = converter.decode(
            """[{"id":1,"name":"mario"},{"id":2,"name":"ana"}]""".encodeToByteArray(),
            typeOf<List<User>>(),
        )

        assertEquals(listOf(User(1, "mario"), User(2, "ana")), decoded)
    }

    @Test
    fun `encodes a serializable class to json bytes with json content type`() {
        val encoded = converter.encode(User(1, "mario"), typeOf<User>())

        assertEquals("""{"id":1,"name":"mario"}""", encoded.decodeToString())
        assertEquals("application/json", converter.contentType)
    }

    @Test
    fun `malformed json surfaces as Failure_Serialization through the client`() = runTest {
        val client = conduit {
            engine = ConduitEngine { HttpResponse(code = 200, body = "not json".encodeToByteArray()) }
            converter = this@KotlinxJsonConverterTest.converter
        }

        val result = client.execute<User>(HttpRequest(url = "https://x/users/1"))

        val failure = result as ConduitResult.Failure.Serialization
        assertTrue(failure.cause is SerializationException)
    }
}
