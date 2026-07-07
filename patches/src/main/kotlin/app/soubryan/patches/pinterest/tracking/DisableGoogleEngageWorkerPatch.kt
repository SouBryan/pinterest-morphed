package app.soubryan.patches.pinterest.tracking

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.soubryan.patches.pinterest.shared.Constants.COMPATIBILITY_PINTEREST
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Matches `com.pinterest.engage.GoogleEngageWorker.createWork()` — the
 * RxWorker override that Pinterest schedules once per day to hand
 * user-facing content over to Google's Engage service (Discover,
 * Assistant, Play Store, ...).
 *
 * The Pinterest-owned class name is preserved by R8, but the method name
 * is mangled to a single letter and the return type (RxJava's `Single`)
 * changes package name between Pinterest releases (`jx2/v` on 14.25,
 * something else on 14.24 and 14.26). We identify the target by
 *
 *   * `definingClass` (stable Pinterest-owned name)
 *   * `PUBLIC` non-static, no parameters
 *   * `custom` predicate that only accepts a method whose return type is
 *     a reference type — RxJava's `Single` is always an object, never a
 *     primitive or void
 *
 * That is enough to uniquely nail the abstract override.
 */
internal object GoogleEngageWorkerCreateWorkFingerprint : Fingerprint(
    definingClass = "Lcom/pinterest/engage/GoogleEngageWorker;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = emptyList(),
    custom = { method, _ ->
        val returnType = method.returnType
        returnType.startsWith("L") && returnType.endsWith(";")
    },
)

/**
 * Neutralises the Google Engage RxWorker even though it is still scheduled
 * from `ReleaseHiltApplication.onCreate` (and from the removed broadcast
 * receiver).
 *
 * The rewritten `createWork()` simply returns `null`. WorkManager treats
 * this as a wrapped `NullPointerException` and marks the run as a
 * retryable failure. Because the schedule is periodic with exponential
 * back-off, the worker quickly settles into "one attempt every few hours,
 * always failing" — no data ever reaches Google Engage.
 *
 * The alternative (returning `Single.just(Result.success())`) requires
 * hard-coding the mangled RxJava `Single` class, which changes name
 * between Pinterest builds. Returning `null` is bytecode-agnostic and
 * therefore identical on every 14.2x release.
 *
 * Requires [disableGoogleEngagePatch] (resource) to be applied as well so
 * the `<receiver>` doesn't re-schedule one-off runs on login/logout.
 */
@Suppress("unused")
val disableGoogleEngageWorkerPatch = bytecodePatch(
    name = "Disable Google Engage worker",
    description = "Rewrites GoogleEngageWorker.createWork() to return null so WorkManager fails the periodic job and no content recommendations are ever published to Google.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    execute {
        GoogleEngageWorkerCreateWorkFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """,
        )
    }
}
