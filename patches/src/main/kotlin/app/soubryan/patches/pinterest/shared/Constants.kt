package app.soubryan.patches.pinterest.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

internal object Constants {
    /**
     * Pinterest app compatibility.
     *
     * The Pinterest APK is distributed by Google Play as an app bundle
     * (`base.apk + split_config.*`), so the preferred file type is APKM
     * (Morphe Manager will guide users to fetch a bundle from APKMirror or
     * export it from an installed copy with SAI / APKExport).
     *
     * Every target from 14.20.0 through 14.27.0 has been verified end-to-end:
     * all 12 patches apply cleanly (12/12) on the exact version code listed
     * below. The fingerprints are anchored on Gson `@SerializedName` values,
     * Pinterest-owned class names and stable Android SDK strings — they have
     * survived every Pinterest release in this range without any code change.
     *
     * Targets are ordered from newest to oldest. **14.25.0 is the recommended
     * daily-driver build** — it is the most-tested version end-to-end on the
     * developer's device (ad blocking, share-sanitiser, copy-link resolver).
     * The other releases apply cleanly but have received less runtime testing.
     */
    val COMPATIBILITY_PINTEREST = Compatibility(
        name = "Pinterest",
        packageName = "com.pinterest",
        apkFileType = ApkFileType.APKM,
        // Pinterest brand red (Play Store / launcher icon background).
        appIconColor = 0xE60023,
        targets = listOf(
            AppTarget(
                version = "14.27.0",
                versionCode = 14278010,
                // Pinterest 14.27 raised the manifest minSdk to 32 (Android 12L).
                // Devices on Android 11 / 12 (SDK 30 / 31) can still patch older
                // releases in this list.
                minSdk = 32,
                description = "Latest release — bumps minSdk to Android 12L (SDK 32).",
            ),
            AppTarget(
                version = "14.26.0",
                versionCode = 14268010,
                minSdk = 29,
            ),
            AppTarget(
                version = "14.25.0",
                versionCode = 14258020,
                minSdk = 28,
                description = "Recommended — verified daily-driver build.",
            ),
            AppTarget(
                version = "14.24.0",
                versionCode = 14248020,
                minSdk = 28,
            ),
            AppTarget(
                version = "14.23.0",
                versionCode = 14238020,
                minSdk = 28,
            ),
            AppTarget(
                version = "14.22.0",
                versionCode = 14228020,
                minSdk = 28,
            ),
            AppTarget(
                version = "14.21.0",
                versionCode = 14218010,
                minSdk = 28,
            ),
            AppTarget(
                version = "14.20.0",
                versionCode = 14208010,
                minSdk = 28,
            ),
        ),
    )
}
