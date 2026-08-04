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

**One shared module, thin platform shells.** All business logic, UI, and persistence live in `shared/`. The `androidApp/`, `desktopApp/`, and `iosApp/` modules are entry-point-only — they each call `App()` from `commonMain` and contribute nothing else. Targets are declared in `shared/build.gradle.kts`.

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

**`shared/src/commonMain/kotlin/com/finanzen/` layers** — `data/`, `domain/`, `di/`, `viewmodel/`, `ui/`. Two non-obvious things to know:
- `WorldLocales` (`data/`) is static Kotlin data generated once from the JDK's own `java.text`/`java.time` locale catalog, deliberately **not** DB columns — see the note in that file for why.
- `di/AppModule.kt`'s `seedIfEmpty(db)` runs in the same transaction as currency seeding on purpose — splitting them was a referential-integrity bug (see `516d065`).

**Persistence: SQLDelight 2.x.** Schemas in `shared/src/commonMain/sqldelight/com/finanzen/db/*.sq` compile to the `com.finanzen.db` package (configured in `shared/build.gradle.kts`). The 10 tables (Currency, Account, Card, Category, TransactionRow, InstallmentPlan, Subscription, RecurringExpense, Budget, Setting) reference each other via FOREIGN KEY; SQLDelight handles cross-`.sq` references at schema compile time. **Use `Money(amountMinor: Long, currency: String)`** everywhere — never `Double` — to avoid float drift. All amounts in the DB are `INTEGER` in the currency's minor units.

**Encryption scope (deliberate)**: only exported backups are AES-GCM encrypted with a PBKDF2/Argon2-derived key from the user PIN. The on-disk SQLite DB is **not** encrypted — OS-level FDE covers data at rest. This is a ponytail shortcut marked in the plan; upgrade to SQLCipher only if the threat model demands it.

**Stack pins**: see `gradle/libs.versions.toml`. Note: `androidx.navigation:navigation-compose` (KMP) is still **alpha** — fallback would be Voyager/PreCompose if it becomes a blocker.

## CI

See `.github/workflows/ci.yml` (root-level, `working-directory: finance_app`).

## Platform reality

- **iOS cannot be built on Windows.** The dev loop on Windows is `./gradlew :desktopApp:run`. Real iOS builds happen in CI (`macos-14` runner) or on a Mac.
- **`iosApp/iosApp.xcodeproj` does not exist yet** — created by hand once on a Mac per `iosApp/README.md`. The Swift sources, `Info.plist`, and `Config.xcconfig` are already in place to drop into a fresh Xcode iOS App project.
- **Android signing** is read from `~/.gradle/finanzen-keystore.properties` (outside the repo). When absent, the `release` signing config is simply not created and `bundleRelease` will produce an unsigned bundle.

## Reference

Full implementation plan and roadmap: `~/.claude/plans/eres-un-senior-kotlin-gentle-wombat.md`. Items marked `// ponytail:` in code are intentional shortcuts with a documented upgrade path — preserve the comment when changing nearby code.

Model-assignment policy for subagent definitions (`.claude/agents/*.md`): see the `subagent-model-policy` skill.
