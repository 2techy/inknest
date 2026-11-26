# Set up InkNest

[Tiếng Việt](../vi/setup.md) · [Documentation](README.md)

Install and configure InkNest, manage updates and recover access to your reading folders.

Download the APK from the [latest release](https://github.com/2techy/inknest/releases/latest), or [build from source](development.md). Open InkNest and choose **Grayscale** or **Color**. Select **Skip setup** to go straight to Home.

In the reading step, InkNest checks for KOReader. When it finds the app, choose its data folder to connect both. You can add just Books folders, set up reading later, and finish with your current launcher. To return, open Settings → About → Review setup.

## Connect KOReader

1. Install KOReader separately and open a book once to create its settings directory.
2. In setup, check for the KOReader status. In Settings → Reading & data, select KOReader if it is available and has not been selected.
3. Select its data folder containing `settings.reader.lua` or `history.lua`.
4. Add one or more book folders. Subfolders are included. Tap a folder and select **Choose folder again** to replace it, or **Remove folder** to remove it from InkNest. Overlapping roots are rejected.
5. Refresh. Library includes recognized books even if never opened; current reading/status comes from KOReader. Statistics requires `settings/statistics.sqlite3`.

Choose folders through Android's picker. Only primary internal storage is supported; do not choose a broad storage root to bypass missing access. Paths vary by installation.

| Problem or action | Result / recovery |
| --- | --- |
| Lost permission | Select the affected folder again. |
| A root cannot refresh | Its cached books remain; other roots continue updating. |
| Failed refresh | Cached content remains available with an error. |
| Clear reading cache | Rebuilds reading data and covers; preserves pins and appearance. |
| Disconnect | Removes source config/cache/grants, without deleting books or KOReader data. |

Library combines Books folders; its folder menu filters one root. Refresh scans all roots. See [Library](library.md) and [Statistics](statistics.md).

## Reading and launcher behavior

- Tap or long-press a Library book to view details, then use the reading button to open KOReader. Home opens the current book directly. Missing files, grants or handlers produce an error; reading state refreshes from KOReader on return.
- Set the default Home app in Settings → Launcher. To switch back, use Android Settings → Apps → Default apps → Home app; names vary by firmware. HOME returns to InkNest Home when it owns that role. Apps/pins work without reading sources.
- In Apps, tap an app to open it or touch and hold it to manage Home pins or open **App info** in Android Settings. Pinned apps appear first in Home order. Search includes every app and matches names and packages.
- Home’s **Quick access** shows up to four pinned apps in one row. **See all** next to its title opens Apps, where all pins are listed.
- **Grayscale** uses neutral controls and desaturates app icons and book covers. **Color** adds ink-blue accents and keeps original icon and cover colors. Both use static transitions. New installs default to Grayscale. Saved E-Ink and Standard choices carry over as Grayscale and Color, respectively. Preview both modes in setup or Settings → Appearance → Display mode. Appearance also controls Home sections, including mini stats, reading rhythm and pins; hiding pins does not delete them. Font size follows Android; panel refresh remains controlled by firmware.

## App updates

Open Settings → About → App updates. Without a saved successful result, opening the page checks for updates; otherwise it retains that result. Official release builds can check automatically at most every 24 hours while InkNest is open; this is optional. Checking/downloading needs internet; reading and launcher features work offline.

1. Choose **Check for updates**, then **Download update** when available. Expand **What’s new** to read release notes; links open in your browser.
2. Keep InkNest open until preparation finishes. Leaving cancels an unfinished download; **Cancel** also stops it.
3. Choose **Install update**. If Android requests permission to install from InkNest, grant it, return and choose Install again. Confirm installation in Android.

A completed APK remains available if installation is cancelled and is verified again before retry. Checksum, package, version, Android compatibility and signature must pass. Network errors retain the previous check result/time. The GitHub link opens the release page. For local-build testing, see [development](development.md).

## App language

Settings → Appearance → Language offers System default, English and Tiếng Việt. Unsupported system languages fall back to English. Both languages are available offline; Android 13+ also exposes the choice in system app-language settings.

Changing language retains the current Settings page and closes popups. Dates/numbers follow the UI locale; timezone, reading dates and streak rules do not change. Book metadata, external app names and license text remain as supplied. See [architecture](architecture.md) for locale storage and debug diagnostics.
