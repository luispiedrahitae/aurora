# Product

## Register

product

## Users

A single person managing their own personal finances — accounts, transactions, budgets, subscriptions, net worth — across Android, iOS, and a Windows desktop preview build (one shared Compose Multiplatform codebase, `finance_app/`). They open the app in short, frequent sessions (checking a balance, logging a transaction, glancing at monthly spend) rather than long analysis sessions. The primary job on any given screen is either a quick read (what's my balance, am I over budget) or a quick write (log this transaction, adjust this budget) — not deep exploration.

## Product Purpose

Cauce is a personal finance tracker: accounts, transactions, budgets, subscriptions, categories, and analysis (income/expense trends, net worth, spending patterns) in one local-first app. Success looks like the user trusting the numbers at a glance, without friction or judgment, and being able to act on them (log a transaction, adjust a budget) in a couple of taps.

## Brand Personality

Precise & trustworthy. The interface should feel like Linear or Stripe's dashboard: calm confidence, restrained color, the data speaks for itself rather than being dressed up. Not warm/playful (no mascot energy, no celebratory confetti for saving money), not terminal-dense (this is a personal tool for one person, not a power-user trading desk — density serves clarity, not maximalism).

## Anti-references

- Gamified/playful consumer fintech apps (badges, streaks, cutesy illustration) — money tracking is a task, not a game.
- Generic gradient-heavy SaaS dashboard templates (hero-metric-plus-gradient-accent boilerplate).
- Shame-based or alarming treatment of overspend (harsh red flashes, guilt-inducing copy) — informative, not punishing.
- Any dark pattern: manipulative nudges, guilt-driven notifications, engagement-maximizing gamification of spending/saving behavior.

## Design Principles

- Data speaks for itself — restraint over decoration; every visual flourish must earn its place.
- Precision over friendliness — a Swiss-modernist grid and typographic hierarchy over soft/playful shapes.
- Depth with purpose — glass and dimensional layering mark hierarchy (this is the important number), not decoration.
- Trust through consistency — identical components read identically everywhere; no screen invents its own affordances.
- Accessible & ethical by construction — accessibility and non-manipulative framing are load-bearing requirements, not a final pass.

## Accessibility & Inclusion

- WCAG 2.1 AA minimum, verified independently in both the OLED-dark and light themes (not inferred from one).
- Color is never the sole encoding — every chart pairs color with an icon, pattern, or direct label (colorblind-safe).
- No shame-based visual treatment of overspend/negative balances — informative framing (clear numbers, neutral-to-warning color scale) instead of alarming/punishing styling.
- No dark patterns anywhere in the interface.
