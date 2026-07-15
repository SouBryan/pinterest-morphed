package app.soubryan.patches.pinterest.ad

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.soubryan.patches.pinterest.shared.Constants.COMPATIBILITY_PINTEREST
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue

/**
 * Every Pinterest API model that describes a pin/story carries multiple
 * booleans that flag it as advertising material in different ways. If
 * *any* of them is truthy, the client renders the "Promoted"/"Shop now"
 * chrome, fires impression beacons and treats taps as click-outs.
 *
 * Setting `is_promoted = false` alone is not enough — Pinterest shopping
 * ads (the "Comprar agora" carousels on the home feed) often have
 * `is_promoted = false` but `promoted_is_catalog_carousel_ad = true` or
 * `is_native_content = true` or a non-null `ad_data`.
 *
 * The set below covers every boolean flag on `Pin`/`Story` models whose
 * name unambiguously means "this pin *is* an ad". Fields that only
 * *enable* an ad-adjacent capability (e.g.
 * `is_eligible_for_promoted_partnership`, `promoted_is_sideswipe_disabled`)
 * are deliberately excluded — they don't determine whether the pin
 * itself is treated as promoted.
 */
private val AD_INDICATOR_JSON_NAMES = setOf(
    // Core promoted-pin flags (Pin model).
    "is_promoted",
    "is_promoted_pin",
    "is_promoted_carousel_pin",
    "is_promoted_video",
    "is_downstream_promotion",
    // Sponsored-content flags (partnership / brand content).
    "is_sponsored",
    "is_sponsored_content",
    // Third-party demand / Dynamic Product Ads (Natura, Mercado Livre, etc).
    "is_third_party_ad",
    // Native / in-content ad slots.
    "is_native",
    "is_native_content",
    "is_eligible_for_in_content_ads",
    // Ad closeup / detail-page renderers.
    "is_ad_closeup_rp",
    "is_ad_closeup_ui",
    // Ad-only board and user profile flags (partner accounts).
    "is_ads_only",
    "is_ads_only_profile",
    // `promoted_is_*` family — every one of these means the pin should
    // be rendered as some flavour of promoted content.
    "promoted_is_auto_assembled",
    "promoted_is_catalog_carousel_ad",
    "promoted_is_congruency_enabled",
    "promoted_is_lead_ad",
    "promoted_is_max_video",
    "promoted_is_opaque_onetap_enabled",
    "promoted_is_personalized",
    "promoted_is_quiz",
    "promoted_is_showcase",
)

/**
 * Reads the `value = "..."` element off any Gson `@SerializedName`
 * annotation on `field`. Returns `null` if the field has no
 * `SerializedName`.
 *
 * The Gson annotation class itself is mangled by R8 (`Ldp/b;` on 14.25,
 * possibly something else on future builds), so we detect the annotation
 * by its `value` element instead of by class name.
 */
private fun serializedFieldNameOf(
    field: com.android.tools.smali.dexlib2.iface.Field,
): String? {
    for (anno in field.annotations) {
        for (element in anno.elements) {
            if (element.name == "value") {
                val value = element.value
                if (value is StringEncodedValue) {
                    return value.value
                }
            }
        }
    }
    return null
}

/**
 * Shared "the getter's `iget` targets an ad-indicator field whose
 * `SerializedName` is in [AD_INDICATOR_JSON_NAMES] and whose field type
 * matches `expectedFieldType`" check.
 */
private fun ClassDef.matchesAdBooleanGetter(
    instructions: List<com.android.tools.smali.dexlib2.iface.instruction.Instruction>,
    expectedFieldType: String,
): Boolean {
    val get = instructions[0] as? ReferenceInstruction ?: return false
    val field = get.reference as? FieldReference ?: return false

    // The getter must access a field on its own owning class — otherwise
    // it's likely a synthetic accessor or a delegate, not a plain getter.
    if (field.definingClass != this.type) return false
    if (field.type != expectedFieldType) return false

    val classField = this.fields.firstOrNull { it.name == field.name } ?: return false
    return serializedFieldNameOf(classField) in AD_INDICATOR_JSON_NAMES
}

/**
 * Matches a trivial 2-opcode getter whose body is:
 *
 *   `iget-boolean <r>, p0, <owner>-><boolField>:Z`
 *   `return <r>`
 *
 * on any owner class that has *this exact* field annotated with a Gson
 * `@SerializedName` whose value is in [AD_INDICATOR_JSON_NAMES]. Only
 * primitive `boolean` (bytecode type `Z`) fields are considered — the
 * boxed [BoxedBooleanAdFlagGetterFingerprint] handles `Boolean` fields.
 */
internal object PrimitiveBooleanAdFlagGetterFingerprint : Fingerprint(
    returnType = "Z",
    parameters = emptyList(),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    custom = { method, classDef ->
        val instructions = method.implementation?.instructions?.toList()
        val bodyMatches = instructions?.size == 2 &&
            instructions[0].opcode == Opcode.IGET_BOOLEAN &&
            instructions[1].opcode == Opcode.RETURN
        bodyMatches && classDef.matchesAdBooleanGetter(instructions!!, "Z")
    },
)

/**
 * Boxed-`Boolean` counterpart of [PrimitiveBooleanAdFlagGetterFingerprint].
 *
 * Matches:
 *
 *   `iget-object <r>, p0, <owner>-><boxedField>:Ljava/lang/Boolean;`
 *   `return-object <r>`
 *
 * where the accessed field is one of [AD_INDICATOR_JSON_NAMES] on the
 * owner class. Most `promoted_is_*` fields on the Pin model are declared
 * as boxed `Boolean` (Gson deserializes JSON `true`/`false`/`null` into
 * `Boolean.TRUE`/`FALSE`/`null`, so a boxed reference is required to
 * preserve the tri-state).
 */
internal object BoxedBooleanAdFlagGetterFingerprint : Fingerprint(
    returnType = "Ljava/lang/Boolean;",
    parameters = emptyList(),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    custom = { method, classDef ->
        val instructions = method.implementation?.instructions?.toList()
        val bodyMatches = instructions?.size == 2 &&
            instructions[0].opcode == Opcode.IGET_OBJECT &&
            instructions[1].opcode == Opcode.RETURN_OBJECT
        bodyMatches &&
            classDef.matchesAdBooleanGetter(instructions!!, "Ljava/lang/Boolean;")
    },
)

/**
 * Matches a trivial 2-opcode getter of a non-boolean object field whose
 * `@SerializedName("ad_data")` marks it as the pin's ad payload:
 *
 *   `iget-object <r>, p0, Lue;->d:Lcom/pinterest/api/model/e;`
 *   `return-object <r>`
 *
 * Downstream code branches on `pin.getAdData() != null` to decide
 * whether the pin is treated as an ad even when every boolean flag is
 * false. Forcing this getter to return `null` closes that last hole.
 */
internal object AdDataGetterFingerprint : Fingerprint(
    parameters = emptyList(),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    custom = { method, classDef ->
        val returnType = method.returnType
        val isReferenceReturn = returnType.startsWith("L") && returnType.endsWith(";")
        val instructions = method.implementation?.instructions?.toList()
        val bodyMatches = isReferenceReturn &&
            instructions?.size == 2 &&
            instructions[0].opcode == Opcode.IGET_OBJECT &&
            instructions[1].opcode == Opcode.RETURN_OBJECT
        if (!bodyMatches) {
            false
        } else {
            val get = instructions!![0] as? ReferenceInstruction
            val field = get?.reference as? FieldReference
            val matchesAdData = field != null &&
                field.definingClass == classDef.type &&
                field.type == returnType &&
                classDef.fields.firstOrNull { it.name == field.name }
                    ?.let { serializedFieldNameOf(it) == "ad_data" } == true
            matchesAdData
        }
    },
)

/**
 * Hides every kind of ad the Pinterest client can render — Promoted Pins,
 * catalog-carousel shopping ads ("Comprar agora"), native content ads,
 * downstream promotions, in-content ads, quiz ads, showcase ads, video
 * lead ads.
 *
 * The Pinterest `Pin`/`Story` models flag ads via ~14 booleans and one
 * non-null `ad_data` object. Downstream code branches on any of them to:
 *
 *   * render the "Promoted"/"Shop now"/"Comprar agora" chrome
 *   * insert the pin into an ad slot in the home feed or search results
 *   * fire impression / click-out tracking beacons
 *   * open the advertiser deep-link instead of the normal pin closeup
 *
 * This patch rewrites every trivial getter (`iget + return`) that reads
 * one of those ad-indicator fields:
 *
 *   * primitive `boolean` getters → return `false`
 *   * boxed `Boolean` getters → return `Boolean.FALSE`
 *   * `ad_data` object getter → return `null`
 *
 * The class holding the field is identified structurally by its Gson
 * `@SerializedName("<jsonName>")` annotation, so the patch survives R8
 * remangling class and method names (`Ldp/b;`, `Ljx2/v;`, etc). The
 * accessed field is identified by its `SerializedName` too, so we never
 * touch non-ad boolean getters like `is_favorited` or `is_muted`.
 */
@Suppress("unused")
val hidePromotedPinsPatch = bytecodePatch(
    name = "Hide promoted pins",
    description = "Neutralises every ad-indicator field on the Pinterest pin/story models (is_promoted, promoted_is_*, is_native, ad_data, ...) so Promoted Pins, shopping-carousel ads, native-content ads and click-out CTAs are never rendered or fired.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    execute {
        var patched = 0

        PrimitiveBooleanAdFlagGetterFingerprint.matchAll().forEach { match ->
            match.method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """,
            )
            patched++
        }

        BoxedBooleanAdFlagGetterFingerprint.matchAll().forEach { match ->
            match.method.addInstructions(
                0,
                """
                    sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                    return-object v0
                """,
            )
            patched++
        }

        AdDataGetterFingerprint.matchAll().forEach { match ->
            match.method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """,
            )
            patched++
        }

        if (patched == 0) {
            throw app.morphe.patcher.patch.PatchException(
                "No ad-indicator getters matched — the Pinterest API model shape may have changed.",
            )
        }
    }
}
