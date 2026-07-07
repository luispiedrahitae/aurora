# FinanZen

App de finanzas personales **100% local**, multi-moneda, con tarjetas, cuotas, suscripciones, presupuestos, reportes y bloqueo biométrico. **Kotlin Multiplatform + Compose Multiplatform** para Android e iOS, con un preview de escritorio (JVM) para iterar la UI desde Windows.

> Sin servidor, sin cuentas, sin telemetría. Tus datos viven en tu dispositivo.

---

## Funcionalidades

- **Transacciones**: ingresos, gastos y transferencias entre cuentas.
- **Cuentas**: efectivo, débito, crédito y ahorros, con balance de apertura por cuenta.
- **Categorías**: jerárquicas (categoría + subcategoría), por tipo (ingreso/gasto), con icono y color.
- **Tarjetas**: débito y crédito, con día de corte y de pago.
- **Cuotas**: planes de cuotas (installments) sobre tarjetas con tasa de interés.
- **Suscripciones y recurrentes**: frecuencia configurable, próxima fecha de cobro y recordatorios.
- **Presupuestos**: por categoría y mes (YYYYMM), con seguimiento del consumo.
- **Multi-moneda**: tabla `Currency` con decimales y tasa relativa a la moneda base. `Money(amountMinor, currency)` evita errores de coma flotante.
- **Dashboard**: balance por cuenta, gasto por categoría, cashflow mensual.
- **Análisis**: por categoría, por mes, tendencias.
- **Reportes**: exportación **CSV** y **PDF**.
- **Backups encriptados** (AES-GCM con clave derivada del PIN/passphrase) — exportar/importar.
- **Bloqueo**: PIN / patrón / biometría (Android Biometric · Face/Touch ID en iOS).
- **Temas**: claro/oscuro + paletas configurables.
- **"Sobre la app"** y **Política de Privacidad** locales.

---

## Stack

| Capa | Tecnología |
|------|------------|
| UI multiplatforma | **Compose Multiplatform** + Material 3 |
| Lenguaje | **Kotlin 2.1** Multiplatform |
| Estado | `androidx.lifecycle:lifecycle-viewmodel-compose` (KMP) |
| Navegación | `androidx.navigation:navigation-compose` (KMP) |
| Persistencia | **SQLDelight 2.x** (drivers Android · Native iOS · JDBC SQLite Desktop) |
| Inyección | **Koin** |
| Async | Coroutines + Flow |
| Fechas | `kotlinx-datetime` |
| Serialización | `kotlinx-serialization` |
| Imágenes | `coil3` (Compose Multiplatform) |
| Calidad | Spotless (ktlint) · Detekt |

---

## Estructura del proyecto

```
finance_app/
├── settings.gradle.kts                 # incluye :shared, :androidApp, :desktopApp
├── build.gradle.kts                    # plugins + tareas checkQuality / format / prepareForCommit
├── gradle/libs.versions.toml           # version catalog (única fuente de verdad)
├── config/detekt/detekt.yml
│
├── shared/                             # TODO el código compartido
│   └── src/
│       ├── commonMain/
│       │   ├── kotlin/com/finanzen/    # App() composable, domain, data, ui, viewmodel, di
│       │   └── sqldelight/com/finanzen/db/   # schemas .sq (Account, Card, Transaction, …)
│       ├── androidMain/                # actual: AndroidSqliteDriver, biometric, etc.
│       ├── iosMain/                    # actual: NativeSqliteDriver, ComposeUIViewController
│       └── desktopMain/                # actual: JdbcSqliteDriver (preview Windows)
│
├── androidApp/                         # cáscara Android (MainActivity → App())
├── desktopApp/                         # preview JVM para Windows/Mac/Linux (no se distribuye)
└── iosApp/                             # proyecto Xcode (Swift) que embebe Shared.framework
```

Las cáscaras `androidApp` / `iosApp` / `desktopApp` son finas: solo el entry point invoca `App()` de `commonMain`.

---

## Requisitos

| Herramienta | Versión | Para qué |
|-------------|---------|----------|
| **JDK** | 17+ (Temurin recomendado) | Compilar todos los targets |
| **Android Studio** | Ladybug (2024.2) o superior | Desarrollo Android |
| **Xcode** | 16+ | Build iOS (**solo macOS**) |
| **Gradle** | usa el wrapper incluido | — |

> En Windows no puedes compilar iOS localmente. Usa `:desktopApp:run` para iterar la UI compartida y deja que CI (`macos-latest`) construya iOS.

### Instalar JDK 17 en Windows (si aún no lo tienes)

```powershell
# Opción 1: winget (recomendado)
winget install EclipseAdoptium.Temurin.17.JDK

# Opción 2: Android Studio ya trae un JBR, que sirve como JDK:
$env:JAVA_HOME = "$env:LOCALAPPDATA\Programs\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

Verifica con `java -version` (debe decir 17.x).

---

## Build & Run

### Bootstrap inicial (una sola vez)

```bash
cd finance_app
./gradlew help     # baja Gradle 8.10.2 vía el wrapper
```

### Android

```bash
# Debug en emulador o dispositivo
./gradlew :androidApp:installDebug

# Bundle firmado para Play Store (requiere keystore configurado, ver abajo)
./gradlew :androidApp:bundleRelease
# → androidApp/build/outputs/bundle/release/androidApp-release.aab
```

### Desktop preview (Windows / Mac / Linux)

Sirve para iterar la UI compartida sin emulador ni Mac.

```bash
./gradlew :desktopApp:run
# o el atajo:
./gradlew runDesktopPreview
```

Para empaquetar un instalador local (no para distribución):

```bash
./gradlew :desktopApp:packageDistributionForCurrentOS
# Windows  → MSI/EXE en desktopApp/build/compose/binaries/main/
# macOS    → DMG
# Linux    → DEB
```

### iOS (requiere macOS + Xcode)

```bash
# 1) Generar el Shared.framework para Xcode
./gradlew :shared:embedAndSignAppleFrameworkForXcode

# 2) Abrir y ejecutar
open iosApp/iosApp.xcodeproj
```

Para detalles sobre crear el `iosApp.xcodeproj` la primera vez, ver `iosApp/README.md`.

---

## Calidad de código

```bash
./gradlew format             # Spotless apply (ktlint) en todos los módulos
./gradlew checkQuality       # Spotless check + Detekt + tests del módulo shared
./gradlew prepareForCommit   # format → checkQuality (atajo previo a commit)
```

CI (`.github/workflows/ci.yml`) ejecuta `checkQuality` + build Android + build Desktop + build iOS en cada PR.

---

## Subir a **Google Play Store**

1. Crea un **upload keystore** (una vez):

   ```bash
   keytool -genkey -v -keystore finanzen-upload.jks \
     -keyalg RSA -keysize 2048 -validity 10000 -alias upload
   ```

2. Crea `~/.gradle/finanzen-keystore.properties` (fuera del repo):

   ```
   storeFile=/ruta/absoluta/a/finanzen-upload.jks
   storePassword=••••••••
   keyAlias=upload
   keyPassword=••••••••
   ```

3. Sube `versionCode` / `versionName` en `androidApp/build.gradle.kts` por release.
4. Genera el bundle:

   ```bash
   ./gradlew :androidApp:bundleRelease
   ```

5. En **Google Play Console**: crea la app, sube el AAB, completa ficha (descripción, screenshots, icono 512×512, feature graphic), política de privacidad (puede ser un GitHub Pages o gist), declaración de seguridad de datos (recordando: 100% local, sin recolección).
6. Promueve a Closed Testing → Open Testing → Production.

---

## Subir a **Apple App Store**

1. Apple Developer Program activo. Anota tu **Team ID** y úsalo en `iosApp/Configuration/Config.xcconfig`.
2. En **App Store Connect**: crea la app con Bundle ID `com.finanzen.ios` (o el que prefieras).
3. En Xcode → *Signing & Capabilities* selecciona tu Team y deja "Automatically manage signing".
4. *Product → Archive*. Cuando termine, *Distribute App → App Store Connect → Upload*.
5. En App Store Connect, sube screenshots (6.7", 6.5", 5.5"), descripción, política de privacidad, age rating, App Privacy (sin recolección).
6. **TestFlight** para beta. Luego *Submit for Review*.

> Para automatizar release con CI ver `fastlane` o GitHub Actions con `apple-actions/upload-testflight-build` (no incluido en este scaffold; agregar cuando el flujo manual sea estable).

---

## Política de privacidad (resumen)

FinanZen **no recolecta, transmite ni vende ningún dato**. Toda la información (cuentas, transacciones, tarjetas, suscripciones, etc.) se almacena exclusivamente en el dispositivo del usuario, en una base de datos SQLite local. Los backups exportados están encriptados con **AES-GCM** usando una clave derivada del PIN/passphrase del usuario y nunca abandonan el dispositivo salvo que el propio usuario los comparta.

El texto completo se publica en `docs/privacy.md` y se enlaza desde la pantalla "Sobre la app".

---

## Roadmap inmediato

Ver el plan completo en `.claude/plans/eres-un-senior-kotlin-gentle-wombat.md`. Próximos hitos:

1. Repositorios + Koin DI + ViewModels base (cuentas, transacciones).
2. Nav graph + bottom navigation + theming.
3. Pantallas core (dashboard, transactions, add/edit).
4. Tarjetas + planes de cuotas.
5. Suscripciones + recordatorios (WorkManager / UNUserNotificationCenter).
6. Análisis + gráficos koalaplot.
7. Reportes PDF/CSV.
8. Lock screen (PIN / patrón / biométrico).
9. Backup encriptado.
10. Preparación de stores (screenshots, política, About).

---

## Licencia

TBD (sugerencia: MIT o Apache 2.0).
