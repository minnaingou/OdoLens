# AGENTS.md — OdoLens

Android app (Kotlin, Jetpack Compose, single-activity, Navigation3) for logging trips/fuel
economy and a parking timer with notifications, built on ML Kit OCR + Gemini AI.

## Build & test

- Build debug APK: `./gradlew :app:assembleDebug`
- Unit tests: `./gradlew :app:testDebugUnitTest` (21 tests)
- Test caveat: the ParkingViewModel runs an infinite countdown loop on `viewModelScope`.
  In its unit tests, use `UnconfinedTestDispatcher` as Main and cancel `viewModelScope`
  before the test returns — never `advanceUntilIdle()` (it never idles → hang).

## Layout (`app/src/main/java/com/mndublo/odolens/`)

- `MainActivity.kt` / `Navigation.kt` / `NavigationKeys.kt` — entry, Navigation3 graph, and
  deep-link signals that flow into screens: `openExtendSheet`, `openParkingTab`, `autoScanTarget`
- `api/GeminiClient.kt` — Gemini REST calls (parking-ticket parse, dashboard parse; each tries a
  list of models in order)
- `data/` — persistence + pure domain:
  - `SettingsRepository` (DataStore) implements narrow interfaces: `ParkingSettingsSource`,
    `DashboardSettingsSource`, `SettingsSource`
  - `TripRepository` implements `TripStore` (JSON file in filesDir)
  - `ParkingTimerPlanner` — PURE domain: 12h cap, expiry math, countdown formatting
    (no Android imports; unit-tested)
  - `Models.kt`, `TimeFormatter.kt`, `AppLogger.kt`
- `notification/` — OS alarms + notifications: `NotificationHelper`, `ParkingTimerManager`
  (extend/cancel timer; delegates math to the planner), receivers (`ParkingAlarmReceiver`,
  `ParkingExtendReceiver`, `ParkingExpiredReceiver`)
- `ocr/OCRManager.kt` — ML Kit text recognition + dashboard regex parsing (`DashboardData`)
- `ui/common/` — SHARED components used by multiple screens: `ImageSourceButtons`,
  `ScanCameraOverlay`, `ImageUtils.loadBitmapFromUri`, `rememberFabVisibility`
- `ui/main/MainScreen.kt` — bottom-nav tab host (Trips / Parking / Settings)
- `ui/{dashboard,parking,settings}/` — per feature: one `*ViewModel` (state + events) +
  thin `*Screen` shell + screen-scoped `*Card` / `*Section` components
- `ui/camera/CameraScreen.kt` — CameraX `CameraView` (full-screen capture)
- `ui/permission/` — first-launch notification-permission onboarding screen

## Conventions

- MVVM: composables are stateless shells; logic lives in ViewModels (`MutableStateFlow`
  state; one-shot UI feedback via boolean flags consumed by the screen, e.g.
  `alarmJustScheduled`, `scanFeedback`).
- Testable seams: ViewModels depend on narrow interfaces (settings/trip stores, alarm
  scheduler, parsers) and are unit-tested with fakes. `Bitmap` parameters are nullable in
  pipeline seams so the logic is JVM-testable — the UI always passes non-null.
- Pure rules (cap math, expiry, countdown) belong in `ParkingTimerPlanner` — no Android deps.
- User-facing strings go in `res/values/strings.xml`, referenced via `stringResource` /
  `context.getString` (resolve outside non-composable lambdas).
- The parking timer is persisted in DataStore so it survives tab switches — ViewModels
  re-read it via collectors; never hold timer state in memory only.
