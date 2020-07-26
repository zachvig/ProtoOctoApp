# OctoApp for OctoPrint
This app allows to control an [OctoPrint instance](https://octoprint.org/).

# Overview
The app is structured into "workspaces" which get activated by the app depending on the current state. Following workspaces are availabled:

- The **Connect** workspace is activated when no printer is connected to OctoPrint. OctoApp will auto-connect the first printer being available.
- The **Prepare** workspace is activated when a printer is connected but no print is active. You can prepare your print in this workspace by e.g. homing your machine or preparing filament for the print
- The **Print** workspace is activated as soon as a print is started. This workspaces shows the current print progress and allows tweaking of the current print

![Image](https://gitlab.com/crysxd/octoapp/-/raw/master/designs/Gitlab%20Readme.jpg?inline=false)

# Notworthy features
**PSU plugin support**
This app supports the [PSU Control](https://plugins.octoprint.org/plugins/psucontrol/) plugin allowing the printer to be turned on or off from the app

**Emergency stop**
The app offers a dedicated button to trigger an `M112` emergency stop from the print-workspace

# Installation
You can install the app from [Google Play](https://play.google.com/store/apps/details?id=de.crysxd.octoapp) or download an APK from the [release section](https://gitlab.com/crysxd/octoapp/-/releases).

# Build
## Debug builds
Run `./gradlew assembleDebug` to build the app after cloning. The APK file can be found at `app/build/outputs/apk/debug/app-debug.apk`.

# Build
The folder `test-environment` contains a configured OctoPrint instance you can use as a test environment. Start it with `docker-compose up -d`.

### Test users

- `tests` (password `test`, is admin)

## Release builds

- Place your keystore file at `app/key.keystore`
- Place your Google Service file at `app/service-account.json`
- Add following entried to `local.properties`
  - `signing.storePassword=` followed by your store password
  - `signing.keyAlias=` followed by your key alias
  - `signing.keyPassword=` followed by your key password
- Run `./gradlew assembleRelease`. The APK file can be found at `app/build/outputs/apk/release/app-release.apk`.

# Project structure
The app is devided in modules:

- `octoprint` contains the API connecting the app to octoprint. This module can be extracted and used in other projects. It is planned to release this module as a standalone-library in the future.
- `base` contians shared code for the app
- `signin` contains the sign in screen and the QR code reader for the API key
- `connect-printer` contains the connect-workspace
- `pre-print-contronls` contains the prepare-workspace
- `print-contronls` contains the print-workspace
- `app` contains the single `Activity` as well as the `Application` class alongside other app-level utilities

Design resources can be found in the `design` folder. The app is written in utilizing Android X libraries as well as Kotlin Coroutines.