package app.soubryan.patches.pinterest.tracking

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.soubryan.patches.pinterest.shared.Constants.COMPATIBILITY_PINTEREST
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Matches `com.pinterest.engage.GoogleEngageWorker.createWork()` — the RxWorker
 * override that Pinterest schedules once per day to hand user-facing content
 * over to Google's Engage service (Discover, Assistant, Play Store, ...).
 *
 * The Kotlin/Java method name has been mangled by R8 to a single letter, so
 * we fingerprint on class + shape:
 *   * `PUBLIC FINAL` non-static
 *   * no parameters
 *   * returns `io.reactivex.Single` (mangled to `Ljx2/v;` in this build)
 *
 * The Single type is anchored to the 14.25.0 mapping (`jx2/v`) — if a future
 * Pinterest release reshuffles RxJava's mangling, the fingerprint fails
 * cleanly and the patch is skipped instead of applying incorrectly.
 */
internal object GoogleEngageWorkerCreateWorkFingerprint : Fingerprint(
    definingClass = "Lcom/pinterest/engage/GoogleEngageWorker;",
    returnType = "Ljx2/v;",
    parameters = emptyList(),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
)

/**
 * Neutralises the Google Engage RxWorker even though it is still scheduled by
 * `ReleaseHiltApplication.onCreate` and the (now removed) broadcast receiver.
 *
 * The rewritten `createWork()` simply returns `Single.just(Result.success())`,
 * which:
 *   * makes the WorkManager mark the run as successful
 *   * skips every network call the worker would otherwise perform
 *   * keeps the periodic schedule alive (harmlessly) so the app doesn't spam
 *     retries with backoff
 *
 * Requires [disableGoogleEngagePatch] (resource) to be applied as well so
 * the `<receiver>` doesn't re-schedule one-off runs on login/logout.
 */
@Suppress("unused")
val disableGoogleEngageWorkerPatch = bytecodePatch(
    name = "Disable Google Engage worker",
    description = "Rewrites GoogleEngageWorker.createWork() to a no-op that returns Result.success() so no content recommendations are ever published to Google.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    execute {
        GoogleEngageWorkerCreateWorkFingerprint.method.addInstructions(
            0,
            """
                invoke-static {}, Landroidx/work/ListenableWorker${'$'}Result;->success()Landroidx/work/ListenableWorker${'$'}Result;
                move-result-object v0
                invoke-static { v0 }, Ljx2/v;->g(Ljava/lang/Object;)Ljx2/v;
                move-result-object v0
                return-object v0
            """,
        )
    }
}
