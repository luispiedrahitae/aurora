# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project location

The Gradle project lives in **`finance_app/`**, not at the repo root. Run all Gradle commands from there. CI workflow at `../.github/workflows/ci.yml` sets `working-directory: finance_app` for the same reason.

## Build & verify

```bash
# Quality gate (Spotless + Detekt + commonTest)
./gradlew checkQuality

# Auto-fix formatting
./gradlew format

# Pre-commit shortcut: format then checkQuality
./gradlew prepareForCommit

# Android
./gradlew :androidApp:installDebug
./gradlew :androidApp:bundleRelease          # AAB for Play (needs ~/.gradle/finanzen-keystore.properties)

# Desktop preview (the daily dev loop on Windows; not for distribution)
./gradlew :desktopApp:run
./gradlew runDesktopPreview                  # alias

# iOS — requires macOS + Xcode 16+; generates Shared.framework for the Xcode project
./gradlew :shared:embedAndSignAppleFrameworkForXcode

# Tests
./gradlew :shared:allTests                    # all KMP test targets
./gradlew :shared:desktopTest --tests "com.finanzen.domain.MoneyTest.formatDosDecimales"
```

`./gradlew help` requires JDK 17 on PATH (Temurin or Android Studio JBR — see README "Requisitos").

## Architecture

**One shared module, thin platform shells.** All business logic, UI, and persistence live in `shared/`. The `androidApp/`, `desktopApp/`, and `iosApp/` modules are entry-point-only — they each call `App()` from `commonMain` and contribute nothing else.

**Targets** declared in `shared/build.gradle.kts`:
- `androidTarget()` — consumed by `:androidApp` (plain Android app, not KMP itself)
- `jvm("desktop")` — consumed by `:desktopApp` (Compose Desktop, Windows dev preview only, **not distributed**)
- `iosX64 / iosArm64 / iosSimulatorArm64` — emit `Shared.framework` consumed by `iosApp/iosApp.xcodeproj`

**expect/actual surface** (`shared/src/commonMain/kotlin/com/finanzen/platform/Platform.kt`):
- `platformName: String` — diagnostic, shown on the splash
- `class DriverFactory.create(): SqlDriver` — SQLDelight driver per OS
  - Android: `AndroidSqliteDriver(FinanzenDb.Schema, context, "finanzen.db")` (needs `Context`, wire via Koin)
  - iOS: `NativeSqliteDriver`
  - Desktop: `JdbcSqliteDriver` writing to `~/.finanzen/finanzen.db`; runs `Schema.create` on first launch (Android/iOS drivers do this themselves)

Add new expect/actual pairs here when you need a platform-specific capability (biometrics, file picker, share sheet, notifications, PDF generation). Keep the surface small — most things belong in `commonMain`.

**Persistence: SQLDelight 2.x.** Schemas in `shared/src/commonMain/sqldelight/com/finanzen/db/*.sq` compile to the `com.finanzen.db` package (configured in `shared/build.gradle.kts`). The 10 tables (Currency, Account, Card, Category, TransactionRow, InstallmentPlan, Subscription, RecurringExpense, Budget, Setting) reference each other via FOREIGN KEY; SQLDelight handles cross-`.sq` references at schema compile time. **Use `Money(amountMinor: Long, currency: String)`** everywhere — never `Double` — to avoid float drift. All amounts in the DB are `INTEGER` in the currency's minor units.

**Encryption scope (deliberate)**: only exported backups are AES-GCM encrypted with a PBKDF2/Argon2-derived key from the user PIN. The on-disk SQLite DB is **not** encrypted — OS-level FDE covers data at rest. This is a ponytail shortcut marked in the plan; upgrade to SQLCipher only if the threat model demands it.

**Stack pins** (`gradle/libs.versions.toml`): Kotlin 2.1.0 · AGP 8.7.3 · Compose MP 1.7.3 · SQLDelight 2.0.2 · Koin 4.0 · `androidx.navigation:navigation-compose` 2.8 (KMP, **alpha** — fallback would be Voyager/PreCompose) · `androidx.lifecycle:lifecycle-viewmodel-compose` (KMP, ViewModels in `commonMain`).

## Platform reality

- **iOS cannot be built on Windows.** The dev loop on Windows is `./gradlew :desktopApp:run`. Real iOS builds happen in CI (`macos-14` runner) or on a Mac.
- **`iosApp/iosApp.xcodeproj` does not exist yet** — created by hand once on a Mac per `iosApp/README.md`. The Swift sources, `Info.plist`, and `Config.xcconfig` are already in place to drop into a fresh Xcode iOS App project.
- **Android signing** is read from `~/.gradle/finanzen-keystore.properties` (outside the repo). When absent, the `release` signing config is simply not created and `bundleRelease` will produce an unsigned bundle.

## Reference

Full implementation plan and roadmap: `~/.claude/plans/eres-un-senior-kotlin-gentle-wombat.md`. Items marked `// ponytail:` in code are intentional shortcuts with a documented upgrade path — preserve the comment when changing nearby code.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
