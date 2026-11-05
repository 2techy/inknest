# Set up InkNest

Install the APK, open InkNest and choose E-Ink or Standard display mode. Setup is optional;
Skip setup opens Home. You can revisit it from Settings → About → Review setup.

## Connect KOReader

1. Install KOReader separately and open a book once so its settings directory exists.
2. In InkNest, open Settings → Reading & data. Select the KOReader app.
3. Select the KOReader data folder containing `settings.reader.lua` or `history.lua`.
4. Select Books independently. It must contain the books you want on the shelf; subfolders
   are included. Android's picker grants read access only to the selected trees.
5. Refresh. Library includes supported files that have never been opened. Home's current
   book and reading status come from KOReader; Statistics requires its reading-statistics DB.

Paths differ by installation. Choose them through the system picker; InkNest does not assume
a fixed `/koreader`, `/webdav` or Downloads path. Arbitrary SD/cloud document providers are
not currently supported. Do not choose a broad storage root to work around missing data.

If permission is lost, choose the folder again. A failed refresh preserves cached content.
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
Appearance can hide mini stats and pins without deleting pins. Device-wide font size controls
text. Firmware refresh settings remain managed by the device, not InkNest.
