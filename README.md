# CustomWallpapers ("Naze Wall")

A plug-and-play wallpaper app and ringtone bundle for **any AOSP-based ROM** — LineageOS, AxionOS, crDroid, EvolutionX, Pixel/GMS builds, and plain AOSP.

It keeps your ROM's stock wallpapers intact while adding your own custom wallpaper collection and ringtones.

## Features

- **Works on any ROM** — uses pure Android SDK APIs (`sdk_version: "current"`, zero external dependencies).
- **Never replaces stock wallpapers** — no `PARTNER_CUSTOMIZATION` conflicts, so `Backgrounds` / Pixel / Axion wallpapers stay untouched.
- **Integrates everywhere:**
  - **LineageOS / AOSP / Pixel (`ThemePicker` / `WallpaperPicker2`):** appears inside **Wallpaper & style** as its own **Naze Wall** category.
  - **AxionOS (`AxThemePicker`) & custom launchers:** opens directly from the **App Drawer** (or launcher wallpaper chooser) and lets you preview and apply to **Home screen**, **Lock screen**, or **Both**.
- **Automatic ringtone install** — any audio files in `ringtones/` are automatically installed to `/product/media/audio/ringtones/`.

## Quick Start

### 1. Add the repo to your ROM tree

Clone it into `packages/apps/CustomWallpapers` (or anywhere your tree scans `Android.bp`):

```bash
git clone https://github.com/naden01/walls packages/apps/CustomWallpapers
```

Or add it to your `.repo/local_manifests/`:

```xml
<project path="packages/apps/CustomWallpapers"
         name="naden01/walls"
         remote="github"
         revision="main" />
```

### 2. Include it in your device makefile

Add to `device.mk` (or `lineage_<device>.mk`):

```make
PRODUCT_PACKAGES += \
    CustomWallpapers
```

That single line builds and installs both:
- `/product/app/CustomWallpapers/CustomWallpapers.apk`
- `/product/media/audio/ringtones/*` (via `CustomRingtones`)

No `$(filter-out Backgrounds,...)` workaround is needed.

## Adding Wallpapers

1. **Drop your image** into `res/drawable-nodpi/` using lowercase letters, numbers, and underscores only (no spaces):
   ```text
   res/drawable-nodpi/my_wallpaper.jpg
   ```
   Supported formats: `.jpg`, `.png`, `.webp`.

2. **Add the title** in `res/values/strings.xml`:
   ```xml
   <string name="my_wallpaper_title">My Wallpaper</string>
   ```

3. **Register it** inside `<category>` in `res/xml/wallpapers.xml`:
   ```xml
   <static-wallpaper
       id="my_wallpaper"
       src="@drawable/my_wallpaper"
       title="@string/my_wallpaper_title" />
   ```
   *(Keep attributes un-namespaced — `id`, `src`, `title`, optional `subtitle1` / `subtitle2`, NOT `android:id`.)*

## Adding Ringtones

Drop `.mp3` or `.ogg` files into `ringtones/`:

```text
ringtones/My_Ringtone.mp3
```

They are picked up automatically by `Android.bp` (`CustomRingtones`) and appear in **Settings → Sound & vibration → Phone ringtone**.

## Renaming the App or Category

- **App name:** edit `app_name` in `res/values/strings.xml`.
- **Category name:** edit `naze_wall_title` in `res/values/strings.xml` (and optionally `category id` in `res/xml/wallpapers.xml`).
- **App icon:** edit `res/drawable/ic_launcher.xml`.

## Repository Layout

| Path | Purpose |
| --- | --- |
| `Android.bp` | Soong build rules for `CustomWallpapers` and `CustomRingtones` |
| `AndroidManifest.xml` | Declares `LAUNCHER` and `SET_WALLPAPER` activities |
| `src/com/custom/wallpapers/` | Wallpaper grid, XML parser, and full-screen preview/apply UI |
| `res/xml/wallpapers.xml` | Wallpaper categories and entries |
| `res/values/strings.xml` | App, category, and wallpaper titles |
| `res/drawable-nodpi/` | Wallpaper images |
| `ringtones/` | Ringtone audio files installed to `/product/media/audio/ringtones/` |
| `setup.mk` | Example `PRODUCT_PACKAGES` snippet |
