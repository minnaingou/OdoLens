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
- Live cost estimate before saving; fuel-price tracking with an "as of" date
- Historical trips list with swipe-to-delete

**Parking Timer**

- Scan a parking ticket (Thai or English) to auto-fill the start time and free duration
- Set a reminder with a warning offset (15 / 30 / 45 / 60 minutes or custom)
- Live countdown plus an OS alarm and ongoing notification with quick actions
- Extend free parking (up to a 12-hour cap), clear & reset, and the timer survives tab
  switches

**Settings**

- Gemini API key, 12/24-hour time format, theme (system / light / dark), debug log viewer

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
