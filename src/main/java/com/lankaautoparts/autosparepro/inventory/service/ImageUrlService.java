package com.lankaautoparts.autosparepro.inventory.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns whatever an admin pastes into the "Image URL" box into a link that
 * actually renders as an image in the browser.
 *
 * <ul>
 *   <li>Normalises the link (adds https://, unwraps Google Images redirect
 *       links, converts Google Drive / Dropbox / GitHub / Imgur share links
 *       to their direct-image form).</li>
 *   <li>Probes it server-side. If it is an image, it's accepted. If it is a
 *       web page (e.g. a product page), the page's {@code og:image} /
 *       {@code twitter:image} is used instead.</li>
 *   <li>Only rejects when the link definitely isn't, or doesn't contain, an
 *       image. Network hiccups or bot-blocking (403) are tolerated, since
 *       the admin's own browser may still be able to load the link.</li>
 * </ul>
 *
 * Because the server makes the request, targets are restricted to public
 * http(s) hosts — loopback, private and link-local addresses are refused.
 */
@Service
public class ImageUrlService {

    public static final int MAX_LENGTH = 2048;

    private static final int MAX_REDIRECTS = 5;
    private static final int MAX_HTML_BYTES = 768 * 1024;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

    private static final Pattern META_OR_LINK = Pattern.compile("<(meta|link)\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTR = Pattern.compile("([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')");
    private static final Pattern DRIVE_FILE = Pattern.compile("drive\\.google\\.com/file/d/([\\w-]+)");
    private static final Pattern DRIVE_ID = Pattern.compile("drive\\.google\\.com/(?:open|uc)\\?(?:[^#]*&)?id=([\\w-]+)");
    private static final Pattern IMGUR_PAGE = Pattern.compile("^https?://(?:www\\.|m\\.)?imgur\\.com/(?!a/|gallery/)([A-Za-z0-9]{5,8})/?$");

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /** Outcome of resolving a pasted link. Exactly one of {@code url} / {@code error} is set (both null = blank input). */
    public record Result(String url, String error) {
        public boolean ok() { return error == null; }
    }

    public Result resolve(String raw) {
        if (raw == null || raw.isBlank()) {
            return new Result(null, null);
        }

        String normalized;
        try {
            normalized = normalize(raw);
        } catch (IllegalArgumentException e) {
            return new Result(null, e.getMessage());
        }

        try {
            Probe probe = probe(normalized);
            if (probe.isImage) {
                return limit(normalized);
            }
            if (probe.html != null) {
                String found = findPageImage(probe.html, probe.finalUri);
                if (found != null) {
                    String candidate = normalize(found);
                    Probe second = probe(candidate);
                    if (second.isImage || second.unverified) {
                        return limit(candidate);
                    }
                }
                return new Result(null, "That link is a web page with no image on it. Paste a direct image link "
                        + "(right-click the picture → Copy image address).");
            }
            if (probe.unverified) {
                return limit(normalized); // blocked / flaky — let the browser try
            }
            return new Result(null, "That link doesn't point to an image. Paste a direct image link "
                    + "(right-click the picture → Copy image address).");
        } catch (IllegalArgumentException e) {
            return new Result(null, e.getMessage());
        } catch (java.net.UnknownHostException e) {
            return new Result(null, "Couldn't find that website — check the link for typos.");
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return limit(normalized);
        }
    }

    // ── Normalisation ────────────────────────────────────────────────────

    static String normalize(String raw) {
        String url = raw.trim();
        if (url.length() >= 2 && (url.startsWith("\"") && url.endsWith("\"") || url.startsWith("'") && url.endsWith("'"))) {
            url = url.substring(1, url.length() - 1).trim();
        }
        if (url.startsWith("//")) {
            url = "https:" + url;
        } else if (!url.matches("(?i)^[a-z][a-z0-9+.-]*:.*")) {
            url = "https://" + url;
        }
        if (!url.matches("(?i)^https?://.*")) {
            throw new IllegalArgumentException("Image link must start with http:// or https://");
        }

        // Google Images result link — the real image is in the imgurl parameter.
        Matcher imgres = Pattern.compile("[?&]imgurl=([^&]+)").matcher(url);
        if (url.matches("(?i)^https?://(?:www\\.)?google\\.[a-z.]+/imgres\\?.*") && imgres.find()) {
            url = URLDecoder.decode(imgres.group(1), StandardCharsets.UTF_8);
        }

        Matcher m = DRIVE_FILE.matcher(url);
        if (m.find() || (m = DRIVE_ID.matcher(url)).find()) {
            return "https://drive.google.com/thumbnail?id=" + m.group(1) + "&sz=w1000";
        }
        if (url.matches("(?i)^https?://(?:www\\.)?dropbox\\.com/.*")) {
            url = url.replaceFirst("(?i)//(?:www\\.)?dropbox\\.com", "//dl.dropboxusercontent.com")
                    .replaceAll("[?&]dl=0", "");
        }
        Matcher gh = Pattern.compile("^https?://github\\.com/([^/]+)/([^/]+)/blob/(.+)$", Pattern.CASE_INSENSITIVE).matcher(url);
        if (gh.find()) {
            url = "https://raw.githubusercontent.com/" + gh.group(1) + "/" + gh.group(2) + "/" + gh.group(3);
        }
        Matcher imgur = IMGUR_PAGE.matcher(url);
        if (imgur.find()) {
            url = "https://i.imgur.com/" + imgur.group(1) + ".jpg";
        }

        // Spaces and other unsafe characters would make the link unusable in an <img src>.
        try {
            URI u = new URI(url.replace(" ", "%20"));
            if (u.getHost() == null) {
                throw new IllegalArgumentException("That doesn't look like a valid link.");
            }
            return u.toString();
        } catch (java.net.URISyntaxException e) {
            throw new IllegalArgumentException("That doesn't look like a valid link.");
        }
    }

    // ── Probing ──────────────────────────────────────────────────────────

    private static final class Probe {
        boolean isImage;
        boolean unverified;   // couldn't tell (HTTP error / blocked) — not proof the link is bad
        String html;          // body, when the response was an HTML page
        URI finalUri;
    }

    private Probe probe(String url) throws IOException, InterruptedException {
        URI uri = URI.create(url);
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            assertPublicHost(uri);

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "image/avif,image/webp,image/png,image/jpeg,image/*,text/html;q=0.8,*/*;q=0.5")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            try (InputStream body = response.body()) {
                int status = response.statusCode();
                if (status >= 300 && status < 400) {
                    String location = response.headers().firstValue("Location").orElse(null);
                    if (location == null) break;
                    uri = uri.resolve(location);
                    if (uri.getScheme() == null || !uri.getScheme().toLowerCase(Locale.ROOT).startsWith("http")) {
                        break;
                    }
                    continue;
                }

                Probe probe = new Probe();
                probe.finalUri = uri;
                if (status >= 400) {
                    probe.unverified = true;
                    return probe;
                }

                String type = response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
                if (type.startsWith("image/")) {
                    probe.isImage = true;
                } else if (type.contains("html") || type.isEmpty()) {
                    byte[] bytes = body.readNBytes(MAX_HTML_BYTES);
                    probe.html = new String(bytes, StandardCharsets.UTF_8);
                }
                return probe;
            }
        }
        Probe unverified = new Probe();
        unverified.unverified = true;
        unverified.finalUri = uri;
        return unverified;
    }

    private static void assertPublicHost(URI uri) throws IOException {
        String host = uri.getHost();
        if (host == null) {
            throw new IllegalArgumentException("That doesn't look like a valid link.");
        }
        for (InetAddress address : InetAddress.getAllByName(host)) {
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                throw new IllegalArgumentException("Links to private or local addresses can't be used for part images.");
            }
        }
    }

    // ── Page image discovery ─────────────────────────────────────────────

    private static String findPageImage(String html, URI base) {
        String fallback = null;
        Matcher tags = META_OR_LINK.matcher(html);
        while (tags.find()) {
            String key = null;
            String value = null;
            Matcher attrs = ATTR.matcher(tags.group());
            while (attrs.find()) {
                String name = attrs.group(1).toLowerCase(Locale.ROOT);
                String val = attrs.group(2) != null ? attrs.group(2) : attrs.group(3);
                switch (name) {
                    case "property", "name", "rel" -> key = val.toLowerCase(Locale.ROOT);
                    case "content", "href" -> value = val;
                    default -> { }
                }
            }
            if (key == null || value == null || value.isBlank()) continue;
            value = value.replace("&amp;", "&").trim();

            if (key.equals("og:image") || key.equals("og:image:secure_url") || key.equals("og:image:url")) {
                return base.resolve(value).toString();
            }
            if ((key.equals("twitter:image") || key.equals("twitter:image:src") || key.equals("image_src")) && fallback == null) {
                fallback = base.resolve(value).toString();
            }
        }
        return fallback;
    }

    private static Result limit(String url) {
        if (url.length() > MAX_LENGTH) {
            return new Result(null, "That link is too long (max " + MAX_LENGTH + " characters).");
        }
        return new Result(url, null);
    }
}
