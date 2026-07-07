package app.soubryan.patches.pinterest.ad

import app.morphe.patcher.patch.resourcePatch
import app.soubryan.patches.pinterest.shared.Constants.COMPATIBILITY_PINTEREST
import org.w3c.dom.Element

/**
 * Disables Android Privacy Sandbox / Ad Services integration.
 *
 * Starting with Android 13 (API 33), the framework offers the "Privacy
 * Sandbox on Android" ad APIs (Topics, Custom Audience, Attribution
 * Reporting). Apps opt in by declaring:
 *
 *   `<property android:name="android.adservices.AD_SERVICES_CONFIG"
 *              android:resource="@xml/ga_ad_services_config" />`
 *
 * The referenced XML enables Topics, Attribution Reporting and Custom
 * Audience so the on-device system can profile the user for advertisers.
 * Removing the property makes the framework treat the app as opted out
 * (the sandbox APIs return `AdServicesException` / disabled) without
 * requiring any changes to the referenced resource itself.
 */
@Suppress("unused")
val disablePrivacySandboxAdServicesPatch = resourcePatch(
    name = "Disable Android Privacy Sandbox Ad Services",
    description = "Removes the AD_SERVICES_CONFIG property so Android Privacy Sandbox ad APIs (Topics, Attribution Reporting, Custom Audiences) are not opted into.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    execute {
        document("AndroidManifest.xml").use { document ->
            val application = document
                .getElementsByTagName("application")
                .item(0) as Element

            val properties = application.getElementsByTagName("property")
            for (i in properties.length - 1 downTo 0) {
                val prop = properties.item(i) as Element
                if (prop.getAttribute("android:name") == "android.adservices.AD_SERVICES_CONFIG") {
                    application.removeChild(prop)
                }
            }
        }
    }
}
