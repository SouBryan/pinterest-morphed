package app.soubryan.patches.pinterest.tracking

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import app.soubryan.patches.pinterest.shared.Constants.COMPATIBILITY_PINTEREST
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private const val URL_SANITIZER_CLASS =
    "Lapp/soubryan/extension/pinterest/UrlSanitizer;"

/**
 * Matches every method in the app that calls
 * `ClipboardManager.setPrimaryClip(ClipData)`.
 *
 * `ClipboardManager` is an Android platform class whose class + method
 * names are guaranteed stable across every Android release, so this
 * fingerprint is unaffected by Pinterest's R8 remangling. The
 * `methodCall` filter is what actually locates the call inside the
 * matching method — we don't care about the enclosing method's shape,
 * only that it makes that specific SDK call.
 */
internal object SetPrimaryClipCallFingerprint : Fingerprint(
    filters = listOf(
        methodCall(
            definingClass = "Landroid/content/ClipboardManager;",
            name = "setPrimaryClip",
            parameters = listOf("Landroid/content/ClipData;"),
            returnType = "V",
        ),
    ),
)

/**
 * Runs every `ClipData` about to be pushed to the system clipboard
 * through [`UrlSanitizer.sanitizeClipData`][URL_SANITIZER_CLASS] first,
 * so the "Copy link" flow can no longer paste a Pinterest short URL
 * that carries a per-share fingerprint.
 *
 * The Pinterest client uses `pin.it/<slug>` and
 * `pinterest.<tld>/url_shortener/<slug>/redirect/` for shared links.
 * The slug itself encodes who shared the link and who opened it, so
 * stripping query parameters isn't enough — the whole path has to be
 * resolved to the canonical `pinterest.com/pin/<id>/` form. The
 * sanitiser does that with a bounded HTTP HEAD request (worker thread,
 * 3-second timeout, falls back to the original URL on any failure).
 *
 * Every call site of `ClipboardManager.setPrimaryClip(ClipData)` in the
 * APK is patched, so this covers the pin overflow-menu "Copy link", the
 * long-press-on-URL flow in comments, the invite-link buttons, and any
 * other clipboard-write path Pinterest might reach. The sanitiser is
 * defensive — if the clipped text is not a Pinterest URL, the original
 * `ClipData` is returned by reference, so non-URL copies (search text,
 * comment drafts, ...) pay no cost.
 */
@Suppress("unused")
val sanitizeCopyLinkPatch = bytecodePatch(
    name = "Sanitize copied links",
    description = "Resolves Pinterest short URLs (pin.it/…, pinterest.com/url_shortener/…) to their canonical pin URL before they are placed on the system clipboard, so \"Copy link\" no longer produces a fingerprinted short link.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    extendWith("extensions/pinterest.mpe")

    execute {
        // matchAll() covers every enclosing method that pushes to the
        // system clipboard. Iterate every match, and inside each match
        // iterate every setPrimaryClip call — a single method may write
        // to the clipboard from multiple branches (share vs. long-press,
        // for example).
        SetPrimaryClipCallFingerprint.matchAll().forEach { match ->
            match.method.apply {
                // Walk the instruction list from the end so injecting
                // extra instructions before an earlier call doesn't
                // shift the index of a later call we still need to
                // patch.
                val callSites = implementation!!.instructions
                    .withIndex()
                    .filter { (_, instruction) ->
                        instruction.opcode == Opcode.INVOKE_VIRTUAL ||
                            instruction.opcode == Opcode.INVOKE_VIRTUAL_RANGE
                    }
                    .map { it.index }
                    .reversed()

                for (index in callSites) {
                    val instruction = getInstruction<Instruction>(index)
                    // Every `invoke-virtual` implements both
                    // ReferenceInstruction (for the callee method) and
                    // FiveRegisterInstruction (for the argument regs),
                    // but only the ReferenceInstruction cast exposes the
                    // method reference, so filter by that first.
                    val ref = (instruction as? ReferenceInstruction)?.reference?.toString()
                        ?: continue
                    if (!ref.contains("Landroid/content/ClipboardManager;->setPrimaryClip")) {
                        continue
                    }
                    // For an `invoke-virtual` the argument order in the
                    // registers is (this, arg0, arg1, …). setPrimaryClip
                    // takes a single ClipData, so registerD is the clip.
                    val clipDataReg = (instruction as FiveRegisterInstruction).registerD

                    addInstructions(
                        index,
                        """
                            invoke-static { v$clipDataReg }, $URL_SANITIZER_CLASS->sanitizeClipData(Landroid/content/ClipData;)Landroid/content/ClipData;
                            move-result-object v$clipDataReg
                        """,
                    )
                }
            }
        }
    }
}
