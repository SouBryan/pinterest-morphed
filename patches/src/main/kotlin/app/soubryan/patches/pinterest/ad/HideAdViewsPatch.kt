package app.soubryan.patches.pinterest.ad

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.soubryan.patches.pinterest.shared.Constants.COMPATIBILITY_PINTEREST
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * Class names of ad-specific views that the Pinterest client instantiates
 * to render ad chrome ("Sponsored" / "Comprar agora" / "Shop now" / CTA
 * buttons / advertiser attribution).
 *
 * Neutralising [hidePromotedPinsPatch] stops the client from *treating*
 * a pin as an ad — no impression beacons, no click-out — but the JSON
 * that lands in the very first home-feed payload can still be structured
 * so that the client instantiates these views for the initial render.
 * We collapse them to zero height on first construction so nothing ever
 * appears on screen even if the client somehow decided to instantiate
 * one.
 *
 * Class names are Pinterest-owned (not Gson-serialized model classes) and
 * therefore preserved by R8 across every 14.2x build. If a future Pinterest
 * release refactors one of these views to a different fully-qualified name
 * the corresponding fingerprint just fails and the patch is skipped —
 * every other entry keeps working.
 */
private val AD_VIEW_CLASSES = listOf(
    "Lcom/pinterest/featurelibrary/textads/TextAdView;",
    "Lcom/pinterest/activity/pin/view/modules/LegacyPromotedCloseupActionButtonModule;",
    "Lcom/pinterest/feature/board/detail/header/view/lego/BoardSponsoredCuratorView;",
    "Lcom/pinterest/feature/pin/closeup/view/PromotedPinCloseupFloatingActionBarModule;",
)

/**
 * Builds a fingerprint that matches the widest constructor
 * `<init>(Context, AttributeSet, int)` on the given ad-view class. R8
 * keeps class names for Pinterest-owned views, so anchoring on
 * `definingClass` + constructor signature is stable.
 *
 * Kotlin `@JvmOverloads` on the view usually generates three overloads
 * (`(Context)`, `(Context, AttributeSet)`, `(Context, AttributeSet, int)`)
 * that all chain into the 3-arg one, so patching the 3-arg constructor is
 * enough to also cover the other two.
 */
private fun adViewCtorFingerprint(className: String): Fingerprint = Fingerprint(
    definingClass = className,
    name = "<init>",
    returnType = "V",
    parameters = listOf(
        "Landroid/content/Context;",
        "Landroid/util/AttributeSet;",
        "I",
    ),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
)

/**
 * Hides every ad-specific view the Pinterest client can render — the
 * "Comprar agora" / "Shop now" text-ad cards on the home feed, promoted
 * closeup action bars, board sponsor headers, etc.
 *
 * For each class in [AD_VIEW_CLASSES] the widest constructor is located
 * and a `setVisibility(GONE)` call is appended right before its
 * `return-void`. The rest of the constructor still runs — Dagger/Hilt
 * component injection, layout inflation, listener wiring — so downstream
 * code that keeps a reference to the view doesn't NPE on things it
 * expects to be non-null. The view simply never draws and takes zero
 * layout space (`View.GONE` skips both measure and draw), so no ad ever
 * reaches the screen.
 *
 * This is a defence-in-depth companion to [hidePromotedPinsPatch]. In
 * theory the model-level flag rewrites should keep the client from
 * building any of these views, but Pinterest can also decide to
 * instantiate them from the feed adapter regardless of pin flags (for
 * example on the very first home-feed render after a fresh install,
 * before any client-side filtering has taken effect). This patch closes
 * that window.
 *
 * The patch does not fail if some of the class names have been renamed;
 * it counts matches across all fingerprints and only errors out if
 * *none* of them matched (which would mean the ad-view surface has
 * changed enough to need a code update).
 */
@Suppress("unused")
val hideAdViewsPatch = bytecodePatch(
    name = "Hide ad views",
    description = "Collapses Pinterest's ad-specific views (TextAdView, promoted closeup action bars, board sponsor headers, …) to zero-size on construction, so ad chrome never draws even if the feed adapter tries to render one.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    execute {
        // View.GONE == 8
        val gonePrelude = """
            const/16 v0, 0x8
            invoke-virtual { p0, v0 }, Landroid/view/View;->setVisibility(I)V
        """.trimIndent()

        var patched = 0
        for (className in AD_VIEW_CLASSES) {
            val fingerprint = adViewCtorFingerprint(className)
            val match = fingerprint.matchOrNull() ?: continue

            val method = match.method
            val instructions = method.implementation?.instructions?.toList() ?: continue
            val returnIndex = instructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
            if (returnIndex < 0) continue

            // Insert `setVisibility(GONE)` right before the constructor's
            // trailing `return-void`. Placing it at the end means every
            // field initialisation, Dagger injection and layout inflation
            // that the original ctor did still runs — downstream code
            // that keeps a reference to `this` doesn't NPE.
            method.addInstructions(returnIndex, gonePrelude)
            patched++
        }

        if (patched == 0) {
            throw app.morphe.patcher.patch.PatchException(
                "No ad-view constructors matched — the Pinterest ad-rendering surface has changed and this patch needs an update.",
            )
        }
    }
}
