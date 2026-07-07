package app.soubryan.patches.pinterest.tracking

import app.morphe.patcher.patch.resourcePatch
import app.soubryan.patches.pinterest.shared.Constants.COMPATIBILITY_PINTEREST
import org.w3c.dom.Element

/**
 * Removes the `com.google.android.gms.permission.AD_ID` `<uses-permission>`
 * from `AndroidManifest.xml`.
 *
 * With this permission absent, `AdvertisingIdClient.getAdvertisingIdInfo()`
 * returns a zeroed-out ID marked as `limitAdTracking = true`, so any residual
 * SDK call that reads the Advertising ID gets nothing usable.
 *
 * Cross-app tracking based on the Google Advertising ID is effectively broken.
 */
@Suppress("unused")
val removeAdIdPermissionPatch = resourcePatch(
    name = "Remove Advertising ID permission",
    description = "Strips the com.google.android.gms.permission.AD_ID permission so any residual SDK cannot read the device's Google Advertising ID.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    execute {
        document("AndroidManifest.xml").use { document ->
            val root = document.documentElement
            val permissions = root.getElementsByTagName("uses-permission")

            for (i in permissions.length - 1 downTo 0) {
                val perm = permissions.item(i) as Element
                if (perm.getAttribute("android:name") == "com.google.android.gms.permission.AD_ID") {
                    root.removeChild(perm)
                }
            }
        }
    }
}
