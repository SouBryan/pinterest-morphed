package app.soubryan.extension.pinterest;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rewrites URLs the app is about to hand to the Android share sheet so
 * that the recipient gets a link with no client-side tracking.
 *
 * Two problems are handled:
 *
 * 1. **Query-parameter tracking** (`utm_source`, `fbclid`, `gclid`, ...)
 *    Solved by parsing the URL, dropping every known tracking key from
 *    the query string, and rebuilding the URL. Fast, offline, always
 *    safe.
 *
 * 2. **Short URLs with per-share fingerprints** (`pin.it/<slug>`, similar
 *    to `vm.tiktok.com` / `t.co` / `bit.ly`). The slug itself encodes
 *    who created the link and who opened it, so query-stripping does
 *    nothing. We resolve the redirect once via `HEAD`, extract the path
 *    from the target URL, and rebuild it against the canonical
 *    `pinterest.com` host with no query at all. A hard 3s timeout keeps
 *    the share flow snappy — if resolution fails (offline, timeout, non
 *    3xx, ...) the original URL is returned untouched so the share still
 *    works.
 */
@SuppressWarnings("unused")
public final class UrlSanitizer {

    private UrlSanitizer() {}

    // region Tracking parameters

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
            "gbraid",   // Google Ads Enhanced Conversions
            "wbraid",   // Google Ads Enhanced Conversions
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

    // endregion

    // region Regex — where to look for links inside free-form share text

    /** Matches `https://pin.it/<slug>` short URLs (case-insensitive host). */
    private static final Pattern PIN_IT_PATTERN =
            Pattern.compile("https?://pin\\.it/[A-Za-z0-9_-]+", Pattern.CASE_INSENSITIVE);

    /**
     * Matches Pinterest's server-side URL shortener:
     * `https://<any>pinterest.<tld>/url_shortener/<slug>/redirect/`.
     * These carry a per-share fingerprint inside the slug just like
     * `pin.it/<slug>`.
     */
    private static final Pattern URL_SHORTENER_PATTERN =
            Pattern.compile(
                    "https?://[a-z0-9.-]*pinterest\\.[a-z.]{2,10}/url_shortener/[A-Za-z0-9_-]+/redirect/?",
                    Pattern.CASE_INSENSITIVE);

    /** Matches any pinterest.* URL (any TLD, any subdomain, any path). */
    private static final Pattern PINTEREST_URL_PATTERN =
            Pattern.compile("https?://[a-z0-9.-]*pinterest\\.[a-z.]{2,10}/[^\\s]*",
                    Pattern.CASE_INSENSITIVE);

    // endregion

    // region HTTP resolver

    /** Hard cap so the share flow can't stall the UI more than this. */
    private static final int RESOLVE_TIMEOUT_MS = 3_000;

    /** Cap on redirect hops to avoid infinite loops on hostile redirects. */
    private static final int MAX_REDIRECT_HOPS = 5;

    /**
     * Matches URLs of the form
     * `https?://<any>pinterest.<tld>/url_shortener/<slug>/redirect/`.
     * When the request goes to `www.pinterest.com/...` (or any subdomain
     * other than `api.`), the server responds `200 OK` and does the real
     * redirect via JavaScript, so the short URL cannot be resolved with
     * a plain HTTP HEAD request. When the request goes to
     * `api.pinterest.com/url_shortener/<slug>/redirect/` instead, the
     * server responds with a real `302 Found` whose `Location` header is
     * the fully-qualified pin URL. We rewrite the URL to that host before
     * dispatching the HEAD.
     */
    private static final Pattern URL_SHORTENER_REWRITE_TARGET = Pattern.compile(
            "^(https?://)([a-z0-9.-]*)(pinterest\\.[a-z.]{2,10}/url_shortener/[A-Za-z0-9_-]+/redirect/?)$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Matches a `/pin/<numericId>` prefix on the resolved URL path.
     * Pinterest tends to append suffixes like `/sent/` and query strings
     * like `?invite_code=...&sender=...&sfo=1` that are per-share
     * fingerprints; we drop everything after the pin id so the recipient
     * receives a clean canonical link.
     */
    private static final Pattern PIN_PATH_PATTERN =
            Pattern.compile("^(/pin/[0-9]+)/?", Pattern.CASE_INSENSITIVE);

    /**
     * Follows the `pin.it/<slug>` or `pinterest.<tld>/url_shortener/<slug>/redirect/`
     * chain via successive HEAD requests until either a non-redirect
     * response arrives or {@link #MAX_REDIRECT_HOPS} is reached.
     *
     * If the terminal URL points at `/pin/<id>` on a pinterest host, we
     * return a clean `https://pinterest.com/pin/<id>/` (no subdomain, no
     * `/sent/` suffix, no query string). Anything else falls back to the
     * original short URL so the share still works.
     */
    private static String resolveShortUrl(String shortUrl) {
        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread t = new Thread(runnable, "morphe-url-resolver");
            t.setDaemon(true);
            return t;
        });

        try {
            Future<String> future = executor.submit(new Callable<String>() {
                @Override
                public String call() {
                    // Rewrite the seed URL if it's a JS-side redirect so
                    // we hit the API endpoint that actually returns a
                    // real 3xx (see URL_SHORTENER_REWRITE_TARGET javadoc).
                    String current = shortUrl;
                    Matcher rewrite = URL_SHORTENER_REWRITE_TARGET.matcher(current);
                    if (rewrite.matches()) {
                        current = rewrite.group(1) + "api." + rewrite.group(3);
                    }

                    for (int hop = 0; hop < MAX_REDIRECT_HOPS; hop++) {
                        HttpURLConnection conn = null;
                        try {
                            conn = (HttpURLConnection) new URL(current).openConnection();
                            conn.setInstanceFollowRedirects(false);
                            conn.setConnectTimeout(RESOLVE_TIMEOUT_MS);
                            conn.setReadTimeout(RESOLVE_TIMEOUT_MS);
                            conn.setRequestMethod("HEAD");
                            // A neutral browser UA avoids getting a
                            // deep-link redirect that opens the app on
                            // the recipient's phone.
                            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                            conn.setRequestProperty("Accept", "*/*");

                            int status = conn.getResponseCode();
                            if (status >= 300 && status < 400) {
                                String location = conn.getHeaderField("Location");
                                if (location == null || location.length() == 0) {
                                    break;
                                }
                                current = location;
                                continue;
                            }
                            // Non-redirect terminal response — stop
                            // following and try to canonicalise whatever
                            // URL we ended up on.
                            break;
                        } catch (Throwable ignored) {
                            break;
                        } finally {
                            if (conn != null) {
                                try { conn.disconnect(); } catch (Throwable ignored) {}
                            }
                        }
                    }

                    // Try to canonicalise the terminal URL down to a bare
                    // pin link. Anything that isn't `/pin/<id>` on a
                    // pinterest host falls back to the original short URL.
                    try {
                        Uri terminal = Uri.parse(current);
                        String host = terminal.getHost();
                        if (host == null || !host.toLowerCase().contains("pinterest")) {
                            return shortUrl;
                        }
                        String path = terminal.getEncodedPath();
                        if (path == null) {
                            return shortUrl;
                        }
                        Matcher pinMatch = PIN_PATH_PATTERN.matcher(path);
                        if (!pinMatch.find()) {
                            return shortUrl;
                        }
                        return "https://pinterest.com" + pinMatch.group(1) + "/";
                    } catch (Throwable ignored) {
                        return shortUrl;
                    }
                }
            });

            // The Future.get timeout is a per-URL upper bound; the HEAD
            // requests inside have their own per-hop timeout so a bad
            // redirect chain can never stall the caller forever.
            return future.get((MAX_REDIRECT_HOPS * RESOLVE_TIMEOUT_MS) + 500L, TimeUnit.MILLISECONDS);
        } catch (Throwable ignored) {
            return shortUrl;
        } finally {
            executor.shutdownNow();
        }
    }

    // endregion

    // region Sanitisers

    /**
     * Returns {@code url} with every tracking parameter stripped from its
     * query string. Non-tracking params are preserved.
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
     * Full sanitisation pipeline applied to a free-form share text
     * (which may or may not be a bare URL).
     *
     *   1. Every {@code pin.it/<slug>} short URL found in the text is
     *      resolved to its canonical {@code pinterest.com/<path>} form.
     *      The HTTP call always runs on a worker thread, so calling this
     *      from the UI thread is safe (no NetworkOnMainThreadException).
     *      The caller does block on {@code Future.get} for up to
     *      {@link #RESOLVE_TIMEOUT_MS} + 500ms per short URL — long
     *      enough to complete on a healthy connection, short enough to
     *      avoid an ANR if the network is dead.
     *   2. Every {@code pinterest.<tld>} URL found in the text (including
     *      the ones we just rewrote in step 1) has its tracking query
     *      parameters stripped.
     */
    public static String sanitizeText(String text) {
        if (text == null || text.length() == 0) {
            return text;
        }

        String result = text;

        // Deduplicate short URLs so we only pay one HTTP round-trip per
        // unique slug even if the text contains it multiple times.
        // Both pin.it/<slug> and pinterest.<tld>/url_shortener/<slug>/redirect/
        // carry per-share fingerprints and need the same HEAD-resolve
        // treatment.
        Set<String> shortUrls = new HashSet<>();
        Matcher pinItMatcher = PIN_IT_PATTERN.matcher(text);
        while (pinItMatcher.find()) {
            shortUrls.add(pinItMatcher.group());
        }
        Matcher shortenerMatcher = URL_SHORTENER_PATTERN.matcher(text);
        while (shortenerMatcher.find()) {
            shortUrls.add(shortenerMatcher.group());
        }
        for (String shortUrl : shortUrls) {
            String resolved = resolveShortUrl(shortUrl);
            if (!resolved.equals(shortUrl)) {
                result = result.replace(shortUrl, resolved);
            }
        }

        // Strip UTMs from every pinterest.* URL now present in the text
        // (works whether we resolved a pin.it or not).
        Set<String> pinterestUrls = new HashSet<>();
        Matcher pinterestMatcher = PINTEREST_URL_PATTERN.matcher(result);
        while (pinterestMatcher.find()) {
            pinterestUrls.add(pinterestMatcher.group());
        }
        for (String pinterestUrl : pinterestUrls) {
            String cleaned = stripTrackingParams(pinterestUrl);
            if (!cleaned.equals(pinterestUrl)) {
                result = result.replace(pinterestUrl, cleaned);
            }
        }

        return result;
    }

    /**
     * Rewrites {@link Intent#EXTRA_TEXT} on {@code intent} through
     * {@link #sanitizeText(String)}. The intent is returned unchanged if
     * it has no EXTRA_TEXT or if the text carries no tracking.
     *
     * Injected by {@code SanitizeSharingLinksPatch} at the tail of the
     * Pinterest share-Intent factory.
     */
    public static Intent sanitizeShareIntent(Intent intent) {
        if (intent == null) {
            return intent;
        }
        try {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (text != null) {
                String cleaned = sanitizeText(text);
                if (!cleaned.equals(text)) {
                    intent.putExtra(Intent.EXTRA_TEXT, cleaned);
                }
            }
        } catch (Throwable ignored) {
            // Never let a sanitiser mishap crash the share flow.
        }
        return intent;
    }

    /**
     * Returns a copy of {@code data} whose first text item has been run
     * through {@link #sanitizeText(String)}. Injected by
     * {@code SanitizeCopyLinkPatch} right before every call site of
     * {@link android.content.ClipboardManager#setPrimaryClip(ClipData)}.
     *
     * The clip is returned untouched (same reference) when it has no
     * items, when the first item has no text, or when the text carries
     * no tracking. This keeps the hook cheap for the common non-URL
     * clipboard uses (search box, comment field, ...).
     */
    public static ClipData sanitizeClipData(ClipData data) {
        if (data == null || data.getItemCount() == 0) {
            return data;
        }
        try {
            ClipData.Item item = data.getItemAt(0);
            CharSequence text = item == null ? null : item.getText();
            if (text == null || text.length() == 0) {
                return data;
            }
            String cleaned = sanitizeText(text.toString());
            if (cleaned.equals(text.toString())) {
                return data;
            }

            String label = data.getDescription() != null && data.getDescription().getLabel() != null
                    ? data.getDescription().getLabel().toString()
                    : "Text";
            return ClipData.newPlainText(label, cleaned);
        } catch (Throwable ignored) {
            return data;
        }
    }

    // endregion
}
