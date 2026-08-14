# OdoLens

Android app for logging trips and fuel economy, with a smart parking reminder — powered by
ML Kit OCR and Google Gemini AI.

> Note: developed with agentic-coding assistance. See `AGENTS.md` for the code layout
> conventions used by AI tooling.

## Features

**Trip Dashboard**

- Scan a car dashboard photo (camera or gallery) — ML Kit OCR reads distance and fuel
  economy; Gemini AI takes over automatically when OCR is unclear
- Log trips with distance, fuel economy, fuel price and an optional name
- Live trip cost estimate before saving; fuel-price tracking with an "as of" date
- Searchable trip history with swipe-to-edit and swipe-to-delete

**Parking Timer & Directory**

- Scan a parking ticket (Thai or English) to auto-fill the start time and free duration
- Quick-start presets ("Now", "5m ago", "15m ago", "30m ago") and time picker
- Built-in Parking Directory to save, edit, and quickly select favorite parking spots and free durations
- Alert warning offset validation (15 / 30 / 45 / 60 minutes or custom)
- Material 3 Expressive wavy countdown ring and notification alerts
- "Parking Expired" card with a live "Expired X ago" ticker until acknowledged
- Automatic end-of-day purge for stale expired sessions
- Extend free parking (up to a 12-hour cap), clear & reset, and direct notification dismissal (timer survives tab switches)

**Settings & Customization**

- Instant auto-saving preferences (theme mode, time format, API key)
- Material You Dynamic Color: match system wallpaper theme (Android 12+) alongside branded dark/light modes
- Gemini API key management with paste action and visibility toggle
- Built-in debug log viewer

## Android version support

| | |
|---|---|
| Minimum | Android 7.0 (API 24) |
| Target / compile | Android 16 (API 36) |
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |

## Tech stack

- Jetpack Compose + Material 3, Navigation3, MVVM (ViewModel + StateFlow)
- CameraX, ML Kit Text Recognition, OkHttp (Gemini REST), DataStore Preferences,
  kotlinx.serialization

## Getting started

1. Clone the repo and open it in Android Studio, or build from the command line:
   `./gradlew :app:assembleDebug`
2. Optional but recommended: get a free Gemini API key from
   [Google AI Studio](https://aistudio.google.com/) and enter it in **Settings → Gemini AI Config**.
   - Without a key, English parking tickets still parse via local regex; dashboard scans
     require the key for the AI fallback.
3. Run on a device or emulator with Android 7.0 (API 24) or newer.

## License

MIT — see [LICENSE](LICENSE).
