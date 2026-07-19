# Conduit

[![CI](https://github.com/marioponceg/Conduit/actions/workflows/ci.yml/badge.svg)](https://github.com/marioponceg/Conduit/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/marioponceg/Conduit/branch/main/graph/badge.svg)](https://codecov.io/gh/marioponceg/Conduit)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.marioponceg/conduit-core)](https://central.sonatype.com/artifact/io.github.marioponceg/conduit-core)

Lightweight, coroutine-first networking library for Android and the JVM, built on OkHttp with
typed error handling, request/response interceptors and built-in retry policies.

```kotlin
val client = conduit {
    engine = OkHttpEngine()
    converter = KotlinxJsonConverter()
    baseUrl = "https://api.example.com"
    interceptors += RetryInterceptor()
}

when (val result = client.get<List<User>>("/users")) {
    is ConduitResult.Success -> show(result.value)
    is ConduitResult.Failure.Http -> log("HTTP ${result.code}: ${result.body}")
    is ConduitResult.Failure.Network -> retryLater(result.cause)
    is ConduitResult.Failure.Serialization -> report(result.cause)
}
```

## Why Conduit

- **Errors are values, not exceptions.** Every call returns a sealed `ConduitResult`: success,
  HTTP error, network error or serialization error. The compiler forces you to handle all of
  them — no invisible control flow, no forgotten catch blocks.
- **Coroutines-first.** Suspend functions end to end, with real structured cancellation:
  cancelling your coroutine cancels the HTTP call in flight.
- **Own interceptor pipeline.** Suspend chain-of-responsibility for auth, logging, caching or
  retries — policy as data, independent of the transport underneath.
- **Retry policies built in.** Exponential backoff with jitter, transient-only triggers, and
  non-idempotent methods excluded by default (no accidentally duplicated POSTs).
- **Modular by design.** The core has a single dependency (kotlinx-coroutines). OkHttp and
  kotlinx.serialization live in their own modules behind seams — swap either without touching
  your code.

## Modules

| Module | What it gives you |
|---|---|
| `conduit-core` | `ConduitResult`, HTTP models, engine and converter seams, interceptor pipeline, `conduit { }` DSL |
| `conduit-engine-okhttp` | Default engine: OkHttp bridged into suspend calls with structured cancellation |
| `conduit-serialization-kotlinx` | Default converter: kotlinx.serialization JSON |

Declaring the engine and converter modules is enough — both expose `conduit-core` transitively.

## Installation

Conduit is published on Maven Central:

```kotlin
dependencies {
    implementation("io.github.marioponceg:conduit-engine-okhttp:0.1.0")
    implementation("io.github.marioponceg:conduit-serialization-kotlinx:0.1.0")
}
```

## Making requests

Typed verbs cover the common cases; bodies are encoded and decoded by the configured converter:

```kotlin
val user: ConduitResult<User> = client.get<User>("/users/1")

client.post<User, CreateUser>("/users", body = CreateUser(name = "Ana"))

client.delete<Unit>("/users/1")   // Unit skips decoding entirely
```

For full control, build the request yourself — `execute` returns the raw `HttpResponse`, and
`execute<T>` decodes it:

```kotlin
val raw: ConduitResult<HttpResponse> = client.execute(
    HttpRequest(url = "/reports", method = HttpMethod.POST, body = bytes),
)
```

Relative URLs resolve against `baseUrl`; absolute URLs pass through untouched.

## Interceptors

An interceptor sees the request, decides what to do with the chain, and returns a response —
mutate, observe, short-circuit or retry, all with one pattern:

```kotlin
val auth = ConduitInterceptor { chain ->
    chain.proceed(
        chain.request.copy(headers = chain.request.headers + ("Authorization" to "Bearer $token")),
    )
}
```

Interceptors run in list order around the engine, and the pipeline is itself a `ConduitEngine`,
so intercepted engines compose anywhere an engine fits.

## Retries

```kotlin
interceptors += RetryInterceptor(
    maxAttempts = 4,
    initialDelay = 250.milliseconds,
    retryNonIdempotent = false,   // the default: POST/PATCH are never replayed silently
)
```

Retries trigger on `IOException` and transient statuses (`408, 429, 500, 502, 503, 504` by
default), waiting with exponential backoff and jitter between attempts.

## Design notes

Conduit is a portfolio project built with senior-engineering discipline: every design unit lands
as a reviewed PR with tests first, public API tracked by the Binary Compatibility Validator,
90% minimum line coverage enforced in CI, and the reasoning behind each decision recorded in
[AGENTS.md](AGENTS.md).

## License

[Apache 2.0](LICENSE)
