package app.soubryan.patches.pinterest.ad

import app.morphe.patcher.patch.resourcePatch
import app.soubryan.patches.pinterest.shared.Constants.COMPATIBILITY_PINTEREST
import org.w3c.dom.Element

/**
 * Removes the Google Mobile Ads (AdMob) `APPLICATION_ID` `<meta-data>` from
 * `AndroidManifest.xml`.
 *
 * The Google Ads SDK reads this meta-data at startup to identify the publisher
 * account. When it is missing, the SDK aborts initialization silently (it does
 * not crash the host app), which is enough to stop ad requests from being made.
 *
 * The two related debug flags (`NATIVE_AD_DEBUGGER_ENABLED`,
 * `DISABLE_CRASH_REPORTING`) are removed alongside it since they are only
 * meaningful when the SDK actually starts.
 */
@Suppress("unused")
val disableGoogleAdsPatch = resourcePatch(
    name = "Disable Google Ads SDK",
    description = "Removes the Google Mobile Ads (AdMob) initialization metadata so the SDK never starts.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    execute {
        val metaToRemove = setOf(
            "com.google.android.gms.ads.APPLICATION_ID",
            "com.google.android.gms.ads.flag.NATIVE_AD_DEBUGGER_ENABLED",
            "com.google.android.gms.ads.flag.DISABLE_CRASH_REPORTING",
        )

        document("AndroidManifest.xml").use { document ->
            val application = document
                .getElementsByTagName("application")
                .item(0) as Element

            val metas = application.getElementsByTagName("meta-data")

            // Iterate in reverse — removing a node shifts subsequent indices.
            for (i in metas.length - 1 downTo 0) {
                val meta = metas.item(i) as Element
                if (meta.getAttribute("android:name") in metaToRemove) {
                    application.removeChild(meta)
                }
            }
        }
    }
}
