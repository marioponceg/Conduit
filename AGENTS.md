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

No source modules exist yet — the repo is a Gradle scaffold. When adding modules:

- Wire each module into `settings.gradle.kts` (`include(...)`).
- Planned shape: a `conduit-core` pure-JVM module holding the public API, result types, and
  interceptor pipeline; the OkHttp engine either inside core or as `conduit-engine-okhttp` if the
  dependency boundary warrants it (raise this when it comes up).
- As modules multiply, move shared build logic into convention plugins (`build-logic/`) rather than
  duplicating configuration; apply detekt through them.
- The root `build.gradle.kts` still declares the AGP `android-application` plugin from the original
  scaffold; remove it when the first real (pure Kotlin) module replaces the scaffold assumptions.

## Build & toolchain

- JDK 21 (`gradle/gradle-daemon-jvm.properties`), Gradle 9.4.1 (wrapper), AGP 9.2.1.
- Configuration cache is enabled (`org.gradle.configuration-cache=true`).
- Dependency versions live in `gradle/libs.versions.toml` (version catalog) — add dependencies
  there, never inline version strings in build files.

## Verification — run before considering any task done

```sh
./gradlew build    # compiles everything and runs all unit tests
./gradlew detekt   # static analysis + formatting (ktlint rules via detekt-formatting)
```

- Detekt config: `config/detekt/detekt.yml` (builds upon defaults, `maxIssues: 0`).
- TODO once Binary Compatibility Validator is applied: `./gradlew apiCheck` (and `apiDump` to
  intentionally update the tracked API surface).
- A task is not done until both commands pass locally; CI runs the same commands on every PR.

## Commit & branching conventions

- **Conventional Commits** for every commit: `feat:`, `fix:`, `chore:`, `docs:`, `test:`,
  `refactor:`, `ci:`, `build:` …
- **Trunk-based workflow**: `main` is protected. All work happens on short-lived feature branches
  (`feature/<topic>`), merged via PR. No direct commits to `main` except trivial admin files.
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
| Already in place | CI (build + test + detekt), AGENTS.md, version catalog |
| First library module PR | Kover (test coverage + CI verification step), Kotlin explicit API mode, Binary Compatibility Validator (`apiCheck` in CI), Dokka; remove scaffold `android-application` plugin |
| Before `v0.1.0` | Maven Central publishing setup + tag-triggered release workflow |

- **Coverage tool is Kover** (JetBrains' official Kotlin coverage plugin — preferred over raw
  JaCoCo, which miscounts inline functions and coroutine bodies). Do not add it before the first
  module lands; coverage config with no code to measure is dead config.
