# Estándares de código — Sage

Reglas que sigue todo el código de `finance_app/`. Si una regla estorba, se cambia aquí primero y
después en el código; no se rompe en silencio.

Nombre visible de la app: **Sage**. El paquete sigue siendo `com.finanzen` (identificador interno,
no se renombra). Idioma: prosa, comentarios, textos de UI y mensajes de commit en **español**;
identificadores de código en **inglés**. La app es solo en español: los textos van en el código.

## 1. Arquitectura y carpetas

Un módulo compartido (`shared/`) y tres cascarones (`androidApp/`, `desktopApp/`, `iosApp/`) que
solo arrancan `App()`.

```
shared/src/commonMain/kotlin/com/finanzen/
├── domain/      Lógica pura: dinero, calendarios, cálculos financieros, simulaciones.
├── data/        Repositorios (uno por archivo), respaldo, semillas. Único dueño de SQLDelight.
├── viewmodel/   Un ViewModel por pantalla. Orquesta repositorios + domain y expone estado.
├── ui/
│   ├── theme/       Tokens: color, tipografía, espaciado, radios, sombras, movimiento.
│   ├── components/  Componentes reutilizables, uno por archivo.
│   ├── screens/     Una pantalla por archivo; componentes privados de esa pantalla al final.
│   └── navigation/  Rutas y grafo.
├── platform/    expect/actual (una capacidad por archivo).
├── security/    Cripto y PIN.
└── di/          Módulos de Koin.
```

Dirección de dependencias: `ui → viewmodel → data → domain`. Reglas duras:

- `domain/` no importa Compose, SQLDelight, Koin ni nada de `data/`, `viewmodel/` o `ui/`.
  Recibe y devuelve tipos propios o primitivos.
- Todo cálculo financiero (totales, rendimientos, cuotas, proyecciones, presupuestos, tendencias)
  vive en `domain/`. Un ViewModel no hace aritmética de dinero: llama a `domain/`.
- `ui/` no ejecuta consultas ni llama repositorios; solo habla con su ViewModel.
- Ningún archivo supera ~400 líneas. Si crece, se divide por responsabilidad.
- Las capacidades de plataforma nuevas se añaden como par expect/actual en `platform/`, y se
  mantienen pocas.

## 2. Nombres

| Qué | Convención | Ejemplo |
|---|---|---|
| Archivos | PascalCase, nombre del tipo o composable principal | `FundProjection.kt` |
| Pantallas | `<Nombre>Screen` | `InvestmentsScreen` |
| ViewModels | `<Nombre>ViewModel`; estado `<Nombre>UiState` | `BudgetsViewModel` |
| Repositorios | `<Entidad>Repository`, un archivo cada uno | `AccountRepository` |
| Composables | PascalCase, sustantivo | `MoneyText` |
| Funciones | camelCase, verbo | `projectFund()` |
| Flujos observables | `observe<Algo>()` | `observeOpen()` |
| Montos | sufijo `Minor`, tipo `Long` | `amountMinor` |
| Fechas | sufijo `EpochDay` o tipo `LocalDate` | `startEpochDay` |
| Tasas | sufijo `Pct` (porcentaje) o `Rate` (fracción) | `annualRatePct` |
| Constantes | UPPER_SNAKE_CASE | `MAX_PIN_LENGTH` |
| Tests | frase en español en camelCase | `proyeccionSinAportesCreceSoloPorInteres` |
| Tablas y consultas SQL | tabla PascalCase singular, consulta camelCase | `Investment`, `selectOpen` |

## 3. Linter y formatter

- **Spotless + ktlint** formatea; **Detekt** analiza (`config/detekt/detekt.yml`, `maxIssues: 0`).
- Comandos, desde `finance_app/`:
  - `./gradlew format` — corrige formato.
  - `./gradlew checkQuality` — Spotless + Detekt + todos los tests. Es la puerta de calidad del CI.
  - `./gradlew prepareForCommit` — ambos.
- **Pre-commit:** `.githooks/pre-commit` corre `spotlessCheck` y `detekt`. Se activa una vez por
  clon con `git config core.hooksPath .githooks`. Los tests corren en el CI, no en el hook.
- Spotless es el único dueño del formato; Detekt no trae reglas de formato.
- `config/detekt/baseline-shared.xml` congela la deuda anterior al rediseño. El código nuevo no
  puede añadir entradas; el archivo solo se regenera (`./gradlew detektBaseline`) para encogerlo.
- No se usa `@Suppress` sin un comentario que explique por qué.

## 4. Tipado

- Dinero: siempre `Long` en unidades menores o `Money`. Nunca `Double` para un monto.
  `Double` solo para tasas, con redondeo explícito al volver a dinero.
- Conjuntos cerrados (tipo de movimiento, tipo de cuenta, frecuencia, estado) son `enum class`,
  no cadenas. La conversión a texto ocurre solo en el borde con la base de datos.
- Prohibido `!!`. Se usa `?: return`, `requireNotNull` con mensaje o un tipo no nulo.
- Las funciones públicas de `domain/` y `data/` declaran su tipo de retorno.
- Los warnings del compilador son errores (`allWarningsAsErrors`); se activa al cerrar la Fase 3,
  cuando desaparezcan los usos de APIs de Material obsoletas.

## 5. Errores, estado y datos

- **Errores esperados** (archivo inválido, clave incorrecta, validación) se devuelven como tipo
  (`sealed interface` de resultado), no como excepción ni como cadena `"error:"`. La pantalla
  traduce el resultado a un mensaje en español.
- **Errores inesperados** no se tragan: nada de `catch (e: Exception) {}` vacío.
- **Validación** en el borde (formularios), con un mensaje por campo.
- **Estado:** cada ViewModel expone `StateFlow` inmutables creados con
  `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), inicial)`. El estado inicial
  distingue "cargando" de "vacío".
- **Lecturas** reactivas: `Flow` de SQLDelight desde el repositorio.
- **Escrituras:** una operación de negocio = una transacción de base de datos.
- **Ids nuevos:** siempre `lastInsertRowId()`, nunca `selectAll().last()`.
- **Esquema:** cualquier cambio de tabla lleva su migración `.sqm` una vez publicada la app;
  mientras sea pre-release se anota en el `.sq` que requiere reinstalar.

## 6. Tests

- Toda función de `domain/` tiene tests en `commonTest` (corren en todas las plataformas).
  Mínimo por cálculo financiero: caso normal, borde (cero, un periodo, tasa 0) y redondeo.
- Repositorios y ViewModels se prueban en `desktopTest` contra SQLite en memoria.
- Un test que no depende de la JVM no va en `desktopTest`.
- Un bug corregido deja un test que lo reproduce.

## 7. Commits

[Conventional Commits](https://www.conventionalcommits.org/) en español, imperativo, un tipo de
cambio por commit:

```
<tipo>(<ámbito opcional>): <resumen en minúscula, sin punto final>
```

Tipos: `feat`, `fix`, `refactor`, `style`, `test`, `docs`, `chore`, `build`, `ci`, `perf`.
Ejemplos: `feat(inversiones): añade proyección por escenarios`,
`refactor(data): separa los repositorios en archivos`.

El trabajo del rediseño va en la rama `redesign`; `main` y `backup/pre-redesign` no se tocan.
