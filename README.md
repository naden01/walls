# CustomWallpapers — "Naze Wall"

A universal wallpaper and ringtone package for AOSP-based ROMs (LineageOS, AxionOS,
Pixel/GMS builds, crDroid, etc.).

## How it works

Instead of registering as a `PARTNER_CUSTOMIZATION` stub (which forces the picker to
choose only ONE partner package and hides stock ROM wallpapers on LineageOS/AOSP, and is
ignored completely by ROMs with custom pickers like AxionOS `AxThemePicker`),
`CustomWallpapers` is a self-contained wallpaper picker app with zero external library
dependencies:

- **AOSP / LineageOS (`ThemePicker` / `WallpaperPicker2` / Pixel):** exposes
  `android.intent.action.SET_WALLPAPER` without `GET_CONTENT`, so it appears inside the
  system wallpaper picker as its own **Naze Wall** category (`ThirdPartyAppCategory`)
  *alongside* stock `Backgrounds` / Pixel wallpapers.
- **AxionOS (`AxThemePicker`) & other ROMs/launchers:** exposes a launcher entry
  (`MAIN` / `LAUNCHER`) so **Naze Wall** opens directly from the app drawer or launcher
  chooser and applies Home / Lock / Both screens via `WallpaperManager`.
- **Ringtones:** `CustomRingtones` (`prebuilt_media`, pulled in automatically via
  `required: ["CustomRingtones"]`) installs everything in `ringtones/` to
  `/product/media/audio/ringtones/`.

## Add wallpapers

1. Drop images into `res/drawable-nodpi/` (e.g. `wallpaper_1.png`).
2. Add one `<static-wallpaper>` per image in `res/xml/wallpapers.xml`.
3. Add the display title in `res/values/strings.xml`.

To add ringtones, drop audio files (`.mp3` / `.ogg`) into `ringtones/`.

## Build

Include the module in your device makefile (see `setup.mk`):

```make
PRODUCT_PACKAGES += CustomWallpapers
```

No `$(filter-out Backgrounds,...)` workaround is needed; stock ROM wallpapers remain
intact.

## Files

- `Android.bp` — Soong `android_app` (`CustomWallpapers`) + `prebuilt_media` (`CustomRingtones`)
- `AndroidManifest.xml` — `SET_WALLPAPER` + `LAUNCHER` activity declarations
- `src/com/custom/wallpapers/` — native Android SDK picker & preview activities
- `res/xml/wallpapers.xml` — category "Naze Wall" + wallpaper list
- `res/values/strings.xml` — app, category, and wallpaper titles
- `res/drawable-nodpi/` — wallpaper images
- `ringtones/` — audio files installed to `/product/media/audio/ringtones/`
