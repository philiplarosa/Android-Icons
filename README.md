# Android Icons

A small icon pack app for [Lawnchair](https://lawnchair.app) (it also works in Nova and most other launchers that support icon packs).

![Icon preview](docs/preview.png)

Rows show how the icons look with circle, squircle and rounded-square icon shapes, plus Android 13+ themed icons. The icons are adaptive, so Lawnchair masks them to whatever icon shape you've chosen.

## Install

1. Open the [latest release](https://github.com/philiplarosa/Android-Icons/releases/latest) on your phone and tap **AndroidIcons.apk**.
2. Allow your browser to install unknown apps when Android asks, then tap **Install**.

Every push to this repo builds a new release. Newer builds install over older ones; you don't need to uninstall first.

## Use it in Lawnchair

- **Whole pack:** Lawnchair Settings › General › Icon pack › Android Icons. Apps listed in `appfilter.xml` switch automatically. Everything else keeps its normal icon.
- **One app at a time:** long-press an app, tap the edit (pencil) option, tap the icon, choose Android Icons and pick any icon.

Open the Android Icons app to see which icons match an app on your phone (✓ means it auto-applies).

## Included icons

Phone, Messages, Camera, Chrome, Gmail, Maps, Photos, YouTube, Spotify, Settings.

`appfilter.xml` covers the Google, Samsung and stock Android versions of Phone, Messages and Camera. If an icon shows "No matching app found" in the app, your phone uses a different component name. You can still pick that icon manually, or add the right line (see below).

## Add or change an icon

All of it lives in `app/src/main/res/`:

| File | What it does |
| --- | --- |
| `drawable/<name>.xml` | The icon: a background colour plus a foreground glyph |
| `drawable/<name>_glyph.xml` | The white glyph (vector, drawn on a 24×24 grid) |
| `xml/drawable.xml` | Icons Lawnchair shows in its picker |
| `xml/appfilter.xml` | Which app each icon applies to automatically |

**To add an icon:**

1. Copy an existing pair, for example `spotify.xml` and `spotify_glyph.xml`, and rename both (lowercase letters, numbers and underscores only).
2. Change the background colour in `<name>.xml`. For the glyph, either edit the path, or in Android Studio use File › New › Vector Asset to import an SVG and point the icon's `foreground` at it.
3. Add `<item drawable="<name>" name="Label" />` to `xml/drawable.xml`.
4. In the Android Icons app, scroll to **Installed apps** and tap the app you want. That copies its `appfilter.xml` line. Paste it into `xml/appfilter.xml` and set `drawable` to your icon's name.

**Using PNG artwork instead of vectors:** drop a square PNG (512×512 is plenty) into `res/drawable-nodpi/<name>.png` and do steps 3 and 4. Lawnchair shows a PNG exactly as drawn, so it won't follow your icon shape setting. For that, use the PNG as the `foreground` of an adaptive icon. Keep the important part inside the middle 61% of the image, because the edges get cropped by the shape mask.

You can do all of this in the GitHub web editor from your phone. Commit, wait for the build to finish (about 3 minutes), then install the new release.

## Build locally

Needs JDK 17+ and the Android SDK (Android Studio installs both).

```sh
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

Every build is signed with `app/sideload.keystore`. The password is `android`, so builds from your computer and from GitHub can update each other. That's fine for sideloading but not secure: use your own private key if you ever publish this to the Play Store.
