package app.soubryan.patches.pinterest.tracking

import app.morphe.patcher.patch.resourcePatch
import app.soubryan.patches.pinterest.shared.Constants.COMPATIBILITY_PINTEREST
import org.w3c.dom.Element

/**
 * Forces Google Analytics for Firebase into a permanent opt-out state by
 * flipping the four `google_analytics_default_allow_*` `<meta-data>` values
 * to `false`.
 *
 * These flags are the officially documented Google consent gates
 * (https://developers.google.com/tag-platform/security/guides/app-consent).
 * When set to `false`, the Firebase Analytics / Measurement SDK still
 * initializes but does not collect analytics, ad data, ad-user data or
 * personalization signals until the user grants consent — which the app never
 * does. The net effect is that no analytics or advertising events are
 * transmitted.
 *
 * Combined with [disableFirebaseAnalyticsInitPatch] (bytecode), this provides
 * defense in depth: the SDK is both starved of consent and never asked to
 * initialize.
 */
@Suppress("unused")
val optOutGoogleAnalyticsPatch = resourcePatch(
    name = "Opt out of Google Analytics",
    description = "Sets the default Google Analytics consent flags to false so the Firebase Measurement SDK does not collect analytics, ad data or personalization signals.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    execute {
        val flagsToDisable = setOf(
            "google_analytics_default_allow_analytics_storage",
            "google_analytics_default_allow_ad_storage",
            "google_analytics_default_allow_ad_user_data",
            "google_analytics_default_allow_ad_personalization_signals",
        )

        document("AndroidManifest.xml").use { document ->
            val application = document
                .getElementsByTagName("application")
                .item(0) as Element

            val metas = application.getElementsByTagName("meta-data")

            for (i in 0 until metas.length) {
                val meta = metas.item(i) as Element
                if (meta.getAttribute("android:name") in flagsToDisable) {
                    meta.setAttribute("android:value", "false")
                }
            }
        }
    }
}
