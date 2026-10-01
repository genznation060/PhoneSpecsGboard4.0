# Phone Specs → Gboard

Android utility for the phone-spec comparison workflow.

## What it does
1. Paste an Excel/Google Sheets TSV table.
2. Generate columns.
3. Select one phone/spec column (preview shows sample values).
4. Set the starting spreadsheet row + delay.
5. Optionally save settings (delay / start row / last column).
6. Tap **LOAD ALL** to place each cell into Android's clipboard one at a time.
7. Or open **Clipboard Manager** (`devdnua.clipboard`) and create a matching category.

## New features
- **Column Preview** – shows first ~8 non-empty values of the selected column so you can verify parsing.
- **Save Settings** – remembers delay, start row and last selected column.
- **Clipboard Manager integration** – one-tap open of `devdnua.clipboard` + helper to copy the column name for creating a new Category.
- **Keyboard helpers** – Input Method picker and system Input settings.

## Important
This app writes to Android's system clipboard. It does not directly control Gboard or Clipboard Manager.  
Clipboard Manager must be installed for the open buttons to work. Direct deep-link to its Categories screen is attempted but not guaranteed (most apps do not export that Activity).

## Build
Open this folder in Android Studio and build/install the `app` module,  
or use the GitHub Actions workflow to produce a debug APK.
