# InkNest design system

[Tiếng Việt](../vi/design-system.md) · [Documentation](README.md)

Shared rules for readable, static E-Ink interfaces. Read token values directly from [ui/theme](../../app/src/main/java/dev/twotechy/inknest/ui/theme).

## Tokens and typography

Foundation tokens (`InkSpacing`, `InkRadius`, `InkBorder`, `InkPalette`, `InkTextStyles`) feed semantic component/layout tokens and Material typography/colors/shapes. Screens reuse these definitions instead of copying dimensions.

Home branding/clock use system serif; other text uses system sans-serif. Text uses sp and follows system font scale. Use body styles for essential instructions, allow full book titles to wrap and keep app labels to two lines. The Home mark uses the original launcher artwork without adaptive-icon padding.

## Spacing and dimensions

- Reuse shared section, card, cover and control tokens; do not maintain separate per-screen copies.
- Minimum touch targets are 48×48dp. A smaller visible surface, such as ChoiceTab, must retain its larger touch bounds. Heights grow for wrapping/font scale; never shrink text to fit.
- Narrow layouts stack cover/details or metric groups. Missing optional content reserves no empty column/section.

## Layout and navigation

`AppScaffold` owns insets, gutters, content width and adaptive navigation; `ScreenScaffold` owns headers, scrolling and an optional footer. Routes do not add page padding again. Adapt to window width/font scale, never device model.

- Header actions share full touch targets: the final action is centered and a preceding Search icon aligns toward it for a compact visual gap. Search Close uses the same final column; Options uses a sliders icon. Page Back artwork aligns with the leading content edge inside a full touch target, with titles following a compact icon slot; submenu Back stays centered. Home keeps its two-row fallback when needed.
- Main order: Home → Library → Stats → Apps. Bottom navigation becomes a scrollable rail at 600dp window width. Headers align with the content gutter; main pages have no Back button.
- Settings opens from Home/configuration actions, hides main navigation and returns to the originating tab. Settings details, onboarding and Book details hide main navigation/tab swipes.
- HOME returns to Home, closes transient UI and invalidates gestures. Preserve destination/query/scroll state where supported.
- Selection uses a filled icon and top marker. If labels do not fit, accessible icons share a selected-destination caption. Scroll overflowing tabs/charts horizontally and content vertically.

## Color and images

Both modes use grayscale UI. E-Ink strengthens structural outlines and grayscales app icons; Standard uses lighter outlines and original icon colors. Informative small text stays primary/secondary. Errors and selection must remain understandable without color alone.

Transitions, menus and dialogs stay static: no decorative animation, ripple, shadow, elevation, shimmer or crossfade.

| Image cache | Contract |
| --- | --- |
| App icons | Shared RAM cache, 100 icons at 128px; keys include component/version and locale/density. IO misses use an initial-letter fallback. |
| Decoded book covers | Shared retained-allocation LRU capped at one eighth of the app heap; keys include immutable URI, display mode and 64px size buckets capped at 480px. Active references/temporary buffers are outside this budget. At most two different keys decode concurrently; duplicate requests share work independently of caller cancellation. E-Ink requests prefer RGB565, Standard uses ARGB8888; transparency may require ARGB8888. Grayscale is applied at render time. |

Home/Library share a static book placeholder and IO decoding. Clear/disconnect invalidates decoded covers and prevents stale in-flight work restoring them. No bitmap belongs in ViewModels or saved state. Disk thumbnails follow the [reading-cache contract](reading-cache.md).

## Feature hierarchy

Use shared headers, controls and tokens throughout. Keep primary actions reachable, optional content collapsible and errors/retry visible without replacing successful content. Settings navigation/toggle rows share dividers; read-only metadata has no chevron. Onboarding scrolls above a fixed footer, exposes step/progress semantics and announces save errors; saving disables navigation/completion.

Library filters and Stats periods share equal-width segments that scroll when needed. Menus reuse shared tokens, accessible touch targets and explicit selection states.

Feature behavior belongs in [setup](setup.md), [Library](library.md) and [Statistics](statistics.md), rather than a second screen specification here.

## Gestures and refresh

- `InkGestureTokens` owns thresholds: 40dp tab swipe, 56dp pull refresh. After system touch slop, lock direction when one axis exceeds the other by 1.5×. Fire once on release; cancellation/multi-touch never fires. Respect Back-edge insets and child-consumed horizontal scrolling.
- Tab swipes wrap through Home → Library → Stats → Apps, one tab per gesture. Exclude navigation, Settings, onboarding, Book details, search/IME and modal surfaces.
- Home/Library/Stats/Apps expose `RefreshAction` through `ScreenScaffold`. Pull starts at the top, including empty/short content. The static monochrome indicator overlays below the header without moving content; keep button/retry alternatives. Disable reading refresh without sources; tab changes do not refresh.

Validate wrapping, accessible targets and controls at large fonts/narrow widths. Physical contrast/ghosting requires [device acceptance](reliability.md).
