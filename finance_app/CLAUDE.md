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

**expect/actual surface** — `platform/Platform.kt` plus one file per capability (`platform/BiometricUnlock.kt`, `platform/FilePicker.kt`, `platform/Locale.kt`, `security/Crypto.kt`):
- `platformName: String` — diagnostic, shown on the splash
- `class DriverFactory.create(): SqlDriver` — SQLDelight driver per OS (Android: `AndroidSqliteDriver` wired via Koin with `Context`; iOS: `NativeSqliteDriver`; Desktop: `JdbcSqliteDriver` → `~/.finanzen/finanzen.db`, runs `Schema.create` itself on first launch)
- `class NotificationScheduler` — reminders/budget alerts; Android real (WorkManager), iOS/Desktop are stubs (`// ponytail:` marked)
- `class ReportExporter` — CSV/PDF to disk; Desktop writes `~/Downloads`, Android/iOS have TODOs
- `class BackupCrypto` — AES-GCM backup encryption, self-describing JSON envelope (salt+iv+ciphertext), any platform can decrypt any other's backup
- `class BackupIO` — writes the backup file to platform storage (Android MediaStore/Descargas, Desktop `~/Downloads`)
- `fun rememberBiometricUnlock(): BiometricUnlock` — Android real, iOS stub (LocalAuthentication not wired yet)
- `fun rememberBackupPicker(...)` — file picker for backup import
- `fun systemCountryCode(): String` — ISO-3166 country of the system locale, no permission needed; used once on first launch to pick an initial currency (see `data/WorldLocales.kt`) instead of hardcoding USD
- `sha256Hex` / `secureRandomBytes` (`security/Crypto.kt`) — hashing/RNG primitives backing PIN storage

Add new expect/actual pairs here when you need a platform-specific capability. Keep the surface small — most things belong in `commonMain`. iOS actuals are largely stubs (`// ponytail:` marked with the real target: LocalAuthentication, UIDocumentPicker, CryptoKit, UNUserNotificationCenter) since iOS can't be exercised from this Windows dev box.

**`shared/src/commonMain/kotlin/com/finanzen/` layers**:
- `data/` — one repository per table (`AccountRepository`, `TransactionRepository`, ...) wrapping SQLDelight queries, plus `BackupSerializer`/`BackupSnapshot` (export/import format), `DefaultSeed` (first-launch seed, includes `WorldCurrencies` — 149 ISO 4217 currencies), `WorldLocales` (`CURRENCY_LOCALE_INFO` — CLDR symbol position/date order/month names per currency, plus `COUNTRY_TO_CURRENCY`; static Kotlin data generated once from the JDK's own `java.text`/`java.time` locale catalog, deliberately **not** DB columns — see the note in that file on why), `ReportBuilder` (CSV/PDF content assembly)
- `domain/` — `Money(amountMinor, currency)`, `InstallmentMath` (French amortization), `SymbolPosition`/`DateOrder` (`CurrencyFormat.kt`); pure logic, no platform deps
- `di/AppModule.kt` — single Koin module wiring `FinanzenDb` → repositories → ViewModels; `seedIfEmpty(db)` runs here on first launch, in the same transaction as currency seeding (was a referential-integrity bug — see `516d065`)
- `viewmodel/` — one per feature tab (Dashboard, Transactions, Accounts, Categories, Budgets, Subscriptions, Analysis, Reports, Security, Settings, Backup)
- `ui/screens/` + `ui/components/` + `ui/navigation/` (`AppNav`/`Destinations`) + `ui/theme/` (includes `MoneyFormat`/`LocalMoneyFormat` — symbol position, AUTO resolves per-currency via CLDR; `LocalDateLocale` — date order + month names per the base currency, consumed by every screen that renders `Money` or a date)

**Persistence: SQLDelight 2.x.** Schemas in `shared/src/commonMain/sqldelight/com/finanzen/db/*.sq` compile to the `com.finanzen.db` package (configured in `shared/build.gradle.kts`). The 10 tables (Currency, Account, Card, Category, TransactionRow, InstallmentPlan, Subscription, RecurringExpense, Budget, Setting) reference each other via FOREIGN KEY; SQLDelight handles cross-`.sq` references at schema compile time. **Use `Money(amountMinor: Long, currency: String)`** everywhere — never `Double` — to avoid float drift. All amounts in the DB are `INTEGER` in the currency's minor units.

**Encryption scope (deliberate)**: only exported backups are AES-GCM encrypted with a PBKDF2/Argon2-derived key from the user PIN. The on-disk SQLite DB is **not** encrypted — OS-level FDE covers data at rest. This is a ponytail shortcut marked in the plan; upgrade to SQLCipher only if the threat model demands it.

**Stack pins** (`gradle/libs.versions.toml`): Kotlin 2.1.0 · AGP 8.7.3 · Compose MP 1.7.3 · SQLDelight 2.0.2 · Koin 4.0 · `androidx.navigation:navigation-compose` 2.8 (KMP, **alpha** — fallback would be Voyager/PreCompose) · `androidx.lifecycle:lifecycle-viewmodel-compose` (KMP, ViewModels in `commonMain`).

## CI

`.github/workflows/ci.yml` (root-level, `working-directory: finance_app`), 4 jobs on every PR: `quality` (checkQuality, ubuntu, gates the rest) → `android` (assembleDebug + bundleRelease, unsigned), `desktop-preview` (Windows installer), `ios` (macos-14, `linkReleaseFrameworkIosArm64`, then unsigned Xcode build only if `iosApp.xcodeproj` exists). The three build jobs run in parallel after `quality` passes.

## Platform reality

- **iOS cannot be built on Windows.** The dev loop on Windows is `./gradlew :desktopApp:run`. Real iOS builds happen in CI (`macos-14` runner) or on a Mac.
- **`iosApp/iosApp.xcodeproj` does not exist yet** — created by hand once on a Mac per `iosApp/README.md`. The Swift sources, `Info.plist`, and `Config.xcconfig` are already in place to drop into a fresh Xcode iOS App project.
- **Android signing** is read from `~/.gradle/finanzen-keystore.properties` (outside the repo). When absent, the `release` signing config is simply not created and `bundleRelease` will produce an unsigned bundle.

## Reference

Full implementation plan and roadmap: `~/.claude/plans/eres-un-senior-kotlin-gentle-wombat.md`. Items marked `// ponytail:` in code are intentional shortcuts with a documented upgrade path — preserve the comment when changing nearby code.

# Política de modelos para subagentes

Cuando definas o invoques subagentes (`.claude/agents/*.md`), asigna el modelo según la naturaleza de la tarea, no por defecto `inherit`. Esto reduce costo y latencia sin sacrificar calidad donde importa.

## Guía de selección

| Modelo (`model:`) | Cuándo usarlo | Tareas típicas de subagente |
|---|---|---|
| `haiku` | Tareas mecánicas, de bajo riesgo, alto volumen, donde la velocidad y el costo importan más que el razonamiento profundo | Explorar/leer código (`Explore`-like), grep masivo, listar archivos, chequeos de formato/lint, generar mensajes de commit, resúmenes cortos, validaciones simples |
| `sonnet` | El "daily driver": la mayoría del trabajo de codificación, escritura y análisis. Si dudas, usa este | Escribir código, refactors, tests, debugging, revisión de PRs, documentación, queries de datos, tareas multi-paso estándar |
| `opus` | Razonamiento profundo y sostenido en problemas genuinamente difíciles. Cuesta más rate limit/tokens, resérvalo | Diseño de arquitectura compleja, debugging de bugs difíciles de reproducir, revisiones de seguridad críticas, decisiones con muchas variables interdependientes, planning de refactors grandes |
| `fable` | Solo para el subagente más exigente del proyecto: horizonte largo, alta autonomía, tareas que ya le costaron a Opus | Un único subagente "lead-architect" o similar para el 1-2% de tareas más complejas del repo (opcional; usar con moderación por costo) |
| `inherit` (default si se omite) | Cuando el subagente debe comportarse igual que la conversación principal | Subagentes genéricos sin perfil de costo/latencia definido |

Reglas prácticas:
- **Nunca uses `opus` u `fable` para exploración de código o tareas de lectura/grep** — usa `haiku` o `sonnet`.
- **Nunca uses `haiku` para revisiones de seguridad, arquitectura o debugging complejo** — la pérdida de calidad no vale el ahorro.
- Si un subagente hace tanto exploración como modificación en una sola tarea, prioriza el modelo que necesita el paso más difícil de la tarea (normalmente `sonnet`).
- Declara el modelo explícitamente en el frontmatter de cada subagente; no dejes `inherit` salvo que sea intencional.

## Ejemplos de subagentes con modelo asignado

```markdown
---
name: code-explorer
description: Busca y localiza código relevante sin modificarlo. Úsalo antes de cambios grandes.
tools: Read, Grep, Glob
model: haiku
---
Eres un explorador de código. Solo lees y localizas, nunca modificas.
```

```markdown
---
name: code-reviewer
description: Revisa PRs y diffs recientes en busca de calidad, seguridad y mantenibilidad. Úsalo proactivamente tras escribir código.
tools: Read, Grep, Glob, Bash
model: sonnet
---
Eres un revisor de código senior. Analiza el diff más reciente y da feedback priorizado.
```

```markdown
---
name: security-architect
description: Analiza decisiones de arquitectura con impacto en seguridad o diseños de sistema complejos. Úsalo para revisiones críticas, no para tareas rutinarias.
tools: Read, Grep, Glob, Bash
model: opus
---
Eres un arquitecto de seguridad senior. Razona a fondo antes de emitir una recomendación.
```

Al crear un subagente nuevo, pregúntate: *¿esta tarea es mecánica (haiku), estándar (sonnet), o requiere razonamiento profundo y sostenido (opus)?* Fable queda reservado para el subagente más crítico del proyecto, si existe.
