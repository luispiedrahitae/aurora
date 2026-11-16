# Rediseño — bitácora de avance

Registro vivo de por dónde va el rediseño. Se actualiza al cerrar cada paso.
Rama de trabajo: `redesign` · Respaldo intocable: `backup/pre-redesign` (2da1f2f) · Nunca tocar `main`.

## Estado actual

**Fase 2 — Estándares: en curso.** Esperando 4 decisiones del Checkpoint 1 (lista de borrado,
actualización de versiones, nombre de la app, textos en recursos).

## Fases

| Fase | Estado | Notas |
|---|---|---|
| 0 — Respaldo y ramas | ✅ Hecha (2026-11-16) | `backup/pre-redesign` y `redesign` en el remoto |
| 1 — Auditoría | ✅ Hecha (2026-11-16) | Informe entregado en el chat |
| ⛔ Checkpoint 1 | ✅ Aprobado ("continúa", 2026-11-16) | Decisiones puntuales pendientes, ver abajo |
| 2 — Estándares | 🔄 En curso | |
| 3 — Design system (Claude Design) | ⏳ Pendiente | ⛔ Checkpoint 2 antes de tocar código |
| 4 — Simulaciones | ⏳ Pendiente | ⛔ Checkpoint 3 (modelo + fórmulas) antes de implementar |
| 5 — Verificación y entrega | ⏳ Pendiente | |

## Decisiones

| Fecha | Decisión | Quién |
|---|---|---|
| 2026-11-16 | Los cambios de tooling sin commitear de `main` se commitean en `redesign` (e637024), no en `main` | Claude, aprobado en el plan |
| 2026-11-16 | No se migra de stack: se mantiene Kotlin Multiplatform + Compose | Recomendación de la auditoría |

## Pendientes de decisión

1. Lista de borrado (logs/capturas, `.impeccable/`, fuentes Inter, tooling local).
2. Actualizar Kotlin / Compose / navigation a versiones estables.
3. Nombre de la app (FinanZen / Sage / otro).
4. Textos: solo español en código o mover a recursos.

## Hallazgos de la auditoría a resolver en Fase 2

- Cadenas sueltas para tipos (`"EXPENSE"`, `"INCOME"`, frecuencias, estados) → enums.
- Lógica financiera en ViewModels (`fundTotals`, `yieldMinor`, `computeBudgets`, agregación del dashboard) → `domain/`.
- Repositorios bloqueantes llamados en el hilo principal.
- 10 archivos de `ui/` importan tipos de `data/` o de la BD.
- `FinanceComponents.kt` (1.301 líneas) y `Repositories.kt` (9 repos) por dividir.
- 175 `.dp` literales en pantallas; 12 rutas de navegación como cadenas.
- `AndroidAesGcm.kt` / `JvmAesGcm.kt` duplicados.
- 6 tests de `desktopTest` sin dependencia JVM → `commonTest`.
- Sin migraciones de BD (`verifyMigrations` activo sin esquema versionado).
- `checkQuality` falla con la configuration cache de Gradle (Spotless 6.25); pasa con `--no-configuration-cache`.
- `seedDemoTransactions` sin llamadas en producción.

## Cómo verificar

Desde `finance_app/`: `./gradlew checkQuality --no-configuration-cache` (línea base 2026-11-16: verde, 195 tests).
