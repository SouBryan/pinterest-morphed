package app.soubryan.extension.pinterest;

import android.content.Intent;
import android.net.Uri;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Strips tracking / attribution query parameters from URLs the app puts on
 * the Android share sheet. Called from {@code SanitizeSharingLinksPatch}
 * at the tail of the Pinterest share-Intent factory.
 */
@SuppressWarnings("unused")
public final class UrlSanitizer {

    private UrlSanitizer() {}

    /**
     * Query parameters that carry no functional meaning for the pin URL
     * itself and only exist so third parties can attribute the click back
     * to Pinterest (or to a specific in-app share flow).
     */
    private static final Set<String> TRACKING_PARAMS = new HashSet<>(Arrays.asList(
            // Google / generic UTM taxonomy
            "utm_source",
            "utm_medium",
            "utm_campaign",
            "utm_content",
            "utm_term",
            "utm_id",
            "utm_creative_format",
            "utm_marketing_tactic",
            // Ad-network click IDs
            "fbclid",   // Facebook
            "gclid",    // Google Ads
            "gbraid",   // Google Ads (Enhanced Conversions)
            "wbraid",   // Google Ads (Enhanced Conversions)
            "yclid",    // Yandex
            "ttclid",   // TikTok
            "twclid",   // Twitter/X
            "li_fat_id",// LinkedIn
            "mc_cid",   // Mailchimp campaign
            "mc_eid",   // Mailchimp email
            // Pinterest / install-referrer specific
            "amp_client_id",
            "app_upsell_type",
            "sfo",
            "sender"
    ));

    /**
     * Returns {@code url} with every tracking parameter stripped from its
     * query string. Non-tracking params are preserved. If parsing fails or
     * there is nothing to strip, the original string is returned unchanged.
     */
    public static String stripTrackingParams(String url) {
        if (url == null || url.length() == 0 || url.indexOf('?') < 0) {
            return url;
        }
        try {
            Uri uri = Uri.parse(url);
            String query = uri.getQuery();
            if (query == null || query.length() == 0) {
                return url;
            }

            Uri.Builder builder = uri.buildUpon().clearQuery();
            boolean modified = false;

            for (String key : uri.getQueryParameterNames()) {
                if (TRACKING_PARAMS.contains(key.toLowerCase())) {
                    modified = true;
                    continue;
                }
                List<String> values = uri.getQueryParameters(key);
                if (values.isEmpty()) {
                    // Preserve bare "?key" params
                    builder.appendQueryParameter(key, "");
                } else {
                    for (String value : values) {
                        builder.appendQueryParameter(key, value);
                    }
                }
            }

            return modified ? builder.build().toString() : url;
        } catch (Throwable ignored) {
            return url;
        }
    }

    /**
     * Rewrites {@link Intent#EXTRA_TEXT} on {@code intent} through
     * {@link #stripTrackingParams(String)}. The intent is returned unchanged
     * if it has no EXTRA_TEXT or if the text carries no tracking params.
     *
     * Injected by {@code SanitizeSharingLinksPatch} at the tail of the
     * Pinterest share-Intent factory so the sanitised value replaces whatever
     * the server-side handed us.
     */
    public static Intent sanitizeShareIntent(Intent intent) {
        if (intent == null) {
            return intent;
        }
        try {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (text != null) {
                String cleaned = stripTrackingParams(text);
                if (!cleaned.equals(text)) {
                    intent.putExtra(Intent.EXTRA_TEXT, cleaned);
                }
            }
        } catch (Throwable ignored) {
            // Never let a sanitiser mishap crash the share flow.
        }
        return intent;
    }
}
