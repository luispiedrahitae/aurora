# Rediseño — bitácora de avance

Registro vivo de por dónde va el rediseño. Se actualiza al cerrar cada paso.
Rama de trabajo: `redesign` · Respaldo intocable: `backup/pre-redesign` (2da1f2f) · Nunca tocar `main`.

## Estado actual

**Fase 2 — Estándares: hecha.** Siguiente paso: **Fase 3**, crear el design system y las pantallas
en Claude Design y detenerse en el ⛔ Checkpoint 2.

## Fases

| Fase | Estado | Notas |
|---|---|---|
| 0 — Respaldo y ramas | ✅ Hecha (2026-11-16) | `backup/pre-redesign` y `redesign` en el remoto |
| 1 — Auditoría | ✅ Hecha (2026-11-16) | Informe entregado en el chat |
| ⛔ Checkpoint 1 | ✅ Aprobado (2026-11-16) | Decisiones en la tabla de abajo |
| 2 — Estándares | ✅ Hecha (2026-11-16) | Ver "Fase 2 paso a paso" |
| 3 — Design system (Claude Design) | ⏳ Pendiente | ⛔ Checkpoint 2 antes de tocar código |
| 4 — Simulaciones | ⏳ Pendiente | ⛔ Checkpoint 3 (modelo + fórmulas) antes de implementar |
| 5 — Verificación y entrega | ⏳ Pendiente | |

## Fase 2 paso a paso

- [x] `docs/STANDARDS.md` escrito.
- [x] Borrado aprobado: ~140 logs y capturas, 6 carpetas `.impeccable/`, tooling local viejo
      (`.superpowers/`, `.github/hooks/`, `.github/skills/`), 4 fuentes Inter.
- [x] `.gitignore` raíz ignora logs y volcados; `.gitattributes` fuerza LF en scripts.
- [x] Hook de pre-commit en `.githooks/` (activar con `git config core.hooksPath .githooks`).
- [x] Tests sin dependencia JVM movidos de `desktopTest` a `commonTest` (5 archivos).
- [x] Actualización de versiones (ver tabla). `checkQuality` verde con la configuration cache.
- [x] Detekt ahora analiza `shared/` (antes no encontraba fuentes y no revisaba nada). Las 219
      entradas de deuda previa quedaron en `config/detekt/baseline-shared.xml`.
- [x] Repositorios: un archivo por repositorio.
- [x] Cálculo de días del mes unificado en `domain/Dates.kt` (había 6 copias).
- [x] Dos errores de compilación de iOS corregidos (Koin y SHA-256); iOS ahora se compila en
      Windows como parte de `checkQuality`.

Movido a otras fases, donde ese código se reescribe de todos modos:

- **Fase 3 (pantallas):** dividir `FinanceComponents.kt`, los 175 `.dp` literales, pantallas que
  importan tipos de la BD, rutas de navegación como constantes, los 4 usos de APIs de Material
  obsoletas y activar `allWarningsAsErrors`.
- **Fase 4 (modelo de datos, Checkpoint 3):** enums en vez de cadenas para tipos, frecuencias y
  estados (vía adaptadores de columna de SQLDelight); mover a `domain/` los cálculos que hoy viven
  en ViewModels (`fundTotals`, `yieldMinor`, `computeBudgets`, agregación del dashboard); quitar
  los 5 `!!` restantes.

## Decisiones

| Fecha | Decisión | Quién |
|---|---|---|
| 2026-11-16 | Los cambios de tooling sin commitear de `main` se commitean en `redesign`, no en `main` | Claude, aprobado en el plan |
| 2026-11-16 | No se migra de stack: se mantiene Kotlin Multiplatform + Compose | Auditoría, aprobado |
| 2026-11-16 | Borrar las 5 categorías de archivos candidatos | Usuario |
| 2026-11-16 | Actualizar Kotlin, Compose y navigation a versiones estables | Usuario |
| 2026-11-16 | Nombre visible de la app: **Sage** (el paquete sigue `com.finanzen`) | Usuario |
| 2026-11-16 | App solo en español; los textos se quedan en el código | Usuario |
| 2026-11-16 | Android Gradle Plugin se queda en 8.13 (la 9 exige otra migración del módulo compartido). Por eso Compose queda en 1.11 y navigation en 2.9: las siguientes exigen AGP 9 | Claude |
| 2026-11-16 | Detekt deja el formato a Spotless y usa línea base para la deuda previa | Claude |
| 2026-11-16 | Se elimina el target `iosX64` (simulador Intel): Compose 1.12 ya no lo publica | Claude |

## Versiones

| Pieza | Antes | Ahora |
|---|---|---|
| Kotlin | 2.1.0 | 2.4.21 |
| Compose Multiplatform | 1.7.3 | 1.11.1 |
| navigation-compose | 2.8.0-alpha10 | 2.9.2 (estable) |
| lifecycle | 2.8.4 | 2.10.0 |
| SQLDelight | 2.0.2 | 2.4.1 |
| Koin | 4.0.0 | 4.2.2 |
| coroutines / serialization / datetime | 1.10.1 / 1.7.3 / 0.6.1 | 1.11.0 / 1.13.0 / 0.8.0 |
| AGP / Gradle | 8.7.3 / 8.10.2 | 8.13.2 / 8.14.7 |
| biometric | 1.2.0-alpha05 | 1.1.0 (estable) |
| Spotless / ktlint / Detekt | 6.25.0 / 1.4.1 / 1.23.7 | 8.11.0 / 1.8.0 / 1.23.8 |

## Pendientes conocidos (fuera del alcance actual)

- Migrar a AGP 9 (desbloquea Compose 1.12 y navigation 2.10).
- `AndroidAesGcm.kt` y `JvmAesGcm.kt` duplicados (requiere un source set JVM compartido).
- Repositorios bloqueantes llamados en el hilo principal.
- Sin migraciones de BD: cada cambio de esquema obliga a reinstalar mientras sea pre-release.
- iOS: sin proyecto Xcode; cripto, respaldo, notificaciones y biometría son stubs.

## Cómo verificar

Desde `finance_app/`: `./gradlew checkQuality` (2026-11-16: verde, 195 tests, incluye compilación de Android e iOS).
