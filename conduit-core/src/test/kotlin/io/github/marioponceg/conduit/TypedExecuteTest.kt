package io.github.marioponceg.conduit

import io.github.marioponceg.conduit.converter.BodyConverter
import io.github.marioponceg.conduit.engine.ConduitEngine
import io.github.marioponceg.conduit.http.HttpMethod
import io.github.marioponceg.conduit.http.HttpRequest
import io.github.marioponceg.conduit.http.HttpResponse
import io.github.marioponceg.conduit.result.ConduitResult
import kotlinx.coroutines.test.runTest
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TypedExecuteTest {

    data class User(val raw: String)

    /** Fake converter: decodes bytes to User(raw) and encodes any value via toString. */
    private val fakeConverter = object : BodyConverter {
        override val contentType: String = "application/fake"

        override fun decode(bytes: ByteArray, type: KType): Any? {
            check(type == typeOf<User>()) { "unexpected type $type" }
            return User(bytes.decodeToString())
        }

        override fun encode(value: Any?, type: KType): ByteArray =
            value.toString().encodeToByteArray()
    }

    @Test
    fun `typed execute decodes a 2xx body with the configured converter`() = runTest {
        val client = conduit {
            engine = ConduitEngine { HttpResponse(code = 200, body = "mario".encodeToByteArray()) }
            converter = fakeConverter
        }

        val result = client.execute<User>(HttpRequest(url = "https://x/users/1"))

        assertEquals(User("mario"), (result as ConduitResult.Success).value)
    }

    @Test
    fun `a converter failure while decoding maps to Failure_Serialization`() = runTest {
        val boom = IllegalArgumentException("malformed")
        val client = conduit {
            engine = ConduitEngine { HttpResponse(code = 200, body = "x".encodeToByteArray()) }
            converter = object : BodyConverter by fakeConverter {
                override fun decode(bytes: ByteArray, type: KType): Any? = throw boom
            }
        }

        val result = client.execute<User>(HttpRequest(url = "https://x/users/1"))

        assertEquals(boom, (result as ConduitResult.Failure.Serialization).cause)
    }

    @Test
    fun `typed execute without a converter fails with a clear message`() = runTest {
        val client = conduit {
            engine = ConduitEngine { HttpResponse(code = 200) }
        }

        val failure = assertFailsWith<IllegalStateException> {
            client.execute<User>(HttpRequest(url = "https://x/users/1"))
        }

        assertTrue(failure.message!!.contains("converter"))
    }

    @Test
    fun `http failures pass through without touching the converter`() = runTest {
        var decodes = 0
        val client = conduit {
            engine = ConduitEngine { HttpResponse(code = 404, body = "nope".encodeToByteArray()) }
            converter = object : BodyConverter by fakeConverter {
                override fun decode(bytes: ByteArray, type: KType): Any? {
                    decodes++
                    return fakeConverter.decode(bytes, type)
                }
            }
        }

        val result = client.execute<User>(HttpRequest(url = "https://x/users/1"))

        assertEquals(404, (result as ConduitResult.Failure.Http).code)
        assertEquals(0, decodes)
    }

    @Test
    fun `execute of Unit needs no converter and ignores the body`() = runTest {
        val client = conduit {
            engine = ConduitEngine { HttpResponse(code = 204) }
        }

        val result = client.execute<Unit>(HttpRequest(url = "https://x/users/1"))

        assertEquals(Unit, (result as ConduitResult.Success).value)
    }

    @Test
    fun `a typed body is encoded by the converter and gets its content type`() = runTest {
        var sent: HttpRequest? = null
        val client = conduit {
            engine = ConduitEngine { received ->
                sent = received
                HttpResponse(code = 204)
            }
            converter = fakeConverter
        }

        val result = client.execute<Unit, User>(
            HttpRequest(url = "https://x/users", method = HttpMethod.POST),
            body = User("mario"),
        )

        assertTrue(result is ConduitResult.Success)
        assertEquals("User(raw=mario)", sent?.body?.decodeToString())
        assertEquals("application/fake", sent?.headers?.get("Content-Type"))
    }
}
