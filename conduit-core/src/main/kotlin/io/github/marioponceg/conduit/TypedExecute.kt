package io.github.marioponceg.conduit

import io.github.marioponceg.conduit.http.HttpRequest
import io.github.marioponceg.conduit.result.ConduitResult
import kotlinx.coroutines.CancellationException
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Executes [request] and decodes a successful body into [T] with the client's configured
 * converter. HTTP and network failures pass through untouched; a decoding failure becomes
 * [ConduitResult.Failure.Serialization].
 */
public suspend inline fun <reified T> ConduitClient.execute(request: HttpRequest): ConduitResult<T> =
    executeTyped(request, typeOf<T>())

/**
 * Like `execute<T>`, but first encodes [body] with the configured converter, sending it with
 * the converter's `Content-Type` (unless the request already sets one). An encoding failure
 * becomes [ConduitResult.Failure.Serialization] without anything being sent.
 */
public suspend inline fun <reified T, reified B> ConduitClient.execute(
    request: HttpRequest,
    body: B,
): ConduitResult<T> = executeTypedWithBody(request, typeOf<T>(), body, typeOf<B>())

@PublishedApi
internal suspend fun <T> ConduitClient.executeTypedWithBody(
    request: HttpRequest,
    responseType: KType,
    body: Any?,
    bodyType: KType,
): ConduitResult<T> {
    val converter = checkNotNull(converter) {
        "Typed execute needs a converter: set `converter = ...` in the conduit { } block " +
            "(e.g. KotlinxJsonConverter from conduit-serialization-kotlinx)"
    }
    val encoded = try {
        converter.encode(body, bodyType)
    } catch (e: CancellationException) {
        throw e
    } catch (
        @Suppress("TooGenericExceptionCaught") e: Exception,
    ) {
        return ConduitResult.Failure.Serialization(e)
    }
    val headers = if (request.headers["Content-Type"] == null) {
        request.headers + ("Content-Type" to converter.contentType)
    } else {
        request.headers
    }
    return executeTyped(
        HttpRequest(url = request.url, method = request.method, headers = headers, body = encoded),
        responseType,
    )
}

@PublishedApi
internal suspend fun <T> ConduitClient.executeTyped(request: HttpRequest, type: KType): ConduitResult<T> {
    if (type == typeOf<Unit>()) {
        @Suppress("UNCHECKED_CAST")
        return when (val raw = execute(request)) {
            is ConduitResult.Failure -> raw
            is ConduitResult.Success -> ConduitResult.Success(Unit) as ConduitResult<T>
        }
    }
    val converter = checkNotNull(converter) {
        "Typed execute needs a converter: set `converter = ...` in the conduit { } block " +
            "(e.g. KotlinxJsonConverter from conduit-serialization-kotlinx)"
    }
    return when (val raw = execute(request)) {
        is ConduitResult.Failure -> raw
        is ConduitResult.Success -> try {
            @Suppress("UNCHECKED_CAST")
            ConduitResult.Success(converter.decode(raw.value.body ?: ByteArray(0), type) as T)
        } catch (e: CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            // The converter is pluggable, so its failure type is unknowable here; anything it
            // throws while decoding is, by definition, a serialization failure.
            ConduitResult.Failure.Serialization(e)
        }
    }
}
