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

Every listed version has been verified end-to-end — **all 12 patches apply
cleanly on the exact `versionCode` in the table below**. The fingerprints are
anchored on Gson `@SerializedName` values, Pinterest-owned class names and
stable Android SDK strings, so they survive every 14.2x release without any
code change.

**14.25.0 is the recommended daily-driver build** — it's the most-tested
version on real hardware (ad blocking, share sanitiser, copy-link resolver).
The other releases apply cleanly but have received less runtime testing.

| Version | Code       | minSdk | Direct download from APKMirror                                                                                                                                                        |
| ------- | ---------- | :----: | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 14.27.0 | `14278010` |   32   | _not on APKMirror yet — export from the Play Store with [SAI](https://f-droid.org/packages/com.aefyr.sai.fdroid/)_                                                                    |
| 14.26.0 | `14268010` |   29   | [Bundle (arm64-v8a, 480–640dpi)](https://www.apkmirror.com/apk/pinterest/pinterest-one-destination-for-a-world-of-inspiration/pinterest-14-26-0-release/pinterest-14-26-0-android-apk-download/) · [Universal APK](https://www.apkmirror.com/apk/pinterest/pinterest-one-destination-for-a-world-of-inspiration/pinterest-14-26-0-release/pinterest-14-26-0-2-android-apk-download/) |
| **14.25.0** ⭐ | `14258020` |   28   | [Bundle (arm64+v7a, 480dpi)](https://www.apkmirror.com/apk/pinterest/pinterest-one-destination-for-a-world-of-inspiration/pinterest-14-25-0-release/pinterest-14-25-0-2-android-apk-download/) · [Bundle (arm64, 480–640dpi)](https://www.apkmirror.com/apk/pinterest/pinterest-one-destination-for-a-world-of-inspiration/pinterest-14-25-0-release/pinterest-14-25-0-android-apk-download/) · [Universal APK](https://www.apkmirror.com/apk/pinterest/pinterest-one-destination-for-a-world-of-inspiration/pinterest-14-25-0-release/pinterest-14-25-0-3-android-apk-download/) |
| 14.24.0 | `14248020` |   28   | [Bundle (arm64+v7a, 480dpi)](https://www.apkmirror.com/apk/pinterest/pinterest-one-destination-for-a-world-of-inspiration/pinterest-14-24-0-release/pinterest-14-24-0-2-android-apk-download/) · [Bundle (arm64, 480–640dpi)](https://www.apkmirror.com/apk/pinterest/pinterest-one-destination-for-a-world-of-inspiration/pinterest-14-24-0-release/pinterest-14-24-0-android-apk-download/) · [Universal APK](https://www.apkmirror.com/apk/pinterest/pinterest-one-destination-for-a-world-of-inspiration/pinterest-14-24-0-release/pinterest-14-24-0-3-android-apk-download/) |
| 14.23.0 | `14238020` |   28   | [Universal APK](https://www.apkmirror.com/apk/pinterest/pinterest-one-destination-for-a-world-of-inspiration/pinterest-14-23-0-release/pinterest-14-23-0-android-apk-download/)      |
| 14.22.0 | `14228020` |   28   | [Universal APK](https://www.apkmirror.com/apk/pinterest/pinterest-one-destination-for-a-world-of-inspiration/pinterest-14-22-0-release/pinterest-14-22-0-android-apk-download/)      |
| 14.21.0 | `14218010` |   28   | [Universal APK](https://www.apkmirror.com/apk/pinterest/pinterest-one-destination-for-a-world-of-inspiration/pinterest-14-21-0-release/pinterest-14-21-0-android-apk-download/)      |
| 14.20.0 | `14208010` |   28   | [Universal APK](https://www.apkmirror.com/apk/pinterest/pinterest-one-destination-for-a-world-of-inspiration/pinterest-14-20-0-release/pinterest-14-20-0-android-apk-download/)      |

**Which variant to pick?** APKMirror lists two file types per release:

- **Bundle** (`.apkm`) — the Play Store multi-split (`base.apk` + language +
  DPI + ABI splits). Preferred: smaller install, matches your device
  configuration exactly. Requires the [APKMirror Installer](https://www.apkmirror.com/apkm-installer/)
  or [SAI](https://f-droid.org/packages/com.aefyr.sai.fdroid/) to install.
- **Universal APK** (`.apk`) — single file, works everywhere, larger download.
  Use this if the bundle installer refuses your device.

Both file types are accepted by Morphe Manager and the CLI. Patch output is
identical.

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.0.0-dev.6](https://github.com/SouBryan/pinterest-morphed/releases/tag/v1.0.0-dev.6)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;12 patches total
<details open>
<summary>📦 Pinterest&nbsp;&nbsp;•&nbsp;&nbsp;12 patches</summary>
<br>

**🎯 Supported versions:**

| 14.27.0 | 14.26.0 | 14.25.0 | 14.24.0 | 14.23.0 | 14.22.0 | 14.21.0 | 14.20.0 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Disable Android Privacy Sandbox Ad Services](#disable-android-privacy-sandbox-ad-services) | Removes the AD_SERVICES_CONFIG property so Android Privacy Sandbox ad APIs (Topics, Attribution Reporting, Custom Audiences) are not opted into. |  |
| [Disable AppsFlyer tracking](#disable-appsflyer-tracking) | Neutralises the AppsFlyer attribution / tracking SDK. init() becomes a no-op and isStopped() always returns true, so no events, installs, uninstalls or attribution data are transmitted. |  |
| [Disable Bugsnag crash tracking](#disable-bugsnag-crash-tracking) | Removes the Bugsnag API key metadata so the crash reporting SDK cannot initialize or upload telemetry. |  |
| [Disable Google Ads SDK](#disable-google-ads-sdk) | Removes the Google Mobile Ads (AdMob) initialization metadata so the SDK never starts. |  |
| [Disable Google Engage integration](#disable-google-engage-integration) | Removes the Google Engage broadcast receiver so Pinterest cannot publish content recommendations back to Google (Discover, Assistant, Play Store, etc.). |  |
| [Disable Google Engage worker](#disable-google-engage-worker) | Rewrites GoogleEngageWorker.createWork() to return null so WorkManager fails the periodic job and no content recommendations are ever published to Google. |  |
| [Hide ad views](#hide-ad-views) | Collapses Pinterest's ad-specific views (TextAdView, promoted closeup action bars, board sponsor headers, …) to zero-size on construction, so ad chrome never draws even if the feed adapter tries to render one. |  |
| [Hide promoted pins](#hide-promoted-pins) | Neutralises every ad-indicator field on the Pinterest pin/story models (is_promoted, promoted_is_*, is_native, ad_data, ...) so Promoted Pins, shopping-carousel ads, native-content ads and click-out CTAs are never rendered or fired. |  |
| [Opt out of Google Analytics](#opt-out-of-google-analytics) | Sets the default Google Analytics consent flags to false so the Firebase Measurement SDK does not collect analytics, ad data or personalization signals. |  |
| [Remove Advertising ID permission](#remove-advertising-id-permission) | Strips the com.google.android.gms.permission.AD_ID permission so any residual SDK cannot read the device's Google Advertising ID. |  |
| [Sanitize copied links](#sanitize-copied-links) | Resolves Pinterest short URLs (pin.it/…, pinterest.com/url_shortener/…) to their canonical pin URL before they are placed on the system clipboard, so "Copy link" no longer produces a fingerprinted short link. |  |
| [Sanitize sharing links](#sanitize-sharing-links) | Strips UTM and click-ID tracking parameters from the URL the app puts on the Android share sheet, so friends receive clean pin links. |  |

</details>

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
See the [Supported Pinterest versions](#-supported-pinterest-versions) table
above for direct APKMirror links. Prefer the **Bundle** (`.apkm`) variant when
listed — it's the same multi-split file Google Play delivers, so the download
matches your device configuration exactly. If the bundle installer refuses
your device, fall back to the **Universal APK**. Both produce identical
patched builds. Avoid already-modded or repacked APKs from other sources.

### Will you support version X.Y.Z?
Pinterest **14.20.0 through 14.27.0** are already verified (12/12 patches on
every release in that range). If a future Pinterest release still applies all
12 patches, it works with no code change on my side — the fingerprints are
anchored on Gson `@SerializedName` values and Pinterest-owned class names,
which R8 preserves. If a patch does stop applying, open an issue with your
Pinterest version and the CLI output so I can re-anchor the fingerprint.

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
