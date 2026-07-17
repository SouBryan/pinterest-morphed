package app.soubryan.patches.pinterest.auth

import app.morphe.patcher.patch.resourcePatch
import app.soubryan.patches.pinterest.shared.Constants.COMPATIBILITY_PINTEREST
import org.w3c.dom.Element

/**
 * Restores "Continue with Google" login when the device uses **microG-RE**
 * (MorpheApp fork) in place of Google Play Services.
 *
 * ## The problem
 *
 * Pinterest authenticates the Google login flow with the OAuth 2.0 Android
 * client registered in Google's developer console. That client is keyed by
 * `(packageName, signingCertificate SHA-1)`. When the app is patched, the
 * APK is resigned with a developer key that does **not** match the SHA-1
 * Pinterest registered with Google, so the OAuth server rejects the token
 * exchange with `invalid_client` and the app silently returns to the login
 * screen after the account picker.
 *
 * ## The fix
 *
 * microG-RE (`app.revanced.android.gms` — the fork of ReVanced GmsCore
 * used by MorpheApp) implements *per-caller signature spoofing*: when it
 * signs a Google API request on behalf of another app, it looks for the
 * meta-data key below on the *caller* and, if present, tells Google that
 * the caller has that signature instead of its real one.
 *
 * ```
 * <meta-data android:name="app.revanced.android.gms.SPOOFED_PACKAGE_SIGNATURE"
 *            android:value="b6a74dbcb894b0f73d8c485c72eb1247a8f027ca"/>
 * ```
 *
 * The hex value is the SHA-1 of the certificate the Play Store build is
 * signed with (Signer #1: CN=Carl Rice, O=Pinterest Inc, Palo Alto, CA).
 * It was verified against `apksigner verify --print-certs` on the 14.27.0
 * base.apk and matches the certificate whose SHA-256 the patcher already
 * enforces as a supply-chain safeguard in `Constants.COMPATIBILITY_PINTEREST`.
 *
 * With the meta-data present, microG-RE forwards the correct signature to
 * Google's OAuth server, the token exchange succeeds, and Pinterest gets a
 * valid `GoogleSignInAccount` back.
 *
 * ## Why this is safe on stock Google Play Services
 *
 * Vanilla Google Play Services does not know about the
 * `app.revanced.android.gms.SPOOFED_*` meta-data namespace, so it ignores
 * the tag entirely. On a Play-certified device the "Continue with Google"
 * button is already broken by the signature mismatch — nothing in the
 * patched APK can fix that path, and adding this meta-data does not make
 * it any worse. The patch is therefore safe to enable by default.
 *
 * ## Why this does *not* help microG upstream (`microg/GmsCore`)
 *
 * Upstream microG has no `PackageSpoofUtils` — it always reports the
 * caller's real signature and relies on ROM-level `FAKE_PACKAGE_SIGNATURE`
 * to make itself look like Google. For that variant the user still needs
 * signature spoofing enabled in their ROM/kernel (LineageOS-for-microG,
 * /e/OS, GrapheneOS Sandboxed Google Play, Magisk FakeGApps module, or an
 * equivalent Xposed/LSPatch injection). This patch is a no-op there.
 *
 * ## Interaction with `RenameToMorphePatch` (future)
 *
 * When (if) the sibling opt-in `RenameToMorphePatch` renames the app to
 * `com.pinterest.morphe.android` so it shows up on the microG-RE self-check
 * screen as "Executando com patches do Morphe", we will also need to
 * declare `SPOOFED_PACKAGE_NAME=com.pinterest` so Google still recognises
 * the OAuth caller as `com.pinterest`. That patch depends on this one.
 */
@Suppress("unused")
val spoofGoogleAuthPatch = resourcePatch(
    name = "Restore Google login (microG-RE)",
    description = "Adds the microG-RE signature-spoof meta-data so \"Continue with Google\" works on devices that use microG-RE instead of Google Play Services. Ignored by vanilla Play Services.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    execute {
        // SHA-1 of Signer #1 in the official Play Store build.
        // Verified with `apksigner verify --print-certs base.apk` on 14.27.0.
        // The corresponding SHA-256 (already enforced as an install-time
        // allowlist in Constants.COMPATIBILITY_PINTEREST) is
        // 341d6881b1ecf38361fbf8c8fbae0aa516b45375c39ef5e78b161869acc1bcfa.
        val pinterestOfficialSignatureSha1 =
            "b6a74dbcb894b0f73d8c485c72eb1247a8f027ca"

        // Fully-qualified meta-data key expected by microG-RE. It is
        // derived at build time in microG-RE from `BASE_PACKAGE_NAME`
        // (`app.revanced`) — see `PackageSpoofUtils.kt` in
        // `MorpheApp/MicroG-RE`, `play-services-base/core/src/main/kotlin`.
        val spoofSignatureMetaName =
            "app.revanced.android.gms.SPOOFED_PACKAGE_SIGNATURE"

        document("AndroidManifest.xml").use { document ->
            val application = document
                .getElementsByTagName("application")
                .item(0) as Element

            // Idempotency: if a previous patch application already added
            // the meta-data (e.g. a re-patch after a Manager upgrade),
            // update its value in place instead of duplicating the tag.
            val existingMetas = application.getElementsByTagName("meta-data")
            for (i in 0 until existingMetas.length) {
                val meta = existingMetas.item(i) as Element
                if (meta.getAttribute("android:name") == spoofSignatureMetaName) {
                    meta.setAttribute("android:value", pinterestOfficialSignatureSha1)
                    return@use
                }
            }

            val meta = document.createElement("meta-data")
            meta.setAttribute("android:name", spoofSignatureMetaName)
            meta.setAttribute("android:value", pinterestOfficialSignatureSha1)
            application.appendChild(meta)
        }
    }
}
