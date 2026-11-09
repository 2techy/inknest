# Set up InkNest

[Tiếng Việt](../vi/setup.md) · [Documentation](README.md)

Build/install an APK using [development](development.md) or [release signing](release.md), open InkNest and choose E-Ink or Standard display mode. Setup is optional;
Skip setup opens Home. You can revisit it from Settings → About → Review setup.

## Connect KOReader

1. Install KOReader separately and open a book once so its settings directory exists.
2. In InkNest, open Settings → Reading & data. Select the KOReader app.
3. Select the KOReader data folder containing `settings.reader.lua` or `history.lua`.
4. Use Add book folder to select one or more Books folders; subfolders are included.
   Tap an existing folder to replace it, or remove it from InkNest without deleting files.
   Overlapping Books folders are rejected because a parent already includes its children.
   Android grants read access only to each selected tree. These controls are also in setup.
5. Refresh. Library includes supported files that have never been opened. Home's current
   book and reading status come from KOReader; Statistics requires its reading-statistics DB.

Paths differ by installation. Choose them through the system picker; InkNest does not assume
a fixed `/koreader`, `/webdav` or Downloads path. Arbitrary SD/cloud document providers are
not currently supported. Do not choose a broad storage root to work around missing data.

Library combines all Books folders by default. Its folder menu filters one root and combines
with search, reading status and sorting. Refresh scans every configured root; an unavailable
root keeps cached books while other roots continue. If permission is lost, choose that folder again.

Older development builds may use a legacy Books-folder configuration key. After updating
from those builds, reselect Books folders; the KOReader data folder, pins and display preferences
are stored separately.
A failed refresh preserves cached content.
Clear reading cache rebuilds local reading data and covers without changing pins or appearance.
Disconnect removes source configuration and local reading cache, not book files or KOReader data.

## Reading and launcher behavior

Tap a book in Library to open it directly in the selected KOReader app; long-press for details.
Home uses the same original-document opening flow.
If a file, permission or handler is unavailable, InkNest reports the error instead of opening
KOReader without the selected book. Dispatching an Android intent is not proof that the reader
opened the correct book; reading state is refreshed from KOReader when returning.

Default launcher is optional in Settings → Launcher. To switch back, use Android Settings →
Apps → Default apps → Home app and choose the previous launcher. Names vary by firmware.
HOME returns to InkNest Home when InkNest owns the Home role. Apps and pins remain available
without a reading source.

E-Ink mode grayscales app icons; Standard preserves their colors. Both keep static transitions.
Appearance groups Language and Display mode under General; both open a selection dialog.
The Home sections group can hide mini stats and pins without deleting pins. Device-wide font size controls
text. Firmware refresh settings remain managed by the device, not InkNest.

## App language

Settings → Appearance → Language offers System default, English and Tiếng Việt.
System default follows Android's locale list; unsupported languages fall back to English.
The names English and Tiếng Việt remain in their own languages. Onboarding follows the
selected app language without a separate language step. Changing language recreates the
Activity while retaining the current Settings page; open popups close.

InkNest uses AndroidX AppCompat per-app locales, not a second DataStore preference.
Android 13 and later share the selection with Android's app-language settings. Older
Android versions use AppCompat's automatic locale storage. This storage can produce
StrictMode disk-access diagnostics in debug on Android 12 and earlier, as documented by
[Android](https://developer.android.com/guide/topics/resources/app-languages).

English resources are the default; Vietnamese resources live in `values-vi`.
Only en/vi locale resources are packaged, including dependencies, and App Bundle language
splitting is disabled so both choices stay available offline. Date labels,
chart captions and number formatting use the UI locale. Language changes do not change
timezone, stored reading dates or streak calculations. Book metadata, external app names
and third-party license text remain supplied by their sources. Installed app metadata is
reloaded after effective UI-locale changes; browsing books does not need a new source scan.
