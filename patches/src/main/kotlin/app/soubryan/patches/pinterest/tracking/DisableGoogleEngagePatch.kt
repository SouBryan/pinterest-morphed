package app.soubryan.patches.pinterest.tracking

import app.morphe.patcher.patch.resourcePatch
import app.soubryan.patches.pinterest.shared.Constants.COMPATIBILITY_PINTEREST
import org.w3c.dom.Element

/**
 * Neutralises the Google Engage SDK integration.
 *
 * Google Engage lets an app "publish" recommendations back to Google
 * (Discover, Assistant, Play Store, Entertainment Space, etc.). To do that,
 * Pinterest registers `com.pinterest.engage.GoogleEngageBroadcastReceiver`
 * with the following actions:
 *
 *   * `com.google.android.engage.action.PUBLISH_RECOMMENDATION` — fires when
 *     Google requests fresh content
 *   * `com.pinterest.unauth.ACTION_USER_LOG_IN_SUCCESS` — fires on sign-in
 *   * `com.pinterest.unauth.ACTION_USER_LOG_OUT_SUCCESS` — fires on sign-out
 *
 * On receive, the receiver schedules a `GoogleEngageWorker` that packages up
 * user-facing content and hands it to Google's Engage service. Removing the
 * `<receiver>` and its metadata prevents any of that from happening: the
 * receiver is never resolved by the system, the worker is never scheduled,
 * and no recommendations are transmitted.
 */
@Suppress("unused")
val disableGoogleEngagePatch = resourcePatch(
    name = "Disable Google Engage integration",
    description = "Removes the Google Engage broadcast receiver so Pinterest cannot publish content recommendations back to Google (Discover, Assistant, Play Store, etc.).",
    default = true,
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    execute {
        document("AndroidManifest.xml").use { document ->
            val application = document
                .getElementsByTagName("application")
                .item(0) as Element

            // Remove the receiver itself.
            val receivers = application.getElementsByTagName("receiver")
            for (i in receivers.length - 1 downTo 0) {
                val receiver = receivers.item(i) as Element
                if (receiver.getAttribute("android:name") == "com.pinterest.engage.GoogleEngageBroadcastReceiver") {
                    application.removeChild(receiver)
                }
            }

            // Remove the Engage environment metadata (only meaningful when the receiver is present).
            val metas = application.getElementsByTagName("meta-data")
            for (i in metas.length - 1 downTo 0) {
                val meta = metas.item(i) as Element
                if (meta.getAttribute("android:name") == "com.google.android.engage.service.ENV") {
                    application.removeChild(meta)
                }
            }
        }
    }
}
