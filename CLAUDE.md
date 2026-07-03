# CLAUDE.md

All project guidance for AI agents lives in [AGENTS.md](AGENTS.md) — read it before making any
change. It covers the project purpose, settled design decisions, module structure, build/toolchain
details, verification commands, and commit/branching/release conventions.

## Claude Code skills

Environment-specific guidance for Claude Code sessions — which skills to use when (these refer to
plugins installed locally, which is why they live here and not in the tool-agnostic AGENTS.md):

- `superpowers:test-driven-development` — default workflow for all library code; every design unit
  lands with its tests.
- `superpowers:writing-plans` / `superpowers:executing-plans` — for multi-PR design units before
  touching code.
- `superpowers:verification-before-completion` — before declaring any task done (pairs with the
  verification commands in AGENTS.md).
- `superpowers:requesting-code-review` / `superpowers:finishing-a-development-branch` — when
  wrapping up a feature branch into a PR.
- `superpowers:systematic-debugging` — for non-obvious test failures or build issues.
- Android skills (`android-cli`, `testing-setup`, etc.) apply **only** to future Android-facing
  sample/consumer modules, not to this pure-JVM library.
