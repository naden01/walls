# CustomWallpapers — "Naze Wall"

A partner wallpaper package for AOSP/LineageOS, targeting the **LineageOS 23.2**
wallpaper picker (`packages/apps/WallpaperPicker2`, shipped as `com.android.wallpaper`
via `packages/apps/ThemePicker`). Its wallpapers appear under a picker category called
**Naze Wall**.

## How it works

The picker does not scan arbitrary apps. `DefaultPartnerProvider.findSystemApk()`
finds a single *partner customization* package by querying broadcast receivers for
`com.android.launcher3.action.PARTNER_CUSTOMIZATION` (and requires it to be a system
app), then reads its resources:

| resource | parsed by |
| --- | --- |
| `xml/wallpapers` (`<category>` + `<static-wallpaper>`) | `DefaultCategoryProvider.getSystemCategories()` |
| `array/partner_wallpapers` (legacy list of drawable names) | `WallpaperParserImpl.parsePartnerWallpaperInfoResources()` |

This module uses the `xml/wallpapers` form so wallpapers land in a *named* category.

> `android.wallpaper.preview` (used in the old draft) is `WallpaperManager.WALLPAPER_PREVIEW_META_DATA`
> and only sets a chooser preview icon for an `ACTION_SET_WALLPAPER` activity — it does
> not add wallpapers. It has been removed.

## Add wallpapers

1. Drop images into `res/drawable-nodpi/` (e.g. `wallpaper_1.png`).
2. Add one `<static-wallpaper>` per image in `res/xml/wallpapers.xml`.
   Attributes are **un-namespaced on purpose** (`id`, `src`, `title`, …) — the parser
   reads them with `getAttributeValue(null, ...)`, so an `android:` prefix hides them.
3. Add the display title in `res/values/strings.xml`.

To rename the category, change `category id` / `category title` in
`res/xml/wallpapers.xml` and the `naze_wall_title` string.

## Build

Include the module (see `setup.mk`):

```make
PRODUCT_PACKAGES += CustomWallpapers
```

**Partner conflict:** LineageOS already ships `Backgrounds`
(`org.lineageos.backgrounds`, added by `vendor/lineage/config/common_mobile.mk`) as a
partner, and the picker reads only ONE partner. The receiver here sets
`android:priority="100"` to win the resolution order. For a deterministic setup, drop
Backgrounds *after* the LineageOS inherit in `lineage_LH8n.mk`:

```make
PRODUCT_PACKAGES := $(filter-out Backgrounds,$(PRODUCT_PACKAGES))
```

Because only one partner is read, LineageOS's stock "Backgrounds" wallpapers will not
appear next to these.

## Files

- `Android.bp` — Soong `android_app` (product partition, resource-only)
- `AndroidManifest.xml` — partner-customization receiver
- `res/xml/wallpapers.xml` — category "Naze Wall" + wallpaper list
- `res/values/strings.xml` — category and wallpaper titles
- `res/drawable-nodpi/` — wallpaper images
- `ringtones/` — extra audio assets kept with the module (not installed by the build)
