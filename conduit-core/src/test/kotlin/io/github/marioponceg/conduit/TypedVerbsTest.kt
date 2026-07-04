package io.github.marioponceg.conduit

import io.github.marioponceg.conduit.converter.BodyConverter
import io.github.marioponceg.conduit.engine.ConduitEngine
import io.github.marioponceg.conduit.http.HttpRequest
import io.github.marioponceg.conduit.http.HttpResponse
import io.github.marioponceg.conduit.result.ConduitResult
import kotlinx.coroutines.test.runTest
import kotlin.reflect.KType
import kotlin.test.Test
import kotlin.test.assertEquals

class TypedVerbsTest {

    data class User(val raw: String)

    private val converter = object : BodyConverter {
        override val contentType: String = "application/fake"
        override fun decode(bytes: ByteArray, type: KType): Any? = User(bytes.decodeToString())
        override fun encode(value: Any?, type: KType): ByteArray = value.toString().encodeToByteArray()
    }

    private fun clientRecording(record: (HttpRequest) -> Unit): ConduitClient = conduit {
        engine = ConduitEngine { received ->
            record(received)
            HttpResponse(code = 200, body = "mario".encodeToByteArray())
        }
        converter = this@TypedVerbsTest.converter
        baseUrl = "https://api.example.com"
    }

    @Test
    fun `get sends a GET to the resolved url and decodes the body`() = runTest {
        var sent: HttpRequest? = null

        val result = clientRecording { sent = it }.get<User>("/users/1")

        assertEquals("GET", sent?.method?.value)
        assertEquals("https://api.example.com/users/1", sent?.url)
        assertEquals(User("mario"), (result as ConduitResult.Success).value)
    }

    @Test
    fun `post sends the encoded body with the converter content type`() = runTest {
        var sent: HttpRequest? = null

        clientRecording { sent = it }.post<User, User>("/users", body = User("ana"))

        assertEquals("POST", sent?.method?.value)
        assertEquals("User(raw=ana)", sent?.body?.decodeToString())
        assertEquals("application/fake", sent?.headers?.get("Content-Type"))
    }

    @Test
    fun `put and patch send their methods with encoded bodies`() = runTest {
        val methods = mutableListOf<String>()
        val client = clientRecording { methods += it.method.value }

        client.put<User, User>("/users/1", body = User("ana"))
        client.patch<User, User>("/users/1", body = User("ana"))

        assertEquals(listOf("PUT", "PATCH"), methods)
    }

    @Test
    fun `delete sends a DELETE without body`() = runTest {
        var sent: HttpRequest? = null

        clientRecording { sent = it }.delete<Unit>("/users/1")

        assertEquals("DELETE", sent?.method?.value)
        assertEquals(null, sent?.body)
    }
}
