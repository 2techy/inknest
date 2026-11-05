# InkNest design system

## Structure

Use three layers, with no per-screen copies of the same dimensions:

1. Foundation: `InkSpacing`, `InkRadius`, `InkBorder`, `InkPalette`, `InkTextStyles`.
2. Semantic dimensions: `InkLayoutTokens`, `InkHeaderTokens`, `InkAppGridTokens`,
   `InkSettingsTokens`, `InkButtonTokens`, `InkNavigationTokens`, `InkReadingTokens`.
3. Layout/components: `AppScaffold`, `ScreenScaffold`, `ScreenHeader`, shared buttons,
   settings rows, app items and reading cards.

Foundation and component definitions live in `app/src/main/java/dev/twotechy/inknest/ui/theme`.
Material typography, colors and shapes are mapped from these definitions. Feature screens
consume the theme and semantic tokens rather than declaring new pixel/dp/sp constants.

## Typography

All interface text, including the Home clock and book titles, uses system sans-serif with zero
letter spacing. Font sizes and line heights use sp and follow system font scale.

| Token | Size / line height | Weight | Use |
| --- | --- | --- | --- |
| displayClock | 44 / 52 | 400 | Home clock |
| pageTitle | 22 / 28 | 600 | Main page headers and setup |
| subPageTitle | 20 / 26 | 600 | Settings details and dialogs |
| cardTitle | 18 / 24 | 600 | Book/card titles and About branding |
| sectionTitle | 16 / 22 | 600 | Section headings |
| bodyLarge | 16 / 24 | 400 | Main text and row titles |
| bodyMedium | 14 / 20 | 400 | Secondary descriptions |
| bodySmall | 12 / 16 | 400 | Package names and compact row descriptions |
| labelLarge | 14 / 20 | 500 | Buttons and controls |
| labelMedium | 12 / 16 | 500 | App grids and navigation |
| labelSmall | 11 / 16 | 400 | Nonessential captions |

Do not use caption styles for important instructions. Do not restore the old 34sp placeholder
heading or introduce serif page titles. Book titles use cardTitle. Selected navigation labels
use weight 600.

## Spacing, shape and border

| Token | dp |
| --- | --- |
| xs | 4 |
| sm | 8 |
| md | 12 |
| lg | 16 |
| xl | 20 |
| xxl | 24 |
| xxxl | 32 |
| huge | 40 |

- Header to content: 16dp.
- Between content sections: 24dp; section heading to content: 12dp.
- Card padding: 16dp; compact cards: 12dp.
- App grid gaps: 12dp horizontal, 16dp vertical.
- Related row text: 4dp; row leading icon to text: 12dp.
- Buttons in the same group: 12dp.
- Radius: small 4dp (covers), medium 8dp (controls/app frames), large 12dp (cards/dialogs).
- Border: thin 1dp (dividers), default 1.25dp (cards/buttons/inputs).

Component dimensions such as 24dp icons, 48dp touch targets, optical strokes and breakpoints
are intentionally separate from the spacing scale. Zero elevation/letter spacing need no token.
Avoid arbitrary 6/10/14/18dp padding. Prefer semantic component tokens to primitive spacing
where an existing component contract applies.

## Layout and navigation

`AppScaffold` owns system safe-area insets, adaptive bottom navigation/rail, content width and
page gutters. `ScreenScaffold` owns the page header, scrollable body and optional footer.
Feature routes must not apply the page padding a second time.

- Page gutters: 12dp below 360dp content width; 16dp below 600dp; 32dp above that.
- Content width is capped at 800dp. Page top padding is 8dp, bottom padding is 16dp.
- Main and detail headers have minimum height 56dp. Titles are left-aligned and vertically
  centered; main headers have no Back action.
- Header icons are 24dp in a real 48x48dp touch region. Back occupies that full layout slot;
  do not use a narrower slot with an overflowing touch region.
- Main tabs retain bottom navigation. At window width 600dp and above, they use a scrollable rail.
- Settings details and onboarding hide both bottom navigation and rail. Back returns to the
  parent; HOME returns to Home and closes transient UI. Setup also offers Skip.
- Navigation uses 24dp icons, 12/16sp labels and a 4dp icon/label gap. Minimum item height is
  64dp without system navigation insets; it grows if required by text.
- Selection uses the filled icon and existing top marker (32x3dp), with no selected background.
  The divider is 1dp. Do not add a second selection indicator.
- When five labels cannot fit, show accessible icon destinations and one selected-destination
  caption instead of clipping labels. The rail scrolls in short windows.

Minimum heights are not fixed heights. Let wrapping and system font scaling increase a header,
row or button. Use scrolling bodies and an accessible setup footer rather than shrinking text.
Never branch on a device model or physical screen diagonal.

## Component dimensions

| Component | Baseline |
| --- | --- |
| Action button | min 112x48dp; padding horizontal 16, vertical 12 |
| Settings row | min 64dp; vertical padding 12dp |
| Settings leading slot / icon / chevron | 40 / 24 / 20dp |
| Radio row | min 56dp |
| Toggle row | min 64dp |
| Checkbox/radio mark | 24dp; static vector drawing |
| App grid frame / icon | 48 / 36dp |
| App list frame / icon | 40 / 28dp |
| App grid cell baseline | min 84dp, scaled with font scale |
| App cell padding / icon-label gap | 4 / 8dp |
| Reading cover | 23% of available width, clamped to 64-112dp |
| Reading progress | height 4dp |
| Context menu | width 160-280dp; static, scrollable; no elevation |

Home pins and Apps share the app grid foundation and labelMedium. Home reserves two label
lines for stable rows. Long labels wrap to at most two lines with ellipsis. Book cards stack
cover/details when the remaining text width would become too small for the current font scale.

## Color and display modes

| Semantic base | Hex |
| --- | --- |
| Primary ink | #181818 |
| Secondary text | #333333 |
| Muted / strong outline | #777777 |
| Standard outline | #A0A0A0 |
| Subtle surface | #F2F2F2 |
| Paper | #FFFFFF |

Both modes keep the grayscale product palette. E-Ink uses #777777 structural outlines and
grayscale app icons. Standard uses #A0A0A0 structural outlines and original app icon colors.
Small informative text uses primary/secondary ink, never the pale outline color. Used Material
color roles, including containers, inverse surfaces and error roles, are explicitly mapped to
this palette; surface tint is transparent. The current UI has no color-only error indication.

Use static selection marks, no decorative transition/ripple/shadow, and zero dialog elevation.
Physical E-Ink sharpness/ghosting must be judged on hardware; emulator screenshots establish
layout and touch geometry only.

## Home treatment

Home uses the same header, section typography and app icon dimensions as Apps:

- Shared 56dp page header and 22/28sp title. Section headings use 16/22sp semibold.
- The 44/52sp sans-serif clock leads the overview. Reading values use 18/24sp semibold
  directly above 12/16sp captions. If the measured clock and stats cannot fit beside one
  another, stats move below the clock/date. Hidden stats leave no reserved column.
- One structural rule separates the clock overview from reading and launcher content.
- Reading setup/loading/error notices use a compact outlined row with a 14/20sp title,
  12/16sp description and 12dp padding. The whole actionable row is a touch target.
- Pinned apps use the shared 48dp outlined icon frame, 36dp artwork and 12/16sp labels.
  The section heading includes a 48dp action to open Apps. The grid keeps responsive
  columns, aligned trailing cells and two-line labels at larger font scales.
- A current-book card has one outline, an 18/24sp title and a text/chevron action beside
  the book details, with a full-width progress track below.
  No nested button or second type family is added to Home.

Unknown reading values remain unknown rather than zero. No shadows, animation or
extra tinted backgrounds are required to distinguish these sections.

## Settings hierarchy

- A page has one header. Rows use 16/24sp regular titles and 12/16sp secondary text;
  section title styles are reserved for real groups, not repeated on every value.
- Navigation/folder selection uses a row and chevron. The folder name and current path
  form one touch target rather than a heading, a value and a separate Choose/Change button.
- Read-only values use the same row spacing without a chevron. Immediate commands use
  a text row without a navigation arrow; disabled commands retain disabled semantics.
- Settings has four categories: Reading & data, Appearance, Launcher and About.
  Reading & data contains source/folders and refresh/cache sections on one page with one
  connection summary. Appearance contains display mode and Home visibility options.
  These groups do not add another level of navigation.
- Display mode descriptions belong to the radio choices, avoiding a repeated explanatory
  paragraph. The same choices are used in onboarding.
- About groups icon/name/version/author into one identity row. Debug-only tools are
  collapsed by default and remain separate from production app details.

## Compact choice tabs

Library filters and Stats units use the shared ChoiceTab component: labelMedium 12sp/16sp,
32dp minimum visible background, 12dp horizontal/8dp vertical padding, 4dp corner radius.
The enclosing interactive area remains at least 48x48dp. Text and visible height grow at
larger system font sizes. Stats chart-range labels use the same typography and 2dp selected
underline. These tokens belong to content controls; the five-tab app navigation is separate.

Heatmap adapts16–52 week columns to the full content width with square cells up to10dp,
roughly3dp horizontal/2dp vertical gaps, and a48dp weekday gutter that grows with font scale. Only empty cells
have a light0.75dp outline; active cells use the shared grayscale palette and2dp corners.
Month/weekday axes remain; week-count and Less/More captions are omitted.
