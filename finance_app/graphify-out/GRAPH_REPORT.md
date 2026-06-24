# Graph Report - .  (2026-06-23)

## Corpus Check
- 407 files · ~213,564 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 374 nodes · 636 edges · 31 communities (24 shown, 7 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 57 edges (avg confidence: 0.81)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Accounts & Cards Data Layer|Accounts & Cards Data Layer]]
- [[_COMMUNITY_Subscriptions & Recurring|Subscriptions & Recurring]]
- [[_COMMUNITY_Money Type & CSV Export|Money Type & CSV Export]]
- [[_COMMUNITY_Project Overview & Build|Project Overview & Build]]
- [[_COMMUNITY_Security & PIN Lock|Security & PIN Lock]]
- [[_COMMUNITY_Backup Snapshot & DB Seed|Backup Snapshot & DB Seed]]
- [[_COMMUNITY_Navigation & Analysis UI|Navigation & Analysis UI]]
- [[_COMMUNITY_ViewModel State Layer|ViewModel State Layer]]
- [[_COMMUNITY_iOS Compose Bridge|iOS Compose Bridge]]
- [[_COMMUNITY_Backup IO & Crypto|Backup IO & Crypto]]
- [[_COMMUNITY_Platform Expect Surface|Platform Expect Surface]]
- [[_COMMUNITY_Desktop Platform Actuals|Desktop Platform Actuals]]
- [[_COMMUNITY_Android Platform Actuals|Android Platform Actuals]]
- [[_COMMUNITY_iOS Platform Actuals|iOS Platform Actuals]]
- [[_COMMUNITY_App Init & Koin DI|App Init & Koin DI]]
- [[_COMMUNITY_Android AES-GCM|Android AES-GCM]]
- [[_COMMUNITY_Desktop AES-GCM|Desktop AES-GCM]]
- [[_COMMUNITY_PIN Hashing|PIN Hashing]]
- [[_COMMUNITY_Android Entry Activity|Android Entry Activity]]
- [[_COMMUNITY_Release & Signing|Release & Signing]]
- [[_COMMUNITY_Nav Destinations|Nav Destinations]]
- [[_COMMUNITY_Biometric Lock Feature|Biometric Lock Feature]]
- [[_COMMUNITY_Reports Export Feature|Reports Export Feature]]

## God Nodes (most connected - your core abstractions)
1. `Money` - 19 edges
2. `SubscriptionsViewModel` - 14 edges
3. `SecurityViewModel` - 13 edges
4. `AppNav()` - 11 edges
5. `CardsViewModel` - 10 edges
6. `BackupViewModel` - 8 edges
7. `ReportsViewModel` - 8 edges
8. `TransactionsViewModel` - 8 edges
9. `SecurityRepository` - 7 edges
10. `TotalsCard()` - 7 edges

## Surprising Connections (you probably didn't know these)
- `Encrypted backups (AES-GCM, PIN-derived key)` --semantically_similar_to--> `Encryption scope (only backups, AES-GCM)`  [INFERRED] [semantically similar]
  README.md → CLAUDE.md
- `FinanZen (100% local personal finance app)` --references--> `FinanZen Project`  [INFERRED]
  README.md → CLAUDE.md
- `main()` --calls--> `initKoin()`  [INFERRED]
  desktopApp/src/main/kotlin/com/finanzen/desktop/Main.kt → shared/src/commonMain/kotlin/com/finanzen/di/AppModule.kt
- `Shared.framework (embedAndSignAppleFrameworkForXcode)` --references--> `shared module (commonMain business logic)`  [INFERRED]
  iosApp/README.md → CLAUDE.md
- `iosApp.xcodeproj manual setup guide` --references--> `iosApp module (Xcode shell)`  [INFERRED]
  iosApp/README.md → CLAUDE.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Platform shells all invoke shared App()** — claude_androidapp_module, claude_desktopapp_module, claude_iosapp_module, claude_app_composable [EXTRACTED 0.95]
- **Detekt config aggregates lint rules** — detekt_detekt_maximumlinelength, detekt_detekt_magicnumber, detekt_detekt_compose_rules, detekt_detekt_config [EXTRACTED 0.90]

## Communities (31 total, 7 thin omitted)

### Community 0 - "Accounts & Cards Data Layer"
Cohesion: 0.08
Nodes (17): Account, Card, Category, Currency, AccountRepository, CardRepository, CategoryRepository, CurrencyRepository (+9 more)

### Community 1 - "Subscriptions & Recurring"
Cohesion: 0.09
Nodes (12): SubscriptionRepository, TransactionRepository, Long, NotificationScheduler, NotificationScheduler, NotificationScheduler, NotificationScheduler, EmptyText() (+4 more)

### Community 2 - "Money Type & CSV Export"
Cohesion: 0.09
Nodes (15): CardEntity, ReportBuilder, CurrencyCode, Money, MoneyTest, Int, Map, CardItem() (+7 more)

### Community 3 - "Project Overview & Build"
Cohesion: 0.08
Nodes (29): androidApp module (Android shell), App() composable entry point, checkQuality Gradle task (Spotless + Detekt + commonTest), 10 SQLDelight tables (Currency, Account, Card, ...), desktopApp module (JVM preview, not distributed), DriverFactory.create() (SqlDriver per OS), Encryption scope (only backups, AES-GCM), FinanZen Project (+21 more)

### Community 4 - "Security & PIN Lock"
Cohesion: 0.10
Nodes (11): Boolean, SecurityRepository, App(), Modifier, Keypad(), KeypadCell(), LockScreen(), PinDots() (+3 more)

### Community 5 - "Backup Snapshot & DB Seed"
Cohesion: 0.12
Nodes (16): BackupSerializer, AccountDto, BackupSnapshot, BudgetDto, CardDto, CategoryDto, CurrencyDto, EncryptedEnvelope (+8 more)

### Community 6 - "Navigation & Analysis UI"
Cohesion: 0.13
Nodes (18): androidx, Color, AppNav(), AnalysisScreen(), CategoryBar(), StatColumn(), TotalsCard(), BackupScreen() (+10 more)

### Community 7 - "ViewModel State Layer"
Cohesion: 0.14
Nodes (9): StateFlow, ViewModel, AnalysisData, AnalysisViewModel, CategorySlice, BackupViewModel, ExportStatus, ReportsViewModel (+1 more)

### Community 8 - "iOS Compose Bridge"
Cohesion: 0.15
Nodes (12): App, Context, ComposeView, ContentView, iOSApp, Scene, Shared, SwiftUI (+4 more)

### Community 9 - "Backup IO & Crypto"
Cohesion: 0.15
Nodes (7): BackupIO, BackupCrypto, BackupCrypto, sha256Hex(), sha256Hex(), sha256Hex(), String

### Community 10 - "Platform Expect Surface"
Cohesion: 0.13
Nodes (6): DriverFactory, BackupCrypto, BiometricAuth, DriverFactory, ReportExporter, SqlDriver

### Community 11 - "Desktop Platform Actuals"
Cohesion: 0.19
Nodes (5): File, BackupIO, BiometricAuth, DriverFactory, ReportExporter

### Community 12 - "Android Platform Actuals"
Cohesion: 0.15
Nodes (4): BackupCrypto, BackupIO, BiometricAuth, ReportExporter

### Community 13 - "iOS Platform Actuals"
Cohesion: 0.17
Nodes (4): BackupIO, BiometricAuth, DriverFactory, ReportExporter

### Community 14 - "App Init & Koin DI"
Cohesion: 0.20
Nodes (6): FinanZenApplication, Application, main(), initKoin(), MainViewController(), KoinAppDeclaration

### Community 18 - "Android Entry Activity"
Cohesion: 0.40
Nodes (3): MainActivity, Bundle, ComponentActivity

## Knowledge Gaps
- **13 isolated node(s):** `UIKit`, `TopDestination`, `androidApp module (Android shell)`, `Platform.kt expect/actual surface`, `Android signing from finanzen-keystore.properties` (+8 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **7 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Money` connect `Money Type & CSV Export` to `Subscriptions & Recurring`, `Navigation & Analysis UI`?**
  _High betweenness centrality (0.061) - this node is a cross-community bridge._
- **Why does `SecurityViewModel` connect `Security & PIN Lock` to `Backup IO & Crypto`, `Navigation & Analysis UI`, `ViewModel State Layer`?**
  _High betweenness centrality (0.047) - this node is a cross-community bridge._
- **Why does `AppNav()` connect `Navigation & Analysis UI` to `Subscriptions & Recurring`, `Money Type & CSV Export`, `Security & PIN Lock`?**
  _High betweenness centrality (0.034) - this node is a cross-community bridge._
- **Are the 14 inferred relationships involving `Money` (e.g. with `.monthlySummaryLines()` and `.transactionsCsv()`) actually correct?**
  _`Money` has 14 INFERRED edges - model-reasoned connections that need verification._
- **What connects `UIKit`, `TopDestination`, `androidApp module (Android shell)` to the rest of the system?**
  _13 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Accounts & Cards Data Layer` be split into smaller, more focused modules?**
  _Cohesion score 0.07965860597439545 - nodes in this community are weakly interconnected._
- **Should `Subscriptions & Recurring` be split into smaller, more focused modules?**
  _Cohesion score 0.0907563025210084 - nodes in this community are weakly interconnected._