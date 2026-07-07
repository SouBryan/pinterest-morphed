package app.soubryan.patches.pinterest.tracking

import app.morphe.patcher.patch.resourcePatch
import app.soubryan.patches.pinterest.shared.Constants.COMPATIBILITY_PINTEREST
import org.w3c.dom.Element

/**
 * Removes the Bugsnag `API_KEY` `<meta-data>` from `AndroidManifest.xml`.
 *
 * Bugsnag reads this value during SDK auto-initialization. When it is missing,
 * the SDK aborts start-up (it logs a warning and returns early), which stops
 * both crash telemetry uploads and the native root-detection library from
 * being armed.
 */
@Suppress("unused")
val disableBugsnagPatch = resourcePatch(
    name = "Disable Bugsnag crash tracking",
    description = "Removes the Bugsnag API key metadata so the crash reporting SDK cannot initialize or upload telemetry.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    execute {
        document("AndroidManifest.xml").use { document ->
            val application = document
                .getElementsByTagName("application")
                .item(0) as Element

            val metas = application.getElementsByTagName("meta-data")

            for (i in metas.length - 1 downTo 0) {
                val meta = metas.item(i) as Element
                if (meta.getAttribute("android:name") == "com.bugsnag.android.API_KEY") {
                    application.removeChild(meta)
                }
            }
        }
    }
}
