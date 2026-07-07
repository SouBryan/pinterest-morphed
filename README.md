# 📌 Pinterest Morphed

Morphe patches that clean up the Pinterest Android app: remove promoted pins,
ad SDKs, third-party analytics and telemetry — for a distraction-free feed.

> ⚠️ Independent community project. Not affiliated with, endorsed by, or
> sponsored by Pinterest or the Morphe open source project.
> "Morphe" is referenced solely for descriptive compatibility.

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->

<!-- Do not modify this section by hand. The patch list is generated when release.yml creates a new release.

     If you wish for the patches list to be collapsed, then remove the word 'EXPANDED' from the comment tag above.

     If you wish to manually keep this list updated then remove the PATCHES_START and PATCHES_END
     comment blocks entirely. -->

#### A list of patches will automatically appear here after the first release is created.

&nbsp;

<!-- PATCHES_END -->

## 🚀 Usage

### With Morphe Manager (recommended)

1. Install [Morphe Manager](https://morphe.software) on your device.
2. Open **Settings → Sources → Add source** and paste:

   ```
   https://github.com/SouBryan/pinterest-morphed
   ```

3. Grab a Pinterest bundle (`.apkm` from APKMirror or export it from your
   installed copy) and let Morphe Manager apply the patches.

### With Morphe CLI

```bash
# Build the patches locally (needs a GitHub PAT with read:packages, see below)
./gradlew :patches:buildAndroid

# Apply to a Pinterest bundle
morphe patch \
  --patches patches/build/libs/patches-*.mpp \
  com.pinterest_14.25.0.apkm
```

## 🛠️ Building from source

Requirements:

- JDK 21
- A GitHub Personal Access Token with `read:packages` scope (the Morphe patcher
  is published to GitHub Packages). Save it as `gpr.user`/`gpr.key` in
  `~/.gradle/gradle.properties`, or set the `GITHUB_ACTOR`/`GITHUB_TOKEN`
  environment variables.

```bash
./gradlew :patches:buildAndroid
# → patches/build/libs/patches-<version>.mpp
```

## 🎯 Supported Pinterest versions

| Version | Status         | Notes                                |
| ------- | -------------- | ------------------------------------ |
| 14.26.0 | Experimental   | Latest beta at time of writing       |
| 14.25.0 | ✅ Recommended | Latest stable on the Play Store      |
| 14.24.0 | Experimental   | Retained for users on older releases |

Fingerprints are anchored to `14.25.0`; other versions are best-effort.

## 🧑‍💻 Development

- Do work on the `dev` branch. `main` is reserved for stable releases.
- Use [Conventional Commits](https://www.conventionalcommits.org/): `feat:`,
  `fix:`, `chore:` — the `release.yml` workflow handles versioning and
  publishing automatically.
- `feat:` / `fix:` on `dev` publish a **pre-release**; merging `dev` → `main`
  publishes a **stable release**.
- Never manually edit generated files: `patches-list.json`, `patches-bundle.json`,
  `CHANGELOG.md`, and the `<!-- PATCHES_START -->…<!-- PATCHES_END -->` block
  in this README.

## 📜 License

Released under the [GNU General Public License v3.0](LICENSE). See [NOTICE](NOTICE)
for GPLv3 §7(b) and §7(c) terms that also apply.
