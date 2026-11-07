# Nim — Minimalist Android launcher

> MVP plan for personal use. Written on 2026-08-27.

**Chosen stack:** Kotlin + Jetpack Compose
**MVP scope:** minimalist home with favorites + A–Z drawer with search
**Environment:** Android Studio + emulator

---

## The design decision that carries the rest

A truly minimalist launcher = **no icons**. Text only.

This isn't just aesthetics: it removes icon loading and caching, adaptive icons,
monochrome themes and the whole bundle of drawer performance pain. The app list
becomes `String` + `ComponentName`, and the UI is trivial in Compose.

If you want icons later, they can be added. Starting with them is expensive.

---

## Stack

| Piece | Choice | Why |
|---|---|---|
| UI | Compose (latest BOM) | Single Activity, zero layout XML |
| App list | `LauncherApps` | Better than `PackageManager.queryIntentActivities` — gives work profiles, install/uninstall callbacks and `startMainActivity` with the right origin |
| State | ViewModel + `StateFlow` | No extra library |
| Persistence | DataStore (Preferences) | Favorites and settings are half a dozen keys; Room is overkill |
| DI | None | Manual. Hilt in an 8-file app is dead weight |
| minSdk / targetSdk | 26 / 37 | API 37 (Android 17) is the one installed — see "Current environment state" |

---

## The details that make it "be a launcher"

Nim is not a regular app. The first two items live in `AndroidManifest.xml` and
decide whether it works at all; the other two decide how it looks.

### 1. HOME intent filter

```xml
<activity
    android:name=".MainActivity"
    android:launchMode="singleTask"
    android:stateNotNeeded="true"
    android:excludeFromRecents="true"
    android:screenOrientation="portrait"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

### 2. `<queries>` — gotcha #1

Since API 30 Android hides installed apps. Without declaring this, your list
comes back **empty with no error at all**:

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent>
</queries>
```

### 3. Solid background — decision revised in M1

The original plan was `windowShowWallpaper=true` with a transparent background.
**It was dropped:** Nim has a solid dark background, no wallpaper.

This isn't just preference — it erases a whole class of problems. With the
wallpaper behind it, text color can't come from `isSystemInDarkTheme()` (what's
behind the text is the *image*, not the system theme), and the correct
alternative was to read `WallpaperManager.getWallpaperColors()`, register an
`OnColorsChangedListener`, and still put a shadow on all typography because the
color hint is global and a high-contrast patch swallows the text. All of that
left the project. Contrast is now known at compile time.

```xml
<item name="android:windowShowWallpaper">false</item>
<item name="android:windowBackground">@color/nim_background</item>
```

`windowBackground` must be the **same color** as in Compose, otherwise launch
flashes a different color before the first frame.

### 4. The palette: shadcn/ui neutral — revised on 2026-11-07

Nim started with two colors (`#121212` background, `#E8E6E3` text, the same
tone at 60% for secondary text). It now uses the same tokens as the author's
web apps (hub, built with shadcn/ui, style `radix-nova`, base color `neutral`)
and Nim TV, so all of them share one look:

| Token | Value | Use |
|---|---|---|
| background | `#0A0A0A` | screen, cards |
| foreground | `#FAFAFA` | text |
| muted-foreground | `#A1A1A1` | date, group labels, descriptions, badges |
| accent | `#262626` | row background while pressed |
| border | white 10% | card outline, separators |
| input | white 15% | search field outline |
| ring | `#737373` | search field outline when focused |
| destructive | `#FF6467` | "Desinstalar" / "Remover atalho" |

Font: Geist (Light for the clock, Regular for rows, Medium for titles and
labels), bundled in `res/font`. Radius scale from `--radius: 0.5rem`: 6dp for
rows, 8dp for the search field, 12dp for cards.

Still no icons: hierarchy comes from size, weight and the muted color.
shadcn's hover/active state becomes touch feedback — the accent background
shows while the finger is down (`pressHighlight` in `ui/Card.kt`). In the
drawer the press is delayed until the gesture is clearly not a scroll, so
scrolling doesn't flash rows.

Settings sections sit in cards with a group label, like hub's sidebar. On the
home the favorites keep the label but drop the card border and the press
highlight, so they read as a plain list under the clock; the long-press menu is a card at the bottom with the destructive
action below a separator. The home is signed "powered by gadfaria" in the
bottom-left, as in Nim TV.

### Two more behaviors

- **Back** on the home does nothing — it must not `finish()`.
- `onNewIntent` = the user pressed HOME while already in the launcher → close
  the drawer and scroll back to the top.

---

## Screens

```
┌─────────────────┐  swipe up  ┌─────────────────┐
│                 │ ─────────► │ [ search_____ ] │
│      14:32      │            │ Calendar        │
│   Thu, Aug 27   │            │ Camera          │
│                 │            │ Chrome          │
│                 │            │ …               │
│  Messages       │ ◄───────── │ (A–Z, scroll)   │
│  Chrome         │ swipe down └─────────────────┘
│  Spotify        │
│  Camera         │   long press on any item →
└─────────────────┘   favorite · app info · uninstall
```

**Search:** normalizes (lowercase + strip accents) and ranks
**prefix > initials > contains**.
`"wpp"` finds "WhatsApp". `"cam"` finds "Câmera" before "Scanner de Câmera".

---

## File structure

```
app/src/main/java/com/gadfaria/nim/
├─ MainActivity.kt          # single activity, HOME intent
├─ data/
│  ├─ AppInfo.kt            # label, ComponentName, UserHandle
│  ├─ AppRepository.kt      # LauncherApps + install/uninstall Callback
│  └─ Settings.kt           # DataStore: favorites, prefs
├─ ui/
│  ├─ NimApp.kt             # host: home ⇄ drawer
│  ├─ HomeScreen.kt         # clock + favorites
│  ├─ DrawerScreen.kt       # search + A–Z list
│  ├─ Card.kt               # card, group label, separator, press feedback
│  └─ theme/                # shadcn tokens, Geist, radius scale
└─ NimViewModel.kt
```

---

## Milestones

| # | Deliverable | How I know it worked |
|---|---|---|
| **M0** ✅ | Android Studio + SDK + emulator (API 37) | `adb devices` lists the emulator |
| **M1** ✅ | Nim project becomes a HOME app | Press HOME on the emulator → pick Nim → my screen shows up |
| **M2** ✅ | Drawer with real apps, A–Z | Full list, tap opens the app |
| **M3** ✅ | Search | 2–3 letters filter, Enter launches the first result |
| **M4** ✅ | Favorites on home + clock | Long press favorites, persists after restart |
| **M5** ✅ | Polish: long-press menu, favorite marker, transitions | I use it daily without annoyance |

M0 is the only slow step (SDK + emulator image download, a few GB).
M1–M4 are quick.

---

## Out of the MVP (on purpose)

Widgets · folders · notifications and badges · icons · configurable gestures ·
settings backup · full settings screen.

All of that is easy once M1–M4 are up — and none of it is needed to start using
Nim as the default launcher.

---

## Current environment state

Checked on 2026-08-27 on this machine — **M0 done**:

- [x] Java / JDK — OpenJDK 25 bundled with Android Studio (`Contents/jbr`)
- [x] Android Studio 2026.1
- [x] Android SDK at `~/Library/Android/sdk`
      — platform-tools 37.0.1 · build-tools 36.0.0 · platform + sources android-37.0
      · cmdline-tools 23.0.0 · emulator 37.1.11
- [x] `adb`, `emulator` and `avdmanager` on the PATH (`ANDROID_HOME` + `JAVA_HOME` in `.zshrc`)
- [x] System image `android-37.0;google_apis;arm64-v8a` (native, no ABI emulation)
- [x] AVD **`nim`** — Pixel 10 profile, 4 GB RAM, 512 MB heap, host GPU, host keyboard on

M0 criterion met: `adb devices` lists `emulator-5554 device`, running
Android 17 / API 37.

How to bring the emulator back up:

```sh
emulator -avd nim &
adb wait-for-device
```

`set-home-activity` **does not survive an emulator reboot**: after each restart
the system launcher becomes the home again and you have to run again

```sh
adb shell cmd package set-home-activity com.gadfaria.nim/.MainActivity
```

---

## Next step

**M5** — polish: a decent transition between home and drawer (today it's a
`Crossfade`, which dissolves both at the same time), and the mock's long-press
menu with app info and uninstall.

---

## What M2 added

`LauncherApps` instead of `PackageManager.queryIntentActivities` — the
difference shows up in three places:

- **`profiles`** gives the work profiles. The same `ComponentName` can exist
  twice, one per profile, and launching in the wrong profile opens the wrong
  app. That's why the `UserHandle` is part of the `AppInfo` key, not just the
  component.
- **`Callback`** reports installs, uninstalls and renames. Verified on the
  emulator: I disabled YouTube with the drawer open and it left the list
  without a single tap. A launcher that only reads the list in `onCreate` shows
  apps that no longer exist.
- **`startMainActivity`** takes `sourceBounds`, so the opening animation starts
  from the tapped item instead of the center of the screen. The bounds come from
  the row's `onGloballyPositioned`.

Sorting uses a `Collator` with PRIMARY strength: a sort by character code would
put "Câmera" after "Z".

The vertical gesture (up opens, down closes) is the minimum for the drawer to be
usable now; the real transition belongs to M5. Closing also happens via the
back button and by pressing HOME inside Nim — the latter resets the scroll to
the top.

## Search and auto-launch

The field sits at the top of the drawer, takes focus and opens the keyboard
along with it: the drawer exists for typing, not for scrolling through 300
names.

### The ranking tiers

Lower is better; within the same tier, alphabetical order applies.

| # | Tier | Example |
|---|---|---|
| 0 | label prefix | `cam` → **Câmera** |
| 1 | word prefix | `cam` → Scanner de **Câm**era |
| 2 | initials | `gm` → **G**oogle **M**aps |
| 3 | contains | `ame` → C**âme**ra |

Word splitting breaks camelCase humps (`WhatsApp` → `whats`,`app`) but not runs
of capitals, so acronyms like `YT` don't become `Y`+`T`. That's what makes `app`
find WhatsApp through tier 1.

**There was a fifth tier, subsequence**, which made `wpp` find WhatsApp. It was
removed. It dragged loose results into the list — `gm` brought Gemini via
`g`…`e`…`m` — and loose results break auto-launch: the screen would show two
names and open one. The original plan cited `wpp`; in practice the cost didn't
pay off. Today WhatsApp is found by `wha` or `app`.

This runs in `data/AppSearch.kt`, plain Kotlin with nothing Android, because the
cases that matter depend on apps the emulator doesn't have — you can't verify
`wpp` finding WhatsApp with `adb`. The 10 tests in
`app/src/test/.../AppSearchTest.kt` cover each tier:

```sh
./gradlew :app:testDebugUnitTest
```

**A single result opens the app on its own**, after 150 ms without typing. The
debounce is short on purpose: it only needs to cover the gap between two
keystrokes of a fast typist, not give time to think. Zero would fire mid-word —
`"gmail"` would already open at `"gm"`.

The rule is literal: **it opens when the search has narrowed the list down to
one name**. No picking among visible results — what's on screen is what opens.
`ca` leaves Calendar and Camera and opens nothing; `cal` opens Calendar.

Two traps this creates, both closed:

- **The loop.** If the search survived the launch, coming back from the app
  would reopen the drawer with a single result and fire again, trapping the
  user. That's why launching *always* closes the drawer and clears the search —
  including on a normal tap and on Enter.
- **The re-anchored scroll.** A `LazyColumn` with `key` keeps the item that was
  at the top when the content changes. Filtering down to one app makes it the
  anchor; when the search is cleared the list tries to keep it at the top and
  hits the end of the scroll — the drawer reopened in the middle of the
  alphabet. The scroll reset has to happen **on open and on every search
  change**, never on close: there it runs before the re-anchoring and gets
  undone by it.

Normalization (lowercase, no accents) is computed once per app at load time,
not on every keystroke — with hundreds of apps, normalizing the whole list per
typed character is the difference between instant search and stuttering
search.

## Favorites (M4)

Long press toggles the favorite, in the drawer and on the home. With no icons
and no menu, the long press has no visual feedback — the vibration is the only
sign that something happened, so it isn't decoration.

The home shows favorites **in the order they were favorited**, at the bottom,
where the thumb reaches. That's why DataStore stores a delimited list, not a
preferences `Set`: a `Set` would lose the order on every read.

Two decisions worth recording:

- **The key is `ComponentName` + profile serial**, from
  `UserManager.getSerialNumberForUser()`. It used to be `user.hashCode()`, which
  worked as a Compose list key but is no good as on-disk identity: `hashCode`
  has no stability contract, and a favorite pointing to the wrong profile after
  a reboot would be a silent bug.
- **A key with no matching app is ignored, not deleted.** An app can be
  temporarily unavailable — work profile in quiet mode, update in progress — and
  deleting the favorite at that moment would lose the user's choice over a
  passing disappearance. It comes back on its own when the app returns.

Verified with a real emulator reboot: favorites come back in order, and the one
unfavorited before stays out.

## Work profile

The drawer has always listed both profiles (that's what `LauncherApps.profiles`
has given since M2), but without telling them apart: on the daily Pixel this
meant **Gmail, Drive, Calendar, Contacts, Files and Play Store showing up twice,
with the same name and no visible difference**.

Now an app from another profile gets a small drawn briefcase after its name.
Detection is `user != Process.myUserHandle()` — the public way to do it, since
`UserManager.isManagedProfile(id)` isn't open API.

This does **not** contradict the "no icons" at the top of this plan. What was
dropped there was loading and caching an icon PER APP, with adaptive icons and
the performance pain that comes with them. This is a single vector shape, shared
by every app in the profile.

The badge sits after the name, like the favorite dot, so every name starts on
the same column as the search field and the card labels. (It used to sit in
front, with its space reserved on every row; that put the names out of line
with the group labels once favorites moved into a card.)

On a name tie, personal comes before work.

**Consequence for auto-launch:** an app duplicated across both profiles never
narrows the list to one name, so `cal` no longer opens Calendar on its own.
That's right — opening the wrong profile would be worse — but it's worth
knowing that it applies to the 6 duplicated apps.

### Testing it

The `nim` emulator got a work profile for this purpose:

```sh
adb shell pm create-user --profileOf 0 --managed Work
adb shell am start-user 10
adb shell cmd package install-existing --user 10 <package>
```

With two profiles, `adb install` without `--user 0` tries to reach both and
hangs. To remove it: `adb shell pm remove-user 10`.

## Build notes

- **AGP 9 has Kotlin built in.** Applying `org.jetbrains.kotlin.android` is now
  a configuration error — the plugin was removed from the project. The KGP
  version comes with AGP; only the Compose plugin is still declared.
- Toolchain pinned in `gradle/libs.versions.toml`: AGP 9.3.2 · Gradle 9.7.1 ·
  Compose BOM 2026.08.00 · `compileSdk`/`targetSdk` 37 · `minSdk` 26 · Java 17.
- Build and install from the terminal:
  ```sh
  ./gradlew :app:assembleDebug
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  adb shell cmd package set-home-activity com.gadfaria.nim/.MainActivity
  ```
- To give the emulator its system launcher back:
  ```sh
  adb shell cmd package set-home-activity com.google.android.apps.nexuslauncher/.NexusLauncherActivity
  ```

---

## Known issues

- None open. Daily use on the device will tell what's missing.

---

## Gesture trap (M5)

Stacking two detectors on the same home Box — `detectVerticalDragGestures` to
open the drawer and `detectTapGestures` for the long press — **doesn't work**.
Both receive the same touch, fight over who consumes it, and the drag starts
failing intermittently: 6 attempts in a row without opening the drawer, with no
error in the log.

The fix is a single detector (`ui/HomeGestures.kt`) that decides what the
gesture is: if it leaves the touch slop before the long-press timeout, it's a
drag; if it stays still until the timeout, it's a long press. After that, 6/6 on
drag and 3/3 on long press.
