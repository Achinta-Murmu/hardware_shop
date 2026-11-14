# Galla

A phone app for a local shop owner. It replaces the paper notebook: items and prices, stock,
QR stickers, sales, purchases, expenses, and daily and monthly profit.

- **No login, no account, no internet.** Everything is stored on the phone. The app has no
  permission to go online.
- **Native Android**: Kotlin, Jetpack Compose, Material 3 Expressive.

This branch (`android-native`) is a rebuild from zero. The original Flutter app is on the
`main` branch.

## Get the app

The newest build is always here:
https://github.com/nayan-dey/hardware_shop/releases/download/preview/galla-preview.apk

Open the link on an Android phone and tap the file to install.

## Build

Nothing needs to be installed on a computer. Every push to `android-native` is built in the
cloud by `.github/workflows/android.yml` and published to the `preview` release above.

To see the screens without a phone, push a commit whose message contains `[shots]`. That runs
`.github/workflows/walkthrough.yml`: a robot user opens every screen on a cloud emulator and
uploads screenshots.

To build by hand, with JDK 21 and the Android SDK: `./gradlew :app:assembleRelease`.

## Read next

`AGENTS.md` explains what the app is, the rules it is built to, how the code is laid out, and
how the data is kept.
