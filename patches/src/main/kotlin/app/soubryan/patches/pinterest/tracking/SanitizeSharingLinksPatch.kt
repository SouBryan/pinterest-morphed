package app.soubryan.patches.pinterest.tracking

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.soubryan.patches.pinterest.shared.Constants.COMPATIBILITY_PINTEREST
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val URL_SANITIZER_CLASS =
    "Lapp/soubryan/extension/pinterest/UrlSanitizer;"

/**
 * Matches the private static factory the Pinterest client uses to build the
 * Android share-sheet `Intent` (in 14.25.0 the mangled name is
 * `kq2.d0.a(qj0.c)`). Every share flow in the app funnels through this
 * method, so patching it here catches WhatsApp, Messenger, Line, Twitter,
 * SMS, email and "copy link" in one shot.
 *
 * The class + method + parameter names have been mangled by R8, but the
 * string literals `"android.intent.action.SEND"`, `"text/plain"`,
 * `"android.intent.extra.TEXT"`, `"message"` and `"title"` all appear inside
 * it exactly once and nowhere else in the app together. Fingerprinting on
 * that constellation of strings therefore survives obfuscation shuffles.
 */
internal object ShareIntentBuilderFingerprint : Fingerprint(
    returnType = "Landroid/content/Intent;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    strings = listOf(
        "android.intent.action.SEND",
        "text/plain",
        "android.intent.extra.TEXT",
        "message",
        "title",
    ),
)

/**
 * Sanitises the URL Pinterest hands to third-party apps on the Android
 * share sheet.
 *
 * The Pinterest backend serves the share text pre-baked with a long tail of
 * UTM/attribution parameters (`utm_source`, `utm_medium`, `utm_campaign`,
 * `sfo`, `sender`, ...) so it can measure re-shares off-platform. Users
 * copying a pin link expect to hand friends `https://pinterest.com/pin/<id>`,
 * not `https://pinterest.com/pin/<id>?utm_source=android_share&sender=...`.
 *
 * This patch appends a call to [`UrlSanitizer.sanitizeShareIntent`]
 * immediately before the share-Intent factory returns, so every downstream
 * caller (WhatsApp, Messenger, Line, SMS, "copy link", ...) receives an
 * intent whose `EXTRA_TEXT` has been scrubbed of tracking parameters.
 * Non-tracking params on the URL are preserved.
 */
@Suppress("unused")
val sanitizeSharingLinksPatch = bytecodePatch(
    name = "Sanitize sharing links",
    description = "Strips UTM and click-ID tracking parameters from the URL the app puts on the Android share sheet, so friends receive clean pin links.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    extendWith("extensions/pinterest.mpe")

    execute {
        ShareIntentBuilderFingerprint.method.apply {
            // Locate the trailing `return-object <reg>` and inject a
            // sanitiser call that rewrites the value in `<reg>` before the
            // method actually returns. The register may be anything from
            // v0..v15 depending on how R8 allocated it in this build; we
            // read it back off the return instruction so the patch stays
            // register-agnostic.
            val instructions = implementation!!.instructions.toList()
            val returnIndex = instructions
                .indexOfLast { it.opcode == Opcode.RETURN_OBJECT }
            val returnReg = getInstruction<OneRegisterInstruction>(returnIndex).registerA

            addInstructions(
                returnIndex,
                """
                    invoke-static { v$returnReg }, $URL_SANITIZER_CLASS->sanitizeShareIntent(Landroid/content/Intent;)Landroid/content/Intent;
                    move-result-object v$returnReg
                """,
            )
        }
    }
}
