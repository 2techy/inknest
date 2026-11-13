# InkNest design system

[Tiếng Việt](../vi/design-system.md) · [Documentation](README.md)

## Tokens and typography

Definitions live in [ui/theme](../../app/src/main/java/dev/twotechy/inknest/ui/theme). Foundation tokens (`InkSpacing`, `InkRadius`, `InkBorder`, `InkPalette`, `InkTextStyles`) feed semantic component/layout tokens, then shared scaffolds and controls. Material typography, colors and shapes derive from these definitions; screens reuse them rather than duplicating dimensions.

All text uses system sans-serif with zero letter spacing. Sizes and line heights use sp and follow system font scale.

| Text token | Size / line height (sp) | Weight |
| --- | --- | --- |
| displayClock | 44 / 52 | 400 |
| pageTitle | 22 / 28 | 600 |
| subPageTitle | 20 / 26 | 600 |
| cardTitle | 18 / 24 | 600 |
| sectionTitle | 16 / 22 | 600 |
| bodyLarge | 16 / 24 | 400 |
| bodyMedium | 14 / 20 | 400 |
| bodySmall | 12 / 16 | 400 |
| labelLarge | 14 / 20 | 500 |
| labelMedium | 12 / 16 | 500 |
| labelSmall | 11 / 16 | 400 |

Book titles use cardTitle; selected navigation labels use weight 600. Important instructions use body text rather than nonessential caption styles.

## Spacing and dimensions

| Spacing token | dp |
| --- | --- |
| xs | 4 |
| sm | 8 |
| md | 12 |
| lg | 16 |
| xl | 20 |
| xxl | 24 |
| xxxl | 32 |
| huge | 40 |

Header/content gap is 8dp; section gap 24dp; heading/content gap 12dp. Cards use 16dp padding, compact cards 12dp. App grid gaps are 12dp horizontal and 16dp vertical. Related text uses 4dp; icon/text and grouped buttons use 12dp. Radii are 4/8/12dp; borders are 1dp thin and 1.25dp default.

| Component | Baseline |
| --- | --- |
| Header / action touch target | min 48dp / 48×48dp |
| Action button | min 112×48dp; padding 16dp horizontal, 12dp vertical |
| Settings / radio / toggle row | min 64 / 56 / 64dp |
| Settings leading slot / icon / chevron / selection mark | 32 / 24 / 20 / 24dp |
| App grid frame / icon | 48 / 36dp |
| App list frame / icon | 40 / 28dp |
| App cell | min 84dp wide; padding 4dp; icon-label gap 8dp |
| Current-reading cover | 23% available width, clamped to 64–112dp |
| Reading progress track | 4dp |
| Context menu | 160–280dp wide; scrollable |
| ChoiceTab | min 32dp visible, min 48×48dp touch; 12/16sp medium text |

Minimum heights grow for wrapping/font scale. App labels wrap to two lines; Home reserves two lines. Reading cards stack cover/details when text width is insufficient. No bitmap belongs in ViewModels or saved state.

## Layout and navigation

`AppScaffold` owns safe-area insets, page gutters, content width and adaptive navigation. `ScreenScaffold` owns headers, scrolling and an optional footer. Routes do not add page padding again.

Gutters are 12dp below 360dp content width, 16dp below 600dp and 32dp above; content width caps at 800dp. Top/bottom padding is 4/16dp. Headers are left-aligned and vertically centered, with a full 48dp slot for Back. Main pages have no Back action.

Main destinations use bottom navigation, switching to a scrollable rail at window width 600dp. Settings details and onboarding hide navigation. HOME returns to Home and closes transient UI. Navigation uses 24dp icons, 12/16sp labels, a 4dp gap and min 64dp item height. Selection uses a filled icon and one 32×3dp top marker; there is no selected background. When labels cannot fit, accessible icons share a selected-destination caption. Layout adapts to window width and font scale, never device model.

## Color and images

| Role | Hex |
| --- | --- |
| Primary ink | #181818 |
| Secondary text | #333333 |
| Muted / strong outline | #777777 |
| Standard outline | #A0A0A0 |
| Subtle surface | #F2F2F2 |
| Paper | #FFFFFF |

Both modes use grayscale UI. E-Ink uses stronger structural outlines and grayscale app icons; Standard uses lighter outlines and original icon colors. Small informative text stays primary/secondary. Material roles, including error roles, map to this palette; tint is transparent. Errors do not depend on color alone. Selection, menus and dialogs remain static, without decorative animation, ripple, shadow or elevation.

Icons use a shared RAM cache of 100 icons at 128px, keyed by component/version and locale/density; misses load on IO with an initial-letter fallback. Home/Library share a static book-shaped placeholder and IO decoding. The decoded-cover LRU has a 4 MiB retained-allocation budget; keys include immutable URI and 64px display-size buckets capped at 480px. Active UI references and temporary buffers are outside that budget. Decodes are serialized and duplicate requests reuse results. Clear/disconnect invalidates the cache and prevents stale in-flight work from restoring it. No crossfade or shimmer is used.

## Feature hierarchy

Home orders clock overview, current book, optional reading rhythm and pins. Clock stats move below the clock when space is insufficient; hidden stats reserve no column. A structural rule separates the overview. Notices use a compact outlined row. Current reading has one outline and a full-width progress track. Home shows the first pin row in saved order; View all opens Apps with all pins accessible.

Settings has Reading & data, Appearance, Launcher and About. Rows use 16sp regular titles, 12sp summaries and 4dp separation. Section headings use 14sp medium text without decorative rules; gaps are 24/12dp. Navigation/toggle rows share thin dividers and align to page gutters. Folder name/path form one target; each Books root has a separate 48dp remove action with confirmation. Read-only metadata has no chevron. Outlined buttons perform commands.

Appearance groups Language and Display mode under General with static selection dialogs; Home sections controls mini stats, reading rhythm and pins independently. About shows branding, version/author, a compact update-status row, website/setup links and third-party licenses last. The update page leads with one status heading, then a two-column metadata grid with smaller labels and regular values. It stacks at narrow widths or large font scales, aligns to page gutters, and groups release notes and the GitHub link as consecutive Settings rows. One primary button changes between check, download, cancel and install above expandable, natively formatted release notes, with static download percentages; progress changes at most once per second. Update errors retain the previous result and check time. Each license opens a scrollable detail; loading/retry/cache are independent per component and offline.

Onboarding has three steps, a static 3dp progress strip, visible step count and progress semantics. Content scrolls above a fixed footer; the primary action and Skip have accessible targets. Reading setup reuses real source controls; launcher requests are explicit. Save errors are announced politely and saving disables navigation/completion.

Stats has Today, daily average and streak in three equal columns, then trend, activity calendar and totals. Time/Pages controls every reading value. The heatmap fits 16–52 weeks, square cells up to 10dp, 3/2dp gaps, a font-scaled 48dp weekday gutter, 0.75dp empty outlines and 2dp corners. Month/weekday labels remain; future dates are absent. Home uses the same time-only calendar. See [statistics](statistics.md) for intensity and counting semantics.

## Gestures and refresh

`InkGestureTokens` defines 40dp tab-swipe and 56dp pull-refresh thresholds. After system touch slop, direction locks when one axis exceeds the other by 1.5×. Actions fire on release; cancellation and multi-touch do not fire. System Back edge insets and child-consumed horizontal scrolling take priority.

Tab swipes follow Library → Stats → Home → Apps → Settings without wrapping. They apply only to top-level content, excluding navigation, Settings details, onboarding, search/IME and modal surfaces. Existing saveable state restores destinations; HOME invalidates active gestures.

Home, Library, Stats and Apps expose `RefreshAction` through `ScreenScaffold`. Pulling begins at the top, including empty/short content. The monochrome running indicator overlays below the header without shifting content or animating. Buttons/retry remain available. Reading refresh is disabled without sources; tab changes do not refresh. Feature content retains cache/error notices without a duplicate running label. Judge E-Ink sharpness and ghosting on physical hardware using [reliability checks](reliability.md).
