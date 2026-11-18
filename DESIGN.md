---
name: Sage
description: Libro contable cálido — fondo crema, terracota como color de acción, salvia para ingresos, tarjetas planas con borde fino, títulos en Caprasimo y cifras en IBM Plex Sans tabular.
colors:
  light-bg: "#F5EAD8"
  light-surface: "#FAF3E6"
  light-surface-sunken: "#ECDCC2"
  light-border: "#D8C9AE"
  light-border-strong: "#8A7859"
  light-ink: "#201E1D"
  light-ink-muted: "#5B5450"
  light-primary: "#AD6332"
  light-primary-ink: "#7F4824"
  light-on-primary: "#FFFFFF"
  light-primary-soft: "#F7EBE3"
  light-income: "#4B553A"
  light-income-soft: "#E7EAE1"
  light-expense: "#7F4824"
  light-expense-soft: "#F3DCCB"
  light-warning: "#8F4A07"
  light-warning-soft: "#F6E3BF"
  light-danger: "#BA1A1A"
  light-danger-soft: "#FFDAD6"
  light-focus: "#1A56C4"
  dark-bg: "#221F19"
  dark-surface: "#2B2720"
  dark-surface-sunken: "#382F23"
  dark-border: "#5A4F3C"
  dark-border-strong: "#8C7E66"
  dark-ink: "#FBF7F0"
  dark-ink-muted: "#C4B9A5"
  dark-primary: "#D79B74"
  dark-primary-ink: "#E4BBA0"
  dark-on-primary: "#372010"
  dark-primary-soft: "#573219"
  dark-income: "#BBC5AA"
  dark-income-soft: "#363D29"
  dark-expense: "#E4BBA0"
  dark-expense-soft: "#573219"
  dark-warning: "#FBBF24"
  dark-warning-soft: "#4A3A10"
  dark-danger: "#FFB4AB"
  dark-danger-soft: "#5C1512"
  dark-focus: "#9CC0FF"
typography:
  title-screen: { fontFamily: "Caprasimo", fontSize: "28sp", lineHeight: "34sp", fontWeight: 400 }
  title-section: { fontFamily: "Caprasimo", fontSize: "20sp", lineHeight: "26sp", fontWeight: 400 }
  title-card: { fontFamily: "IBM Plex Sans", fontSize: "16sp", lineHeight: "22sp", fontWeight: 600 }
  body: { fontFamily: "IBM Plex Sans", fontSize: "16sp", lineHeight: "24sp", fontWeight: 400, fontFeature: "tnum" }
  body-small: { fontFamily: "IBM Plex Sans", fontSize: "14sp", lineHeight: "20sp", fontWeight: 400 }
  label: { fontFamily: "IBM Plex Sans", fontSize: "14sp", lineHeight: "20sp", fontWeight: 500 }
  caption: { fontFamily: "IBM Plex Sans", fontSize: "12sp", lineHeight: "16sp", fontWeight: 400 }
  overline: { fontFamily: "IBM Plex Sans", fontSize: "11sp", lineHeight: "16sp", fontWeight: 600, letterSpacing: "0.06em", textTransform: "uppercase" }
  amount-hero: { fontFamily: "IBM Plex Sans", fontSize: "40sp", lineHeight: "44sp", fontWeight: 600, fontFeature: "tnum" }
  amount-lg: { fontFamily: "IBM Plex Sans", fontSize: "24sp", lineHeight: "28sp", fontWeight: 600, fontFeature: "tnum" }
  amount-md: { fontFamily: "IBM Plex Sans", fontSize: "16sp", lineHeight: "22sp", fontWeight: 600, fontFeature: "tnum" }
  amount-sm: { fontFamily: "IBM Plex Sans", fontSize: "14sp", lineHeight: "20sp", fontWeight: 500, fontFeature: "tnum" }
  data: { fontFamily: "IBM Plex Mono", fontSize: "12sp", lineHeight: "16sp", fontWeight: 400 }
rounded:
  sm: "6dp"
  md: "10dp"
  lg: "16dp"
  full: "50%"
spacing: [4, 8, 12, 16, 20, 24, 32, 48]
---

# Design system: Sage

Guía de uso del sistema visual de la app. Los valores viven en código en `finance_app/shared/src/commonMain/kotlin/com/finanzen/ui/theme/` (`Color.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`, `Motion.kt`) y los componentes en `ui/components/`. La referencia visual está en Claude Design:

- Design system: https://claude.ai/artifact/QdRX1cFzN6aWpb9gnEXy5s
- Pantallas (24): https://claude.ai/artifact/D5SE4kr7NDM584HCZmj2th

## 1. Idea

Un libro contable cálido: una cifra principal por pantalla, todo lo demás plano y ordenado. La identidad viene de la app Sage original (crema, terracota, salvia y Caprasimo); la estructura, de un registro financiero: tarjetas planas con borde fino, filas alineadas y números tabulares.

## 2. Color

| Rol | Uso | Token Compose |
|---|---|---|
| bg | Fondo de página | `colorScheme.background` |
| surface | Tarjetas, barra inferior, hojas | `colorScheme.surface` |
| surface-sunken | Campos, pistas de progreso, segmentado | `colorScheme.surfaceVariant` |
| border / border-strong | Borde de tarjeta y divisores / borde de campos y botones secundarios | `outlineVariant` / `outline` |
| ink / ink-muted | Texto principal / secundario | `onSurface` / `onSurfaceVariant` |
| primary | Rellenos de acción (botón primario, FAB, día elegido) | `primary` |
| primary-ink | Texto en color de acción (enlaces, botón fantasma, avatar) | `onPrimaryContainer` |
| primary-soft | Fondo suave de avatar, pestaña activa, chip elegido | `primaryContainer` |
| income / expense | Montos de ingreso y gasto | `LocalFinanceColors.current.income/expense` |
| warning | Presupuesto cerca del límite (85 %+), cupo casi lleno | `LocalFinanceColors.current.warning` |
| danger | Errores de formulario, acciones destructivas | `colorScheme.error` |
| focus | Borde del campo con foco | `LocalFinanceColors.current.focus` |
| chart 1–7 | Series de gráficos | `LocalFinanceColors.current.chart` |

Reglas:

- **El color nunca va solo.** Ingreso y gasto (salvia y terracota oscuro) se confunden con daltonismo rojo-verde: todo monto lleva signo (+ / −) y, donde hay espacio, flecha o insignia con texto.
- **El terracota de acción no se usa como texto** sobre crema (no llega a 4.5:1); para texto está `primary-ink`.
- **El acento es elegible** en Apariencia (20 colores). Cambia solo la tríada primary/primary-ink/primary-soft; fondos, superficies y colores financieros son fijos.
- **Tema claro, oscuro o automático** (sigue al teléfono). Todos los pares de color cumplen 4.5:1 para texto y 3:1 para bordes de controles en ambos temas.

## 3. Tipografía

- **Caprasimo** solo para títulos de pantalla (28) y de sección (20), la barra superior de subpantallas (20) y la marca "Sage". Nunca en botones, cifras ni texto corrido.
- **IBM Plex Sans** para todo lo demás, con cifras tabulares (`tnum`) para que los montos se alineen.
- **IBM Plex Mono** para etiquetas de ejes de gráficos.
- Las pantallas eligen un rol de `LocalSageType.current` (`titleScreen`, `titleCard`, `body`, `amountHero`…); no se escriben tamaños sueltos. Los roles de Material (`typography.bodyLarge`, etc.) apuntan a estos mismos estilos.
- Una sola cifra `amountHero` por pantalla (saldo total, gastado del mes, costo mensual…).

Licencia: IBM Plex se distribuye con la licencia OFL (`finance_app/licenses/IBM-Plex-OFL.txt`).

## 4. Forma, espacio y elevación

- Radios: 6 (insignias), 10 (botones, campos, chips, segmentado, avatares), 16 (tarjetas, hojas, tablas). La píldora completa solo en el FAB, las pistas de progreso y los interruptores.
- Espaciado: 16 de margen lateral, 24 entre bloques, 8 entre un título de sección y su contenido, 20 de padding en la tarjeta de resumen y 16 en las demás.
- Elevación: las tarjetas no tienen sombra, solo borde de 1 dp. Llevan sombra únicamente lo que flota: FAB, barra inferior, hojas y diálogos.
- Objetivos táctiles de 48 dp como mínimo (filas de lista de 56–64 dp).

## 5. Componentes (`ui/components/`)

| Componente | Archivo | Notas |
|---|---|---|
| `FinanceCard` (`BentoTileSize.Hero/Medium/Small`) | `Cards.kt` | Superficie + borde, radio 16. Hero = tarjeta de resumen. |
| `SectionHeader`, `Overline`, `TitledSection` | `Cards.kt`, `ListRows.kt` | Título Caprasimo 20; etiqueta en mayúsculas sobre cifras. |
| `SageButton` (`Primary`, `Secondary`, `Ghost`, `Danger`) | `Buttons.kt` | Alto 48 (36 compacto). Una sola acción primaria por pantalla. |
| `AddFab` | `Buttons.kt` | Círculo de 56 con "+", su `label` es la descripción accesible. |
| `SageTextField`, `MoneyField`, `PickerField`, `DateField` | `MoneyField.kt`, `PickerField.kt`, `DateField.kt` | Etiqueta arriba (nunca flotante), fondo hundido, error con icono. |
| `SegmentedControl`, `SageChip`, `SelectorPill`, `SageBadge` | `Selection.kt`, `SelectorPill.kt` | Segmentado para opciones excluyentes; chips para filtros. |
| `GroupCard`, `RowDivider`, `ListRow`, `MenuRow`, `SwitchRow`, `RadioRow` | `ListRows.kt` | Listas dentro de una tarjeta con divisores finos. |
| `CategoryAvatar`, `KindAvatar`, `IconAvatar` (`AvatarTone`) | `Avatars.kt` | Cuadrado de 40, radio 10, fondo suave y glifo en el color pleno. |
| `ProgressBar` (`ProgressTone`), `BudgetProgressItem` | `Progress.kt`, `BudgetProgressCard.kt` | Pista de 8 dp; acento, aviso o gasto según el estado. |
| `EmptyState`, `LoadingState`, `ErrorState`, `InlineNotice`, `FieldError` | `Feedback.kt` | Estado vacío con acción opcional, esqueleto de carga, error con reintentar. |
| `ScreenList`, `ScreenColumn`, `FabClearance` | `ScreenLayout.kt` | Ritmo de página estándar. |
| `MainTabHeader`, `SubScreenHeader`, `SheetHeader` | `MainTabHeader.kt` | Barra de 56 dp; pestañas con título Caprasimo 28. |
| Barra inferior y rail | `navigation/AppNav.kt` | Píldora de 56×28 en `primary-soft` para la pestaña activa. |

## 6. Hacer y no hacer

Hacer:

- Poner el signo a todo monto con dirección (+ ingreso, − gasto).
- Usar `EmptyState` con la acción que lo resuelve en vez de dejar una pantalla en blanco.
- Anunciar a TalkBack lo que el color comunica (estado del día en el calendario, insignias de presupuesto, filas elegidas).
- Mostrar `LoadingState` mientras la primera lectura no ha llegado, en vez de un "vacío" fugaz.

No hacer:

- Sombras en tarjetas, degradados o vidrio.
- Caprasimo en botones o cifras.
- Tamaños de letra o `dp` sueltos en las pantallas: usar `LocalSageType`, `LocalSpacing` y `SageRadius`.
- Rojo de alarma para gastos: el gasto es terracota oscuro; el rojo (`danger`) es solo para errores y borrar.
