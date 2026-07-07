package app.soubryan.patches.pinterest.ad

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.soubryan.patches.pinterest.shared.Constants.COMPATIBILITY_PINTEREST
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue

/**
 * The Gson `@SerializedName` annotation is renamed by R8 to a single-letter
 * class inside the `dp` package. In every 14.2x build inspected so far it
 * lands on `Ldp/b;`. The bare `value()` string element is preserved because
 * Gson reads it via reflection at runtime.
 */
private const val GSON_SERIALIZED_NAME_ANNOTATION = "Ldp/b;"

/**
 * Predicate that returns `true` when a class has any field annotated with
 * `@dp.b("<jsonName>")`.
 *
 * Used as a `custom` filter on fingerprints so we can restrict matches to
 * Pinterest API model classes that carry a specific JSON field.
 */
private fun classHasFieldNamed(jsonName: String): (Any, com.android.tools.smali.dexlib2.iface.ClassDef) -> Boolean =
    { _, classDef ->
        classDef.fields.any { field ->
            field.annotations.any { anno ->
                anno.type == GSON_SERIALIZED_NAME_ANNOTATION &&
                    anno.elements.any { element ->
                        element.name == "value" &&
                            (element.value as? StringEncodedValue)?.value == jsonName
                    }
            }
        }
    }

/**
 * Matches trivial boolean getters (`iget-boolean` immediately followed by
 * `return`) on any class that carries a field annotated
 * `@dp.b("is_promoted")`.
 *
 * The fingerprint intentionally does NOT nail down the method name — R8
 * mangles every getter in the Pinterest API models to a single letter and
 * the mapping changes between builds. Instead we rely on:
 *
 *   * exact bytecode shape (2 opcodes only, no branching)
 *   * exact signature (`public final boolean noArgs()`)
 *   * exact class shape (must have `@dp.b("is_promoted")` on some field)
 *
 * That combination uniquely identifies the `getIsPromoted()` getter on each
 * Pinterest model that ships an `is_promoted` field (`ue`, `x5`, `qj`, `o7`, ...).
 */
internal object IsPromotedGetterFingerprint : Fingerprint(
    returnType = "Z",
    parameters = emptyList(),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    custom = { method, classDef ->
        // Must be exactly the "return this.field" bytecode shape.
        val instructions = method.implementation?.instructions?.toList()
        val shapeMatches = instructions?.size == 2 &&
            instructions[0].opcode == Opcode.IGET_BOOLEAN &&
            (instructions[1].opcode == Opcode.RETURN ||
                instructions[1].opcode == Opcode.RETURN_WIDE)

        shapeMatches && classHasFieldNamed("is_promoted")(method, classDef)
    },
)

/**
 * Hides promoted pins throughout the app by neutralising every
 * `boolean isPromoted` getter on every Pinterest API model that ships an
 * `is_promoted` JSON field.
 *
 * The Pinterest client marks a pin/story as an ad via `Pin.getIsPromoted()`
 * (mangled to `p()` or a single letter). Downstream code branches on this
 * boolean to:
 *
 *   * render the "Promoted" badge and click-out CTA
 *   * fire ad-impression tracking beacons
 *   * insert the pin into ad slots inside the home feed and search results
 *
 * Forcing every such getter to return `false` makes every rendered pin look
 * organic to the rest of the app, so no ad chrome is drawn, no ad beacons
 * are sent, and no click-out flow is triggered.
 *
 * The pins themselves still appear in the feed (we don't strip them out of
 * the underlying list), but they behave as regular content. A future patch
 * can also filter them out of the feed-loader response for a truly zero-ad
 * feed.
 */
@Suppress("unused")
val hidePromotedPinsPatch = bytecodePatch(
    name = "Hide promoted pins",
    description = "Overrides every `isPromoted` getter on the Pinterest pin/story models to return false, so ad chrome, ad beacons and click-out CTAs are never rendered or fired.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    execute {
        // matchAll() lets us patch every model class that has an is_promoted
        // field, not just the first one matched.
        val matches = IsPromotedGetterFingerprint.matchAll()

        if (matches.isEmpty()) {
            throw app.morphe.patcher.patch.PatchException(
                "No isPromoted getters matched — the Pinterest API model shape may have changed.",
            )
        }

        matches.forEach { match ->
            match.method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """,
            )
        }
    }
}
