---
name: Sage
description: Warm, organic personal finance tracker — cream ground, terracotta/sage accents, soft-shadowed flat cards, rounded and pill shapes.
colors:
  dark-background: "#221F19"
  dark-surface-container-low: "#2E2A22"
  dark-surface-container: "#382F23"
  dark-surface-container-high: "#413B2F"
  dark-surface-container-highest: "#4A4030"
  dark-ink: "#FBF7F0"
  dark-ink-muted: "#C4B9A5"
  dark-outline: "#5A4F3C"
  light-background: "#F5EAD8"
  light-surface: "#FAF3E6"
  light-surface-container-low: "#F7EEE0"
  light-surface-container: "#F2E6D2"
  light-surface-container-high: "#ECDCC2"
  light-ink: "#201E1D"
  light-ink-muted: "#5B5450"
  light-outline: "#D8C9AE"
  accent-terracotta-dark: "#D79B74"
  accent-terracotta-light: "#C67139"
  accent-sage-dark: "#99A880"
  accent-sage-light: "#79895D"
  income-dark: "#BBC5AA"
  income-light: "#4B553A"
  expense-dark: "#E4BBA0"
  expense-light: "#7F4824"
  warning-dark: "#FBBF24"
  warning-light: "#B45309"
typography:
  display:
    fontFamily: "Figtree Bold"
    fontSize: "52sp"
    fontWeight: 700
    lineHeight: "56sp"
    letterSpacing: "-0.5sp"
  headline:
    fontFamily: "Figtree Bold (Caprasimo for the app wordmark, every screen title — main-tab and sub-screen — every in-screen section subtitle, and primary CTA/FAB button labels)"
    fontSize: "24sp"
    fontWeight: 700
    lineHeight: "34sp"
  title:
    fontFamily: "Figtree SemiBold"
    fontSize: "20sp"
    fontWeight: 600
    lineHeight: "26sp"
  body:
    fontFamily: "Figtree Regular"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
    fontFeature: "tnum"
  label:
    fontFamily: "Figtree Medium"
    fontSize: "13sp"
    fontWeight: 500
    lineHeight: "16sp"
    letterSpacing: "0.02em"
rounded:
  sm: "8dp"
  md: "12dp"
  lg: "16dp"
  xl: "24dp"
  pill: "50%"
spacing:
  xs: "4dp"
  sm: "8dp"
  md: "12dp"
  lg: "16dp"
  xl: "24dp"
  xxl: "32dp"
components:
  bento-tile-hero:
    backgroundColor: "{colors.light-surface-container-high}"
    textColor: "{colors.light-ink}"
    rounded: "{rounded.lg}"
    padding: "20dp"
  bento-tile-medium:
    backgroundColor: "{colors.light-surface-container}"
    textColor: "{colors.light-ink}"
    rounded: "{rounded.lg}"
    padding: "16dp"
  bento-tile-small:
    backgroundColor: "{colors.light-surface-container-low}"
    textColor: "{colors.light-ink}"
    rounded: "{rounded.md}"
    padding: "12dp"
  nav-bar-flat:
    backgroundColor: "{colors.light-surface}"
    textColor: "{colors.light-ink-muted}"
    rounded: "{rounded.sm}"
---

# Design System: Sage

## 1. Overview

**Creative North Star: "Organic Warmth"**

Sage reads like a well-kept notebook, not an instrument panel. Where the prior system (Instrument Panel / Swiss Modernism 2.0) built precision through cold neutrals and true black, Sage builds trust through warmth: a cream ground, two earthy accents (terracotta for spending, sage for growth), generous rounding, and soft, low-opacity shadows instead of glass or hairline borders. The grid discipline and one-hero-number restraint carry over unchanged — this is still a precision tool, just one that feels handled rather than engineered.

This system explicitly rejects: gamified/playful consumer-fintech visuals (badges, mascots, confetti), generic gradient-hero SaaS-dashboard boilerplate, shame-based or alarming treatment of overspend, and glassmorphism/translucency as a decorative material — Sage's depth comes from soft shadows and flat tonal layering, never blur.

**Key Characteristics:**
- Warm cream (`#F5EAD8`) light background and a warm near-black organic dark (`#221F19`, not pure OLED black) — neither ground is a cold neutral
- Two named accents, terracotta and sage, each with a full 100–900 tonal ramp; the user picks one as primary, the other is always available as its semantic opposite (income/expense can be flipped between them)
- Flat cards with soft, low-opacity shadows (`shadow-sm/md/lg`) — no glass, no blur, no translucency anywhere
- 16px base corner radius on cards/containers, full pill (999px/50%) on buttons, avatars, tags, and chips
- Thin (1.75 stroke-width) Lucide-style line icons, rounded caps/joins, replacing the prior filled/outlined Material icon language for all UI chrome
- Every semantic color (income/expense/warning) is paired with an icon or label — never color alone

## 2. Colors

Restrained-but-warm strategy: a cream/organic-dark neutral ramp carries most of the surface; terracotta and sage are the only two accents in the system, and income/expense map onto them directly (swappable via a single flip, never a third semantic hue).

### Primary accents
- **Terracotta** (`#C67139` light / `#D79B74` dark, 100–900 ramp): the default primary accent — buttons, current selection, focus rings, expense amounts at the 700 step. Warm, earthbound, never a cold "danger" red.
- **Sage** (`#79895D` light / `#99A880` dark, 100–900 ramp): the second accent — income amounts at the 700 step, and selectable as the user's primary accent from the Apariencia screen (in which case terracotta becomes the "flip" target instead).

### Neutral
- **Cream** (`#F5EAD8`): light-mode background — warm, not a cold off-white.
- **Cream Surface** (`#FAF3E6`) → **Cream Three** (`#ECDCC2`): light elevation ramp, each step a touch more saturated/darker cream.
- **Organic Black** (`#221F19`): dark-mode background — a warm near-black, deliberately not pure `#000000` OLED.
- **Char One** (`#2E2A22`) → **Char Three** (`#4A4030`): matching dark elevation ramp.
- **Ink** (`#201E1D` on light / `#FBF7F0` on dark): primary text.
- **Ink Muted** (`#5B5450` on light / `#C4B9A5` on dark): secondary text, always ≥4.5:1 against its own surface tier, verified independently per theme.
- **Sand Line** (`#D8C9AE` on light / `#5A4F3C` on dark): the only border weight in the system — 1px, never used as a colored accent stripe.

### Semantic (fixed, not user-tunable independent of accent choice)
- **Income** (sage 700: `#4B553A` light / sage 300: `#BBC5AA` dark): always paired with an upward-trend icon, never color alone.
- **Expense** (terracotta 700: `#7F4824` light / terracotta 300: `#E4BBA0` dark): informative, not punishing — never an alarm red. Always paired with a downward-trend icon.
- **Warning Amber** (`#FBBF24` dark / `#B45309` light): budget/subscription alerts. Always paired with a triangle icon and explicit copy, never a bare color wash.

### Category palette
Twelve hues, all within the warm half of the wheel (terracotta → ochre → olive → moss → fern → amber → sienna → dark olive → dusty rose → light terracotta → rose → clay) — no blues, violets, or cyans, so every category reads as a cousin of the accent/semantic colors rather than a clashing primary. An earlier version spread all 12 evenly around the full 360° wheel (including teal/slate/denim/plum/mauve) and only lightly blended them toward a warm neutral; those cool hues still read as an off-theme rainbow on screen, so the palette was narrowed to warm-only hues instead. Selected by hashing the category name; never the sole encoding — every chart/row pairs its color with a name or icon.

### Named Rules
**The Organic Warmth Rule.** Neither theme's ground is a cold neutral or pure black/white. Light is cream, dark is a warm near-black. Warmth is carried by the ground itself, not bolted on via a tint filter.

**The Soft Shadow, No Glass Rule.** Depth comes from a soft, low-opacity shadow (`shadow-sm/md/lg`) and flat tonal layering — never blur, translucency, or a frosted-glass material. Exactly zero glass surfaces in this system (the prior "One Glass Tile Rule" is retired, not relaxed).

**The No-Shame Rule.** Expense and over-budget states never use alarm-red or flashing/pulsing treatment. Terracotta + icon + a calm number is the ceiling of emphasis.

## 3. Typography

**Display/Headline/Title/Body/Label Font:** Figtree (Regular/Medium/SemiBold/Bold, with system-ui fallback) for all UI chrome, body copy, and data — a single family throughout the interface.
**Character:** Sage's own handoff prototype (`Sage.dc.html`) renders the "Sage" wordmark and the four main-tab screen titles (Resumen/Movimientos/Cuentas/Presupuesto) in **Caprasimo**, a bold slab-serif display face — visible in the handoff screenshots and confirmed by the handoff `README.md` ("Type: Caprasimo (display/headings) + Figtree (body)"). The handoff screenshots also show it on button labels — the FAB ("+ Movimiento"/"+ Cuenta"/etc.) and secondary pill buttons ("Retirar / Cerrar" in `07-inversiones.png`) — so it isn't confined to headings; everything else in the interface (subtitles, body, monetary figures) stays on Figtree. (An earlier pass added Caprasimo, then dropped it after a fidelity check that only inspected the handoff's inline HTML — which doesn't declare `font-family` because it inherits from an external stylesheet not included in the handoff bundle — instead of the rendered screenshots. That check was wrong; Caprasimo is restored for the wordmark, the four screen titles, and button labels.)

### Hierarchy
- **Display** (Figtree Bold, 52sp, 56sp line-height, -0.5sp tracking): the single hero number per screen (balance, net worth). Appears once.
- **Headline** (Figtree Bold, 24sp, 34sp line-height): screen titles. The app wordmark ("Sage") and every screen title use Caprasimo instead of Figtree at this size — the 5 main-tab screens (Resumen/Movimientos/Cuentas/Presupuesto/Más, `MainTabHeader`) and every sub-screen/sheet (`SubScreenHeader`, `SheetHeader`) alike, so the expressive treatment reads as one system rather than stopping at the tab bar. Also used, at label size, on primary CTA/FAB button labels (see `AddFab`).
- **Title** (Figtree SemiBold, 20sp, 26sp line-height): sub-section headers, tile headers.
- **Section label** (Caprasimo/display font, 15sp, uppercase, +0.04em tracking, muted ink): small-caps subheads above a group of rows ("Saldos por cuenta", "Gastos por categoría", "Tarjetas de crédito"). Not a scale role (no other text uses 15sp) — calibrated by measuring cap-height in `screenshots/03-cuentas.png` against the H1 "Cuentas" (22px, known) in the same image: ~15px cap-height at 22px font-size (ratio 0.68) vs. ~8–9px cap-height for the h6 "TARJETAS DE CRÉDITO", implying a mockup font-size of ~12–13px — Title size (20sp) reads far too large on-device, and Label size (13sp) reads too small on-device (Caprasimo has less cap-height per point than Figtree, so equal nominal sizes don't look equally large across the two families). The handoff renders these in sentence case in its source markup but transforms them to uppercase via CSS; match the rendered (uppercase, display-font) result, not the source markup.
- **Body** (Figtree Regular, 16sp, 24sp line-height, tabular figures `tnum`): all monetary values and prose. Tabular figures are non-negotiable — money must not jitter horizontally as digits change.
- **Label** (Figtree Medium, 13sp, 16sp line-height, +0.02em tracking): list-row metadata, chip text, form field labels.

### Named Rules
**The One Number Rule.** Display size is reserved for exactly one number per screen — the thing the user opened the screen to check. Everything else is Headline or smaller.

## 4. Elevation

Flat-and-tonal with soft shadows, no glass or blur anywhere. Depth is conveyed through the elevation ramp (background → container-low → container → container-high) plus a soft, low-opacity shadow on cards that need to visually lift off the page — never a hairline border alone, and never translucency.

### Shadow Vocabulary
- **`shadow-sm`**: list rows, small tiles — a barely-there lift.
- **`shadow-md`**: medium cards, the default resting card treatment.
- **`shadow-lg`**: hero cards (the balance card, a modal sheet) — the most lift in the system, still soft and low-opacity, never a hard drop shadow.

### Named Rules
**The Flat-By-Default Rule.** Every tile is flat in the sense of no gradients, no glass, no beveling — but unlike the prior OLED system, a soft shadow (not a shadow-free hairline) is the default resting treatment here, sized by the shadow vocabulary above.

## 5. Components

### Bento Tiles (cards)
- **Hero tile:** `surfaceContainerHigh`/Cream Three, 16dp radius, `shadow-lg`, Display-size content, spans the full grid width or 2 columns.
- **Medium tile:** `surfaceContainer`/Cream Two, 16dp radius, `shadow-md`, Title-size header + Body content, spans 1 column on phones / half the grid on tablets.
- **Small tile:** `surfaceContainerLow`/Cream One, 16dp radius, `shadow-sm`, Label-size header, compact stat or single-row content.
- **Internal padding:** 20dp hero / 16dp medium / 12dp small.
- Cards converge on the 16dp base radius rather than escalating radius by tile size — Sage's flatter card language reads as one consistent shape family, with size and shadow (not radius) doing the hierarchy work.

### Navigation
- Flat `NavigationRail`/`NavigationBar` on `surface`, no glass. Active destination marked by a colored pill/chip behind the icon (accent-tinted background, `rounded.pill`) plus label color — never a color-only change. Touch targets ≥48dp.

### Charts (Canvas-drawn)
- Income/expense/net-worth/category charts use only the fixed semantic palette (sage = income, terracotta = expense) unless the user has flipped them, never an arbitrary accent. Every series is paired with a direct label or icon — color is never the sole encoding, satisfying the colorblind-safe requirement.

### Forms & Inputs
- Flat `surfaceContainer` fill, 16dp radius, sand-line border at rest, accent-colored border on focus. Error state uses Warning Amber text + icon beside the field, never a red-only border.

### Icons
- Lucide-style, stroke-width 1.75, rounded caps/joins, monochrome (tinted via the surrounding text/icon color) — replaces filled/outlined Material icons for all UI chrome (nav, headers, actions, dialogs, keypad). Category glyphs keep their existing Material-icon-in-a-colored-circle treatment (out of scope for the stroke-icon system — too small for the fill/stroke distinction to read).

## 6. Do's and Don'ts

### Do:
- **Do** keep both grounds warm — cream in light, organic near-black in dark — never a cold neutral or pure `#000000`/`#FFFFFF`.
- **Do** use a soft shadow (`shadow-sm/md/lg`) as the default resting depth cue for cards.
- **Do** pair every semantic color (income/expense/warning) with an icon or label.
- **Do** verify 4.5:1 body-text contrast and 3:1 large-text contrast independently in both the dark and light themes.
- **Do** keep monetary figures on tabular (`tnum`) figures everywhere.

### Don't:
- **Don't** use gamified/playful consumer-fintech visuals — badges, mascots, confetti, streaks.
- **Don't** build a generic gradient-hero SaaS-dashboard template — no gradient text, no hero-metric-plus-gradient-accent cliché.
- **Don't** use alarm-red or flashing/pulsing treatment for overspend or negative balances — Expense (terracotta) + icon + calm number is the ceiling.
- **Don't** apply glass, blur, or translucency anywhere in the system — that material belongs to the retired prior design language, not this one.
- **Don't** rely on color alone anywhere charts, budgets, or status indicators appear.
- **Don't** introduce any dark pattern: manipulative nudges, guilt-driven notifications, or engagement-maximizing gamification.
