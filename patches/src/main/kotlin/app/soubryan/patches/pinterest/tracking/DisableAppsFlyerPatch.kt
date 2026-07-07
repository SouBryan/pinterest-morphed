package app.soubryan.patches.pinterest.tracking

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.soubryan.patches.pinterest.shared.Constants.COMPATIBILITY_PINTEREST
import com.android.tools.smali.dexlib2.AccessFlags

private const val APPS_FLYER_LIB_CLASS = "Lcom/appsflyer/AppsFlyerLib;"

/**
 * Matches the concrete override of `AppsFlyerLib.isStopped()` that ships
 * inside the AppsFlyer SDK.
 *
 * The name `isStopped` is public API of the SDK and therefore stable across
 * both Pinterest and AppsFlyer releases. The subclass name itself
 * (`AFa1tSDK` in 14.25.0) is picked implicitly through the `custom` predicate
 * so this fingerprint keeps working if AppsFlyer reshuffles its internal
 * package.
 */
internal object AppsFlyerIsStoppedFingerprint : Fingerprint(
    name = "isStopped",
    returnType = "Z",
    parameters = emptyList(),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    custom = { _, classDef -> classDef.superclass == APPS_FLYER_LIB_CLASS },
)

/**
 * Matches the concrete override of
 * `AppsFlyerLib.init(String, AppsFlyerConversionListener, Context)`.
 */
internal object AppsFlyerInitFingerprint : Fingerprint(
    name = "init",
    returnType = APPS_FLYER_LIB_CLASS,
    parameters = listOf(
        "Ljava/lang/String;",
        "Lcom/appsflyer/AppsFlyerConversionListener;",
        "Landroid/content/Context;",
    ),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    custom = { _, classDef -> classDef.superclass == APPS_FLYER_LIB_CLASS },
)

/**
 * Neutralises the AppsFlyer install-attribution / tracking SDK.
 *
 * Two overrides on the concrete `AppsFlyerLib` subclass are rewritten:
 *
 *   * `isStopped()` — hard-coded to `true`. The SDK guards every event upload,
 *     conversion callback and background task with this check, so once it
 *     reports "stopped" nothing is transmitted.
 *   * `init(devKey, listener, context)` — turned into a no-op that returns
 *     `this`. The caller keeps a valid `AppsFlyerLib` reference (so method
 *     chaining like `AppsFlyerLib.getInstance().init(...).start(...)` still
 *     type-checks), but no state is set up.
 *
 * These two hooks are enough to disable install attribution, in-app event
 * uploads, uninstall tokens (`updateServerUninstallToken` bails out when the
 * SDK is stopped) and the deep-link resolver.
 */
@Suppress("unused")
val disableAppsFlyerPatch = bytecodePatch(
    name = "Disable AppsFlyer tracking",
    description = "Neutralises the AppsFlyer attribution / tracking SDK. init() becomes a no-op and isStopped() always returns true, so no events, installs, uninstalls or attribution data are transmitted.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    execute {
        AppsFlyerIsStoppedFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """,
        )

        AppsFlyerInitFingerprint.method.addInstructions(
            0,
            """
                return-object p0
            """,
        )
    }
}
