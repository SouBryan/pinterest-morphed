package app.soubryan.patches.pinterest.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

internal object Constants {
    /**
     * Pinterest app compatibility.
     *
     * The Pinterest APK is distributed by Google Play as an app bundle
     * (base.apk + split_config.*), so the preferred file type is APKM
     * (Morphe Manager will guide users to fetch a bundle from APKMirror or
     * export it directly from an installed copy).
     *
     * Targets are ordered from newest to oldest. The first non-experimental
     * target is treated as the "recommended" version by Morphe Manager.
     */
    val COMPATIBILITY_PINTEREST = Compatibility(
        name = "Pinterest",
        packageName = "com.pinterest",
        apkFileType = ApkFileType.APKM,
        // Pinterest brand red (Play Store / launcher icon background).
        appIconColor = 0xE60023,
        targets = listOf(
            AppTarget(
                version = "14.26.0",
                minSdk = 29,
                isExperimental = true,
            ),
            AppTarget(
                version = "14.25.0",
                minSdk = 29,
            ),
            AppTarget(
                version = "14.24.0",
                minSdk = 28,
                isExperimental = true,
            ),
        ),
    )
}
