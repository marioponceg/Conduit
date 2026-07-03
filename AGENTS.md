# AGENTS.md

Guidance for AI agents (and humans) working in this repository. This is the source of truth for
project conventions; `CLAUDE.md` simply points here.

## Project

Conduit is a lightweight, coroutine-first networking library for Android and the JVM, built as an
open-source **portfolio piece demonstrating senior-level engineering practices** — API design,
modern Kotlin/coroutines usage, and testing discipline. It is not a toy project, but its primary
audience is technical reviewers and recruiters reading the commit history, PRs, and code, not end
users.

Conduit will later be consumed by a separate design-system library and a sample app, but **this
repository concerns only the networking library itself**.

## Settled design decisions — do not re-litigate, implement as stated

- **Pure Kotlin/JVM module** (not an Android Library), to keep the core platform-agnostic and
  Multiplatform-friendly later. minSdk 26 applies only to future Android-facing sample/consumer
  modules, never to this library.
- **HTTP engine abstracted behind our own interface, with OkHttp as the default implementation.**
  Rationale: Ktor's client is itself an abstraction over engines (whose recommended Android/JVM
  engine is OkHttp), so layering our interface over Ktor would be an abstraction over an
  abstraction. Using OkHttp directly keeps full control, and the coroutine bridge over
  `Call.enqueue` with structured cancellation is exactly the kind of code this project exists to
  showcase. The engine interface is the future KMP seam: the core module stays pure, and OkHttp
  appears only in the default engine implementation.
- **Errors modeled as a sealed result type** (success / HTTP error / network error / serialization
  error), not thrown exceptions, enabling exhaustive `when` handling.
- **Public configuration via a Kotlin DSL builder.**
- **Own interceptor pipeline** — do not just re-expose OkHttp's interceptors.
- **Coroutines-first API**: suspend functions; `Flow` only where it adds real value.
- **Kotlin explicit API mode** enabled on all library modules.
- **Binary Compatibility Validator** plugin tracks the public API surface (to be applied with the
  first library module; see CI TODO).
- **Dokka** for generated API docs.

Any design decision **not** listed above must be raised with the maintainer before implementing.

## Module structure

- `conduit-core` — pure Kotlin/JVM module holding the public API: the sealed `ConduitResult`
  type (`…conduit.result`), the engine-agnostic HTTP models (`…conduit.http`), and the
  `ConduitEngine` interface (`…conduit.engine`); the interceptor pipeline and DSL land in later
  PRs. Its only dependency is `kotlinx-coroutines-core` (KMP-friendly, required by the
  coroutines-first design). Library tooling applied: explicit API mode, Kover, Binary
  Compatibility Validator, Dokka.
- **Settled (2026-07-04): the OkHttp engine lives in a separate `conduit-engine-okhttp` module**,
  so core stays physically dependency-free and the engine seam is the real KMP boundary. The
  engine module exposes core as an `api` dependency so consumers can declare a single artifact.
- **Engine contract is raw by design**: engines return the `HttpResponse` for any status code and
  throw only `IOException` / `CancellationException`; the mapping to `ConduitResult` happens once,
  in core — never inside an engine.
- Wire each new module into `settings.gradle.kts` (`include(...)`).
- Shared build logic lives in the `build-logic/` included build: library modules apply the
  `conduit.library` convention plugin (Kotlin/JVM + explicit API, detekt with formatting, Kover
  with the 90% rule, Dokka, JUnit 5 platform) instead of repeating configuration. Root-level
  plugins that must see the Kotlin plugin classes (e.g. Binary Compatibility Validator) require
  the plugins to stay loaded `apply false` in the root build so everything shares one
  classloader.

## Build & toolchain

- JDK 21 (`gradle/gradle-daemon-jvm.properties`), Gradle 9.4.1 (wrapper), AGP 9.2.1.
- Configuration cache is enabled (`org.gradle.configuration-cache=true`).
- Dependency versions live in `gradle/libs.versions.toml` (version catalog) — add dependencies
  there, never inline version strings in build files.

## Verification — run before considering any task done

```sh
./gradlew build      # compiles everything and runs all unit tests
./gradlew detekt     # static analysis + formatting (ktlint rules via detekt-formatting)
./gradlew apiCheck   # public API surface matches the committed api/*.api dumps
```

- Detekt config: `config/detekt/detekt.yml` (builds upon defaults, `maxIssues: 0`).
- Intentional public API changes: run `./gradlew apiDump` and commit the updated `api/*.api` files.
- Coverage: `./gradlew koverVerify` enforces a **90% minimum line coverage** on `conduit-core`.
- A task is not done until these commands pass locally; CI runs the same commands on every PR.

## Commit & branching conventions

- **Conventional Commits** for every commit: `feat:`, `fix:`, `chore:`, `docs:`, `test:`,
  `refactor:`, `ci:`, `build:` …
- **Trunk-based workflow**: `main` is protected by a repository ruleset — PR required (no direct
  pushes, no admin bypass), force-pushes and deletion blocked, CI and PR-title checks must pass.
  All work, however trivial, happens on short-lived feature branches (`feature/<topic>`) merged
  via PR.
- **No `develop` or standing release branches.** `main` is always green and always releasable;
  releases are cut from `main` via tags (see below). A `release/x.y` branch is created only if a
  fix ever needs backporting to an older published version — never pre-emptively.
- **Small, reviewable PRs** — one design unit per PR (e.g. one PR for result types, one for the
  HTTP engine abstraction), each including its own tests. PR descriptions explain the *why*, not
  just the *what*.
- **PRs are squash-merged.** The PR title becomes the commit on `main`, so it must follow
  Conventional Commits — CI validates this on every PR (`.github/workflows/pr-title.yml`).

## Releases, versioning & publishing

- **Versioning**: SemVer. Releases are annotated tags on `main` (`vX.Y.Z`); a tag push will
  trigger the release workflow (to be added before `v0.1.0`).
- **Publishing target: Maven Central** under the `io.github.marioponceg` namespace
  (e.g. `io.github.marioponceg:conduit-core`), using the `com.vanniktech.maven.publish` plugin
  and the Central Portal, with GPG-signed artifacts. Implement the publishing setup and the
  tag-triggered release workflow when a minimal publishable API exists — not before.
- **Consumption during development** (design-system library, sample app): via Gradle composite
  builds (`includeBuild`) or `mavenLocal` — no published artifact is required to start consuming
  Conduit from sibling repos.

## Tooling roadmap — when each piece gets wired

| When | What |
|---|---|
| Already in place | CI (build + test + detekt + `koverVerify` + `apiCheck` + Codecov upload), AGENTS.md, version catalog, `main` ruleset, `conduit-core` with tooling (#2), `ConduitResult` (#3), engine interface + HTTP models |
| Next PRs (one design unit each) | `conduit-engine-okhttp` module (coroutine bridge over `Call.enqueue`); interceptor pipeline; configuration DSL |
| Before `v0.1.0` | Maven Central publishing setup + tag-triggered release workflow |

- **Coverage tool is Kover** (JetBrains' official Kotlin coverage plugin — preferred over raw
  JaCoCo, which miscounts inline functions and coroutine bodies). CI uploads the Kover XML report
  to **Codecov** (badge in README, PR comments with total + diff coverage; token in the
  `CODECOV_TOKEN` repo secret).
