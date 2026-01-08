package app.morphe.patches.pinterest

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.encodedValue.MutableIntEncodedValue
import com.android.tools.smali.dexlib2.ReferenceType
import com.android.tools.smali.dexlib2.immutable.value.ImmutableIntEncodedValue
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.util.MethodUtil

@Suppress("unused")
val pinterestNoAdsPatch = bytecodePatch(
    name = "Pinterest Pro: No Ads/Trackers",
    description = "Blocks Pinterest ads, analytics, and crash reporters while redirecting tracker hosts to localhost.",
) {
    compatibleWith(
        "com.pinterest"("14.0.0")
    )

    extendWith("extensions/extension.rve")

    execute {
        disableFirebaseAnalytics()
        disableBugsnag()
        disableAppsFlyer()
        redirectTrackingHosts()
        remapAdViewTypesToOrganic()
    }
}

private fun BytecodePatchContext.disableFirebaseAnalytics() {
    mutableClassDefByOrNull("Lcom/google/firebase/analytics/FirebaseAnalytics;")?.apply {
        stubVoidMethod("logEvent", listOf("Ljava/lang/String;", "Landroid/os/Bundle;"))
        stubVoidMethod("setUserId", listOf("Ljava/lang/String;"))
        stubVoidMethod("setDefaultEventParameters", listOf("Landroid/os/Bundle;"))
        stubVoidMethod("setAnalyticsCollectionEnabled", listOf("Z"))
        stubObjectReturner("getInstance", listOf("Landroid/content/Context;"))
    }
}

private fun BytecodePatchContext.disableBugsnag() {
    // Main client API
    mutableClassDefByOrNull("Lcom/bugsnag/android/Client;")?.apply {
        stubVoidMethod("notify", listOf("Ljava/lang/Throwable;"))
        stubVoidMethod("startSession", emptyList())
        stubVoidMethod("leaveBreadcrumb", listOf("Ljava/lang/String;"))
        stubVoidMethod("addMetadata", listOf("Ljava/lang/String;", "Ljava/util/Map;"))
        stubVoidMethod("pauseSession", emptyList())
        stubVoidMethod("resumeSession", emptyList())
    }

    // Static helpers
    mutableClassDefByOrNull("Lcom/bugsnag/android/Bugsnag;")?.apply {
        stubVoidMethod("start", listOf("Landroid/content/Context;"))
        stubVoidMethod("start", listOf("Landroid/content/Context;", "Ljava/lang/String;"))
        stubObjectReturner("getClient", emptyList())
        stubVoidMethod("leaveBreadcrumb", listOf("Ljava/lang/String;"))
        stubVoidMethod("addMetadata", listOf("Ljava/lang/String;", "Ljava/util/Map;"))
    }
}

private fun BytecodePatchContext.disableAppsFlyer() {
    mutableClassDefByOrNull("Lcom/appsflyer/AppsFlyerLib;")?.apply {
        stubVoidMethod("start", listOf("Landroid/content/Context;"))
        stubVoidMethod("start", listOf("Landroid/content/Context;", "Ljava/lang/String;"))
        stubVoidMethod("logEvent", listOf("Landroid/content/Context;", "Ljava/lang/String;", "Ljava/util/Map;"))
        stubVoidMethod("sendPushNotificationData", listOf("Landroid/content/Context;"))
        stubVoidMethod("init", listOf("Ljava/lang/String;", "Lcom/appsflyer/AppsFlyerConversionListener;", "Landroid/content/Context;"))
        stubObjectReturner("getInstance", emptyList())
    }
}

private fun BytecodePatchContext.redirectTrackingHosts() {
    // Pin primary Pinterest trackers + common SDK endpoints to localhost.
    replaceStringLiteral("analytics.pinterest.com", "127.0.0.1")
    replaceStringLiteral("ads.pinterest.com", "127.0.0.1")
    replaceStringLiteral("app-measurement.com", "127.0.0.1")
    replaceStringLiteral("events.appsflyer.com", "127.0.0.1")
    replaceStringLiteral("monitors.appsflyer.com", "127.0.0.1")
    replaceStringLiteral("e.crashlytics.com", "127.0.0.1")
}

private fun BytecodePatchContext.remapAdViewTypesToOrganic() {
    // Map ad view types to organic pin to avoid pagination stalls when ad items appear in feed.
    val organic = 290 // RecyclerViewTypes.VIEW_TYPE_ORGANIC_PIN
    mutableClassDefByOrNull("Lcom/pinterest/feature/core/view/RecyclerViewTypes;")?.apply {
        val adFields = setOf(
            "VIEW_TYPE_GMA_NATIVE_AD_IMAGE",
            "VIEW_TYPE_GMA_NATIVE_AD_VIDEO",
            "VIEW_TYPE_GMA_NATIVE_AD_APP_INSTALL_IMAGE",
            "VIEW_TYPE_GMA_NATIVE_AD_APP_INSTALL_VIDEO",
            "VIEW_TYPE_GMA_BANNER_AD",
            "VIEW_TYPE_GMA_NATIVE_AD_SBA",
            "VIEW_TYPE_GMA_BANNER_AD_SBA",
            "VIEW_TYPE_GMA_NATIVE_AD_SBA_FULL_SPAN",
            "VIEW_TYPE_GMA_BANNER_AD_SBA_FULL_SPAN",
            "VIEW_TYPE_ADS_CAROUSEL",
            "VIEW_TYPE_ADS_CAROUSEL_PIN_ITEM",
            "VIEW_TYPE_PROMOTED_PIN_VIDEO",
            "VIEW_TYPE_PROMOTED_PIN_VIDEO_AD_EXCLUDE",
            "VIEW_TYPE_PIN_AD_EXCLUDE",
            "VIEW_TYPE_PIN_FULL_SPAN_AD_EXCLUDE",
            "VIEW_TYPE_PIN_VIDEO_AD_EXCLUDE",
            "VIEW_TYPE_CAROUSEL_SINGLE_COLUMN_AD_EXCLUDE",
            "VIEW_TYPE_SHUFFLE_CAROUSEL",
            "VIEW_TYPE_PIN_WEBVIEW_AD",
            "VIEW_TYPE_SPOTLIGHT_COLLECTIONS_AD_EXCLUDE",
            "VIEW_TYPE_HOME_FEED_TUNER_SETTINGS_NOTIFICATION"
        )

        fields.filter { it.name in adFields }.forEach { field ->
            field.initialValue = MutableIntEncodedValue(ImmutableIntEncodedValue(organic))
        }
    }
}

private fun MutableClass.stubVoidMethod(name: String, parameterTypes: List<String>) {
    methods.firstOrNull { it.name == name && it.parameterTypes == parameterTypes }
        ?.addInstruction(0, "return-void")
}

private fun MutableClass.stubObjectReturner(name: String, parameterTypes: List<String>) {
    methods.firstOrNull { it.name == name && it.parameterTypes == parameterTypes }
        ?.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent()
        )
}

// Lightweight, inline string literal replacer scoped to this patch.
private fun BytecodePatchContext.replaceStringLiteral(from: String, to: String) {
    classDefForEach { classDef ->
        val mutableClass by lazy { mutableClassDefBy(classDef) }

        classDef.methods.forEach methodLoop@{ method ->
            val implementation = method.implementation ?: return@methodLoop

            implementation.instructions.forEachIndexed { index, instruction ->
                val refInstruction = instruction as? ReferenceInstruction ?: return@forEachIndexed
                if (instruction.opcode.referenceType != ReferenceType.STRING) return@forEachIndexed

                val reference = refInstruction.reference as? StringReference ?: return@forEachIndexed
                val original = reference.string
                if (!original.contains(from)) return@forEachIndexed

                val mutableMethod = mutableClass.findMutableMethod(method)
                val register = (instruction as OneRegisterInstruction).registerA
                val newString = original.replace(from, to)

                // Use the same opcode but swap the literal.
                mutableMethod.replaceInstruction(
                    index,
                    "${instruction.opcode.name.lowercase()} v$register, \"$newString\"",
                )
            }
        }
    }
}

private fun MutableClass.findMutableMethod(method: Method): MutableMethod =
    methods.first { MethodUtil.methodSignaturesMatch(it, method) }
