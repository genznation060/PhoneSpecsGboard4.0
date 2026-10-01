# Phone Specs → Gboard

Android utility for the phone-spec comparison workflow.

## What it does
1. Paste an Excel/Google Sheets TSV table.
2. Generate columns.
3. Select one phone/spec column.
4. Set the starting spreadsheet row.
5. Tap LOAD ALL TO GBOARD.
6. The app places each cell into Android's clipboard one at a time with a configurable delay (minimum 500 ms).
7. Open Gboard Clipboard and use each item separately in InShot.

## Important
This app writes to Android's system clipboard. It does not directly control Gboard. Gboard must have Clipboard enabled and may impose its own history size/retention rules. For best reliability, use a delay around 1000–1500 ms.

## Build
Open this folder in Android Studio and build/install the `app` module.
