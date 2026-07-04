package io.github.marioponceg.conduit

import io.github.marioponceg.conduit.http.Headers
import io.github.marioponceg.conduit.http.HttpMethod
import io.github.marioponceg.conduit.http.HttpRequest
import io.github.marioponceg.conduit.result.ConduitResult

/** Executes a GET on [url] and decodes the successful body into [T]. */
public suspend inline fun <reified T> ConduitClient.get(
    url: String,
    headers: Headers = Headers.of(),
): ConduitResult<T> =
    execute<T>(HttpRequest(url = url, method = HttpMethod.GET, headers = headers))

/** Executes a POST on [url] with [body] encoded by the converter; decodes the response into [T]. */
public suspend inline fun <reified T, reified B> ConduitClient.post(
    url: String,
    body: B,
    headers: Headers = Headers.of(),
): ConduitResult<T> =
    execute<T, B>(HttpRequest(url = url, method = HttpMethod.POST, headers = headers), body)

/** Executes a PUT on [url] with [body] encoded by the converter; decodes the response into [T]. */
public suspend inline fun <reified T, reified B> ConduitClient.put(
    url: String,
    body: B,
    headers: Headers = Headers.of(),
): ConduitResult<T> =
    execute<T, B>(HttpRequest(url = url, method = HttpMethod.PUT, headers = headers), body)

/** Executes a PATCH on [url] with [body] encoded by the converter; decodes the response into [T]. */
public suspend inline fun <reified T, reified B> ConduitClient.patch(
    url: String,
    body: B,
    headers: Headers = Headers.of(),
): ConduitResult<T> =
    execute<T, B>(HttpRequest(url = url, method = HttpMethod.PATCH, headers = headers), body)

/** Executes a DELETE on [url]; decodes the response into [T] (use `delete<Unit>` to ignore it). */
public suspend inline fun <reified T> ConduitClient.delete(
    url: String,
    headers: Headers = Headers.of(),
): ConduitResult<T> =
    execute<T>(HttpRequest(url = url, method = HttpMethod.DELETE, headers = headers))
