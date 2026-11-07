# Nim

A minimalist, text-only Android launcher. No icons, no widgets: a clock, your
favorites and an A–Z drawer with search.

Built with Kotlin + Jetpack Compose, for personal use.

## Features

- **Home** with clock, date and favorites at the bottom, within thumb reach.
- **A–Z drawer** (swipe up) with search that opens the keyboard right away.
  Ranks label prefix > word prefix > initials > contains, ignoring accents:
  `cam` finds Câmera, `gm` finds Google Maps.
- **Auto-launch** when the search narrows the list down to a single app.
- **Long press** on an app: favorite, app info or uninstall.
- **Work profile**: apps from the other profile get a small briefcase badge after the name.
- **Pinned shortcuts**: accepts Chrome's "Add to home screen" (PWAs) and lists
  the shortcuts in the drawer.
- **Settings** (long press on empty home space): reorder favorites by dragging
  and use Nim's background as the wallpaper.

Styled after shadcn/ui's `neutral` dark theme (the same tokens as the author's
web apps and Nim TV): background `#0A0A0A`, text `#FAFAFA`, muted `#A1A1A1`,
accent `#262626` on press, the Geist font, and cards with a 1dp border. The
home is signed "powered by gadfaria".

## Requirements

- Android 8.0+ (minSdk 26, targetSdk 37)
- JDK 17 and Android SDK 37 (Android Studio ships both)

## Build and install

```sh
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then press HOME on the device and pick Nim, or set it through adb:

```sh
adb shell cmd package set-home-activity com.gadfaria.nim/.MainActivity
```

## Tests

```sh
./gradlew :app:testDebugUnitTest
```

They cover the search ranking tiers (`data/AppSearch.kt`, plain Kotlin).

## Structure

```
app/src/main/java/com/gadfaria/nim/
├─ MainActivity.kt         # single activity, HOME intent
├─ PinShortcutActivity.kt  # handles pin shortcut requests
├─ NimViewModel.kt
├─ data/                   # LauncherApps, search, DataStore, wallpaper
└─ ui/                     # home, drawer, menu, settings, card, theme
```

Geist is licensed under the SIL Open Font License ([GEIST_LICENSE.txt](GEIST_LICENSE.txt)).

Design decisions and the pitfalls found along the way are in
[PLAN.md](PLAN.md).
