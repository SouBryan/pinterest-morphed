# 📌 Pinterest Morphed

<p align="center">

![GitHub Release](https://img.shields.io/github/v/release/SouBryan/pinterest-morphed?style=for-the-badge&logo=github&color=E60023)
![GitHub Pre-Release](https://img.shields.io/github/v/release/SouBryan/pinterest-morphed?include_prereleases&label=pre-release&style=for-the-badge&color=BD081C)
![License](https://img.shields.io/github/license/SouBryan/pinterest-morphed?style=for-the-badge&color=E60023)
![GitHub Stars](https://img.shields.io/github/stars/SouBryan/pinterest-morphed?style=for-the-badge&color=E60023)
![GitHub Issues](https://img.shields.io/github/issues/SouBryan/pinterest-morphed?style=for-the-badge&color=BD081C)

</p>

<br/>

> [!NOTE]
>
> Focused, Pinterest-only patch set for use with **[Morphe](https://morphe.software)**.
> No ads, no trackers, no third-party analytics — just the boards, pins and the feed.
>
> Patch requests are welcome, but this repo intentionally stays scoped to the Pinterest Android app only.

<br/>

## Contents

- [About](#-about)
- [How to use these patches](#how-to-use-these-patches)
- [Supported Pinterest versions](#-supported-pinterest-versions)
- [Patches list](#-patches-list)
- [Reporting bugs](#reporting-bugs)
- [Building from source](#%EF%B8%8F-building)
- [FAQ](#faq)
- [Disclaimer](#-disclaimer)

## ❓ About

Pinterest is a great pin/moodboard app, but the client keeps piling on tracking
SDKs and Promoted Pins that get in the way. This project ships a curated set
of Morphe patches that strip every third-party ad, analytics and telemetry
SDK, plus every "Promoted"/"Sponsored" pin, without touching the parts of the
app you actually use.

The patches target the **Pinterest Android client** distributed on the Play
Store and APKMirror. Everything is delivered as a standard Morphe patch bundle
(`.mpp`) that plugs into **Morphe Manager** or **Morphe CLI** — no side-loaded
APK from strangers, you always build your own patched APK from a clean copy of
Pinterest.

## How to use these patches

Click here to add the patch source to Morphe:
<https://morphe.software/add-source?github=SouBryan/pinterest-morphed>

Or, in Morphe Manager, open **Settings → Sources → Add source** and paste:

```
https://github.com/SouBryan/pinterest-morphed
```

Then:

1. Grab a **clean Pinterest APK bundle** (`.apkm`, `.apks`, `.xapk`) from
   [APKMirror](https://www.apkmirror.com/apk/pinterest/pinterest/) or export it
   from your installed copy.
2. In Morphe Manager, pick **Pinterest** and apply the patches — all are enabled
   by default and safe to combine.
3. Install the patched APK. On the first launch you'll need to sign in again
   (Pinterest treats the re-signed patched build as a different app).

## 🎯 Supported Pinterest versions

| Version | Channel       | Notes                                                          |
| ------- | ------------- | -------------------------------------------------------------- |
| 14.26.0 | Experimental  | Latest beta channel — should work, fingerprints not re-anchored |
| 14.25.0 | **Recommended** | Latest stable on the Play Store — patches developed here      |
| 14.24.0 | Experimental  | Kept working for users still on an older release               |

Fingerprints are anchored to **14.25.0**. Older/newer versions are best-effort:
if a patch's fingerprint no longer matches, the patch is skipped instead of
applying incorrectly, so the app will still install — you just get less
cleanup.

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->

<!-- Do not modify this section by hand. The patch list is generated when release.yml creates a new release.

     If you wish for the patches list to be collapsed, then remove the word 'EXPANDED' from the comment tag above.

     If you wish to manually keep this list updated then remove the PATCHES_START and PATCHES_END
     comment blocks entirely. -->

Currently shipping **9 patches** across two categories:

### 🚫 Ads

| Patch | Type | What it does |
|---|---|---|
| **Hide promoted pins** | bytecode | Rewrites every `isPromoted` getter across every Pinterest API model (Pin, Story, PinnableStory, …) so no pin is ever treated as a promoted slot. Kills the "Promoted" badge, the ad-impression beacons, ad-slot insertion in the feed, and the click-out CTA. |
| **Disable Google Ads SDK** | resource | Removes the AdMob `APPLICATION_ID` metadata from the manifest so the Google Mobile Ads SDK aborts initialization silently. |
| **Disable Android Privacy Sandbox Ad Services** | resource | Removes the `AD_SERVICES_CONFIG` property so the app is not opted into Android 13+ Privacy Sandbox APIs (Topics, Attribution Reporting, Custom Audiences). |

### 🕵️ Tracking / analytics

| Patch | Type | What it does |
|---|---|---|
| **Disable AppsFlyer tracking** | bytecode | Hooks `AppsFlyerLib.init()` to no-op and `AppsFlyerLib.isStopped()` to always return `true`. No install attribution, no in-app events, no uninstall tokens, no deep-link resolution. |
| **Disable Bugsnag crash tracking** | resource | Strips the Bugsnag `API_KEY` metadata so the crash reporting SDK cannot initialize or upload telemetry (the native root-detection library still loads but stays inert). |
| **Disable Google Engage integration** | resource | Removes the `GoogleEngageBroadcastReceiver` and its ENV metadata so the app cannot publish content recommendations back to Google (Discover, Assistant, Play Store surfaces). |
| **Disable Google Engage worker** | bytecode | Also rewrites the periodic `GoogleEngageWorker.createWork()` to `Single.just(Result.success())` — no data leaves the device even if the worker is scheduled again by another code path. |
| **Opt out of Google Analytics** | resource | Flips the four `google_analytics_default_allow_*` consent flags to `false` so the Firebase Measurement / GA4 SDK never collects analytics, ad data, ad-user data or personalization signals. |
| **Remove Advertising ID permission** | resource | Drops the `com.google.android.gms.permission.AD_ID` permission so any residual SDK that reads the Google Advertising ID gets a zeroed-out, opted-out value. |

<!-- PATCHES_END -->

## Reporting bugs

Please include:

- Pinterest version (Settings → About) — e.g. `14.25.0`
- Pinterest Morphed release used to patch — e.g. `stable v1.0.0` or `dev v1.1.0-dev.3`
- Where you got the APK bundle from (APKMirror, Play Store export, …)
- Relevant logs. Capture with:
  ```bash
  adb logcat -c
  adb logcat | grep -i "pinterest\|AndroidRuntime\|FATAL" > logs.txt
  ```
- Steps to reproduce and — if visual — a screenshot or screen recording.

Open a bug via the [issue template](https://github.com/SouBryan/pinterest-morphed/issues/new?template=bug_report.yml).

## 🛠️ Building

Requirements:

- **JDK 21**
- **Android SDK** (any recent build-tools)
- A **GitHub Personal Access Token** with `read:packages` scope — the Morphe
  patcher is published to GitHub Packages, not Maven Central. Save it in
  `~/.gradle/gradle.properties`:
  ```properties
  gpr.user=<your github username>
  gpr.key=<your PAT>
  ```
  Or export `GITHUB_ACTOR` / `GITHUB_TOKEN`.

Then:

```bash
./gradlew :patches:buildAndroid
# → patches/build/libs/patches-<version>.mpp
```

Apply locally with the [Morphe CLI](https://github.com/MorpheApp/morphe-cli):

```bash
morphe patch \
  --patches patches/build/libs/patches-*.mpp \
  com.pinterest_14.25.0.apkm
```

## FAQ

### How do I actually use this?
Install [Morphe Manager](https://morphe.software), add this repo as a patch
source, then pick Pinterest to patch.

### The patched app makes me log in again — is that normal?
Yes. All patched apps are re-signed, so Android sees them as a different app
from the Play Store version and won't share credentials.

### Google/Facebook login is broken.
Also expected. Any auth flow that verifies the app's signing certificate on
the *server* side (Google Sign-In, Google Drive, Meta Login, …) refuses to
talk to a re-signed APK. Log in with email/password instead. This is a
limitation of every patched Android app, not something specific to this repo.

### Which APK/bundle should I download?
For the bundle patches, prefer `.apkm` from APKMirror. `arm64-v8a` is enough
if your phone is from the last 6 years. Avoid already-modded or repacked
APKs.

### Will you support version X.Y.Z?
If it's a Pinterest release after `14.25.0` and the fingerprints still hold,
it should already work (marked "experimental"). If a patch stops applying,
open an issue with your Pinterest version and the CLI output so I can
re-anchor the fingerprints.

### Can you make patches for other apps?
No — this repo is Pinterest-only by design. There are other Morphe patch
sources that cover many apps (see
[rushiranpise/morphe-patches](https://github.com/rushiranpise/morphe-patches),
the official [MorpheApp/morphe-patches](https://github.com/MorpheApp/morphe-patches), etc.).

## 📜 Disclaimer

> **⚠️ Legal Notice**
>
> This project exists for **educational, research and personal privacy purposes only**.
> It modifies a third-party application (Pinterest) and may violate that app's terms of service.
>
> - This project is **not affiliated** with, endorsed by, or sponsored by Pinterest, Inc., the Morphe open source project, or any of the SDK vendors mentioned in the patches list.
> - "Morphe" is referenced solely for descriptive compatibility purposes (see [NOTICE](NOTICE) for the GPLv3 §7(b)/§7(c) terms that apply).
> - Use these patches **at your own risk**. The author is not responsible for account bans, data loss, or any other consequence of using patched software.
> - If you are Pinterest, Inc. or another rights-holder and believe this project infringes on your rights, please open a private issue or contact the repository owner and the relevant patches will be reviewed and, if appropriate, removed.

## 📄 License

Released under the [GNU General Public License v3.0](LICENSE) with the
Additional Terms in [NOTICE](NOTICE).

## ❤️ Credits

- The [Morphe](https://github.com/MorpheApp) team — patcher, CLI, documentation, patches template.
- [rushiranpise/morphe-patches](https://github.com/rushiranpise/morphe-patches) — layout of this README is inspired by their work.
- Everyone who tests patches on their phone and files a good bug report.
