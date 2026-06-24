# iosApp

Proyecto Xcode mínimo que consume el framework `Shared` generado por el módulo `:shared`.

## Cómo crear el `iosApp.xcodeproj` (una sola vez, en una Mac)

Este proyecto solo se puede **compilar y firmar en macOS** con Xcode 16+. Desde Windows usa el módulo `:desktopApp` para previsualizar la UI o el CI (`macos-latest`) para builds reproducibles.

Pasos para crear el `.xcodeproj` desde cero la primera vez:

1. Abre Xcode → `File → New → Project… → iOS App`.
2. Product Name: **iosApp**. Interface: **SwiftUI**. Language: **Swift**. Bundle ID: lo que pongas en `Configuration/Config.xcconfig` (por defecto `com.finanzen.ios`).
3. Guárdalo dentro de `iosApp/` reemplazando los archivos `iOSApp.swift` y `ContentView.swift` por los que ya están en este repo.
4. En *Project → Info → Configurations*, asigna `Configuration/Config.xcconfig` a Debug y Release.
5. En *Target → Build Phases* añade una nueva *Run Script Phase* **antes** de "Compile Sources" con:

   ```sh
   cd "$SRCROOT/.."
   ./gradlew :shared:embedAndSignAppleFrameworkForXcode
   ```

6. En *Target → General → Frameworks, Libraries, and Embedded Content* añade `Shared.framework` (lo verás disponible tras la primera build del paso 5).
7. Build & Run en un simulador o dispositivo.

> Alternativa rápida: usa el **Kotlin Multiplatform Wizard** de JetBrains (`https://kmp.jetbrains.com`) y reemplaza los archivos `App.kt` / `MainViewController.kt` / `iOSApp.swift` con los de este repo.

## Distribución a App Store

Ver la sección "Subir a Apple App Store" del README raíz.
