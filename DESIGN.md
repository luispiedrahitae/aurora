---
name: FinanZen
description: Precise, trustworthy personal finance tracker — Swiss-modernist grid, restrained glass accents, true-black OLED dark mode.
colors:
  dark-background: "#000000"
  dark-surface-container-low: "#0A0A0C"
  dark-surface-container: "#111114"
  dark-surface-container-high: "#1A1A1E"
  dark-surface-container-highest: "#232328"
  dark-ink: "#F2F2F3"
  dark-ink-muted: "#A9AAB0"
  dark-outline: "#2E2F33"
  light-background: "#FAFAFA"
  light-surface: "#FFFFFF"
  light-surface-container-low: "#F4F4F5"
  light-surface-container: "#EDEDEF"
  light-surface-container-high: "#E4E4E7"
  light-ink: "#101012"
  light-ink-muted: "#55565C"
  light-outline: "#D4D4D8"
  accent-teal-dark: "#2DD4BF"
  accent-teal-light: "#0F766E"
  income-dark: "#59CA9A"
  income-light: "#1F7759"
  expense-dark: "#F58E9A"
  expense-light: "#994A4F"
  warning-dark: "#FBBF24"
  warning-light: "#B45309"
typography:
  display:
    fontFamily: "Inter SemiBold"
    fontSize: "48sp"
    fontWeight: 600
    lineHeight: "52sp"
    letterSpacing: "-0.02em"
  headline:
    fontFamily: "Inter SemiBold"
    fontSize: "28sp"
    fontWeight: 600
    lineHeight: "34sp"
  title:
    fontFamily: "Inter SemiBold"
    fontSize: "20sp"
    fontWeight: 600
    lineHeight: "26sp"
  body:
    fontFamily: "Inter Regular"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
    fontFeature: "tnum"
  label:
    fontFamily: "Inter Medium"
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
    backgroundColor: "{colors.dark-surface-container-high}"
    textColor: "{colors.dark-ink}"
    rounded: "{rounded.xl}"
    padding: "20dp"
  bento-tile-medium:
    backgroundColor: "{colors.dark-surface-container}"
    textColor: "{colors.dark-ink}"
    rounded: "{rounded.lg}"
    padding: "16dp"
  bento-tile-small:
    backgroundColor: "{colors.dark-surface-container-low}"
    textColor: "{colors.dark-ink}"
    rounded: "{rounded.md}"
    padding: "12dp"
  nav-bar-glass:
    backgroundColor: "{colors.dark-surface-container-highest}"
    textColor: "{colors.dark-ink-muted}"
    rounded: "{rounded.sm}"
---

# Design System: FinanZen

## 1. Overview

**Creative North Star: "The Instrument Panel"**

FinanZen reads like a well-made instrument, not a consumer app performing friendliness. Swiss Modernism 2.0 is the base: a precise grid, a tight typographic scale, and color spent only where it carries meaning. On top of that restraint, one deliberate modern gesture — a frosted-glass surface reserved for the single most important number on each screen — and a Bento Box Grid that gives the dashboard real dimensional hierarchy instead of a flat scroll of identical cards. Dark, true-black OLED is the primary experience; a fully accessible light mode is a first-class sibling, not an afterthought bolted on.

This system explicitly rejects: gamified/playful consumer-fintech visuals (badges, mascots, confetti), generic gradient-hero SaaS-dashboard boilerplate, shame-based or alarming treatment of overspend, and glassmorphism used as decorative wallpaper rather than a hierarchy signal.

**Key Characteristics:**
- True black (`#000000`) OLED background with tonal elevation tiers, not a tinted near-black
- One frosted-glass surface per screen, reserved for the most important tile — never the default material
- Bento grid on data-dense screens (Dashboard, Analysis); list layouts preserved elsewhere for consistency
- Every semantic color (income/expense/warning) is paired with an icon or label — never color alone
- No drop shadows; depth comes from tonal layering, hairline borders, and restrained accent glow on interactive states

## 2. Colors

Restrained strategy: neutral tonal ramps carry almost the entire surface; one teal accent marks primary actions and current selection; three semantic colors (income/expense/warning) are fixed and never overridden by the user's accent choice.

### Primary
- **Signal Teal** (`#2DD4BF` dark / `#0F766E` light): primary actions, current selection, focus rings, glass-tile border highlight. User-selectable via 10 accent presets — this is the default; every preset follows the same lightness/contrast rules when retuned.

### Neutral
- **True Black** (`#000000`): dark-mode background — the OLED floor, not a card surface.
- **Ink Zero** (`#0A0A0C`) → **Ink Three** (`#232328`): four-step dark elevation ramp for surface-container-low/container/container-high/container-highest. Each step up = one tier closer to the user (background → list row → card → hero tile → glass tile base).
- **Paper** (`#FAFAFA`) / **Paper White** (`#FFFFFF`): light-mode background/surface — a true cool off-white, not a warm cream.
- **Paper One** (`#F4F4F5`) → **Paper Three** (`#E4E4E7`): matching light elevation ramp.
- **Ink** (`#F2F2F3` on dark / `#101012` on light): primary text.
- **Ink Muted** (`#A9AAB0` on dark / `#55565C` on light): secondary text, always ≥4.5:1 against its own surface tier, verified independently per theme.
- **Hairline** (`#2E2F33` on dark / `#D4D4D8` on light): the only border weight in the system — 1px, never used as a colored accent stripe.

### Semantic (fixed, not user-tunable)
- **Income Green** (`#59CA9A` dark / `#1F7759` light): croma/L matched to the teal accent's own restrained recipe (same L/C envelope, hue kept green) so it reads as part of the system rather than a louder color of its own. Always paired with an upward-trend icon, never color alone.
- **Expense Rose** (`#F58E9A` dark / `#994A4F` light): a rose, not an alarm red — informative, not punishing. Same croma/L match as Income Green. Always paired with a downward-trend icon.
- **Warning Amber** (`#FBBF24` dark / `#B45309` light): budget/subscription alerts. Always paired with a triangle icon and explicit copy, never a bare color wash.

### Named Rules
**The True Black Rule.** Dark-mode background is `#000000`, full stop — no tinted near-black "for warmth." Depth comes from the elevation ramp sitting on top of it, not from tinting the floor.

**The One Glass Tile Rule.** Exactly one glass-treated surface per screen — the hero tile carrying that screen's single most important number. Every other tile is flat/tonal. Glass that appears on more than one tile per screen has stopped being a hierarchy signal.

**The No-Shame Rule.** Expense and over-budget states never use alarm-red or flashing/pulsing treatment. Rose + icon + a calm number is the ceiling of emphasis.

## 3. Typography

**Body/UI Font:** Inter (with system-ui fallback)
**Character:** One family carries everything — headings, body, labels, and tabular monetary figures — with weight, not typeface changes, doing the hierarchy work. This is a product surface: no display/body pairing needed.

### Hierarchy
- **Display** (SemiBold 600, 48sp, 52sp line-height, -0.02em): the single hero number per screen (balance, net worth). Appears once, inside the glass tile.
- **Headline** (SemiBold 600, 28sp, 34sp line-height): screen titles.
- **Title** (SemiBold 600, 20sp, 26sp line-height): section headers, tile headers.
- **Body** (Regular 400, 16sp, 24sp line-height, tabular figures `tnum`): all monetary values and prose. Tabular figures are non-negotiable — money must not jitter horizontally as digits change.
- **Label** (Medium 500, 13sp, 16sp line-height, +0.02em tracking): list-row metadata, chip text, form field labels.

### Named Rules
**The One Number Rule.** Display size is reserved for exactly one number per screen — the thing the user opened the screen to check. Everything else is Headline or smaller.

## 4. Elevation

Flat-and-tonal by default, no drop shadows anywhere — shadows don't read on true black. Depth is conveyed through the four-step tonal ramp (background → container-low → container → container-high) plus a 1px hairline border, with a restrained accent-tinted glow reserved for hover/focus/selected states only (a response to interaction, never a resting decoration).

### Shadow Vocabulary
- **None at rest.** No `shadow`/`elevation` value is applied to any static surface.
- **Accent glow** (interactive only): a soft 8dp accent-colored glow at ~12% opacity, applied only on press/focus/selected — the one motion-adjacent depth cue in the system.

### Named Rules
**The Flat-By-Default Rule.** Every tile is flat at rest. Elevation is expressed by which tonal step a surface sits on, never by a shadow.

## 5. Components

### Bento Tiles (replaces generic "Cards")
- **Hero tile:** `surfaceContainerHigh`/`Paper Three`, 24dp radius, glass treatment (see below), Display-size content, spans the full grid width or 2 columns.
- **Medium tile:** `surfaceContainer`/`Paper Two`, 16dp radius, flat, Title-size header + Body content, spans 1 column on phones / half the grid on tablets.
- **Small tile:** `surfaceContainerLow`/`Paper One`, 12dp radius, flat, Label-size header, compact stat or single-row content.
- **Internal padding:** 20dp hero / 16dp medium / 12dp small, matching the `rounded` step down.

### Glass Treatment (hero tile only)
- Backdrop blur (~20dp) over `surfaceContainerHighest` at ~55% opacity, 1px hairline border, a single 1px top highlight at 8% white opacity to suggest a lit edge. Reserved per the One Glass Tile Rule — never the default tile material.

### Navigation
- Frosted `NavigationRail`/`NavigationBar`: same glass recipe as the hero tile but at lower opacity (content scrolls beneath). Active destination marked by the accent teal on icon + label, an accent-tinted pill indicator (`rounded.pill`), never a color-only change. Touch targets ≥48dp.

### Charts (Canvas-drawn)
- Income/expense/net-worth/category charts use only the fixed semantic palette (never the user's accent color, which is reserved for actions/selection). Every series is paired with a direct label or icon — color is never the sole encoding, satisfying the colorblind-safe requirement.

### Forms & Inputs
- Flat `surfaceContainer` fill, 12dp radius, hairline border at rest, accent-colored border + glow on focus. Error state uses Warning Amber text + icon beside the field, never a red-only border.

## 6. Do's and Don'ts

### Do:
- **Do** keep the dark-mode background pure `#000000` — no tinted near-black.
- **Do** reserve glass/blur for exactly one hero tile per screen.
- **Do** pair every semantic color (income/expense/warning) with an icon or label.
- **Do** verify 4.5:1 body-text contrast and 3:1 large-text contrast independently in both the dark and light themes.
- **Do** keep monetary figures on tabular (`tnum`) figures everywhere.

### Don't:
- **Don't** use gamified/playful consumer-fintech visuals — badges, mascots, confetti, streaks.
- **Don't** build a generic gradient-hero SaaS-dashboard template — no gradient text, no hero-metric-plus-gradient-accent cliché.
- **Don't** use alarm-red or flashing/pulsing treatment for overspend or negative balances — Expense Rose + icon + calm number is the ceiling.
- **Don't** apply drop shadows to any resting surface — flat-and-tonal only, glow is interaction-only.
- **Don't** apply glass/blur to more than one tile per screen, or to any non-hero surface "for texture."
- **Don't** rely on color alone anywhere charts, budgets, or status indicators appear.
- **Don't** introduce any dark pattern: manipulative nudges, guilt-driven notifications, or engagement-maximizing gamification.
