package app.soubryan.patches.pinterest.tracking

import app.morphe.patcher.patch.resourcePatch
import app.soubryan.patches.pinterest.shared.Constants.COMPATIBILITY_PINTEREST
import org.w3c.dom.Element

/**
 * Adds Firebase / Google Analytics opt-out `<meta-data>` entries to
 * `AndroidManifest.xml`.
 *
 * These are the officially documented off-switches for the Firebase family
 * of SDKs — the client reads them at process start and never uploads any
 * event / crash / performance data once they are set:
 *
 *   * `firebase_analytics_collection_deactivated = true`
 *     Firebase Analytics goes dormant. No `logEvent`, no `setUserProperty`,
 *     no session pings.
 *
 *   * `firebase_crashlytics_collection_enabled = false`
 *     Crashlytics never uploads stack traces or user identifiers. (Pinterest
 *     ships Bugsnag as its primary crash reporter — this closes the door
 *     on any residual Crashlytics wiring.)
 *
 *   * `firebase_performance_collection_deactivated = true`
 *     Firebase Performance Monitoring stops sampling network + rendering
 *     traces.
 *
 *   * `google_analytics_adid_collection_enabled = false`
 *     Belt-and-braces guard for the Firebase → Google Analytics bridge.
 *     Even if the client tries to read the Advertising ID (which is already
 *     removed by [`removeAdIdPermissionPatch`]), the Measurement SDK is
 *     told to drop it on the floor.
 *
 * These flags are additive: [`optOutGoogleAnalyticsPatch`] flips the
 * *consent* gates (`google_analytics_default_allow_*`) already declared by
 * the app, this patch adds the SDK-level *initialization* off-switches
 * that the app itself does not declare. Together they cover both the
 * "starve the SDK of consent" and the "never initialize it in the first
 * place" strategies.
 *
 * If any of these `<meta-data>` entries somehow already exists in the
 * manifest, the existing value is overwritten — the patch never fails
 * because of a duplicate.
 */
@Suppress("unused")
val disableFirebaseServicesPatch = resourcePatch(
    name = "Disable Firebase services",
    description = "Adds the officially documented Firebase off-switches (`firebase_analytics_collection_deactivated`, `firebase_crashlytics_collection_enabled=false`, `firebase_performance_collection_deactivated`, `google_analytics_adid_collection_enabled=false`) so the Firebase Analytics, Crashlytics and Performance SDKs go dormant.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    execute {
        // Manifest attribute name → target value.
        // "true" for `_deactivated` flags (Firebase reads them as "turn it off"),
        // "false" for `_enabled` / `_collection_enabled` flags.
        val flagsToSet = mapOf(
            "firebase_analytics_collection_deactivated" to "true",
            "firebase_crashlytics_collection_enabled" to "false",
            "firebase_performance_collection_deactivated" to "true",
            "google_analytics_adid_collection_enabled" to "false",
        )

        document("AndroidManifest.xml").use { document ->
            val application = document
                .getElementsByTagName("application")
                .item(0) as Element

            // Pass 1: update any existing entries in place.
            val alreadyDeclared = mutableSetOf<String>()
            val existingMetas = application.getElementsByTagName("meta-data")
            for (i in 0 until existingMetas.length) {
                val meta = existingMetas.item(i) as Element
                val name = meta.getAttribute("android:name")
                val newValue = flagsToSet[name] ?: continue
                meta.setAttribute("android:value", newValue)
                alreadyDeclared += name
            }

            // Pass 2: append the missing entries as new <meta-data> elements.
            for ((name, value) in flagsToSet) {
                if (name in alreadyDeclared) continue
                val meta = document.createElement("meta-data")
                meta.setAttribute("android:name", name)
                meta.setAttribute("android:value", value)
                application.appendChild(meta)
            }
        }
    }
}
