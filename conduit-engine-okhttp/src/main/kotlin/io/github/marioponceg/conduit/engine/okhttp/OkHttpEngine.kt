package io.github.marioponceg.conduit.engine.okhttp

import io.github.marioponceg.conduit.engine.ConduitEngine
import io.github.marioponceg.conduit.http.Headers
import io.github.marioponceg.conduit.http.HttpRequest
import io.github.marioponceg.conduit.http.HttpResponse
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * [ConduitEngine] backed by OkHttp.
 *
 * Bridges OkHttp's callback-based [Call.enqueue] into a suspend function with structured
 * cancellation: cancelling the calling coroutine cancels the in-flight HTTP call.
 *
 * @param client the underlying client; pass a preconfigured one to share its connection pool
 * and dispatcher with the rest of the application.
 */
public class OkHttpEngine(
    private val client: OkHttpClient = OkHttpClient(),
) : ConduitEngine {

    override suspend fun execute(request: HttpRequest): HttpResponse {
        val call = client.newCall(request.toOkHttp())
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        // Reading the body is itself I/O and can fail; treat it as any
                        // other transport failure instead of crashing the callback thread.
                        val conduitResponse = try {
                            response.use { it.toConduit() }
                        } catch (e: IOException) {
                            continuation.resumeWithException(e)
                            return
                        }
                        continuation.resume(conduitResponse)
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        continuation.resumeWithException(e)
                    }
                },
            )
        }
    }
}

private fun HttpRequest.toOkHttp(): Request =
    Request.Builder()
        .url(url)
        .apply {
            for (name in headers.names()) {
                for (value in headers.values(name)) {
                    addHeader(name, value)
                }
            }
        }
        .method(method.value, body?.toRequestBody())
        .build()

private fun Response.toConduit(): HttpResponse =
    HttpResponse(
        code = code,
        headers = Headers.of(headers.toList()),
        body = body?.bytes(),
    )
