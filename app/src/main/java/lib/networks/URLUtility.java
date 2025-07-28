package lib.networks;

import static java.net.URLEncoder.encode;

import android.os.Build;
import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Utility class for URL-related operations including validation, parsing,
 * manipulation, and network requests.
 */
public class URLUtility {

    // HTTP header constants
    public static final String CONTENT_DISPOSITION = "Content-Disposition";
    public static final String ACCEPT_RANGES = "Accept-Ranges";
    public static final String BYTES = "bytes";
    public static final String LAST_MODIFIED = "Last-Modified";
    public static final String E_TAG = "ETag";
    public static final String HEAD = "HEAD";
    public static final String CONTENT_LENGTH = "Content-Length";

    /**
     * Validates whether a string is a properly formatted URL.
     * @param url The URL string to validate
     * @return true if the URL is valid, false otherwise
     */
    public static boolean isValidURL(@Nullable String url) {
        if (url == null || url.isEmpty()) return false;
        try {
            new URL(url);
            return true;
        } catch (Throwable error) {
            return false;
        }
    }

    /**
     * Extracts the filename from a URL path.
     * @param urlString The URL to parse
     * @return The filename portion of the URL, or null if parsing fails
     */
    @Nullable
    public static String getFileNameFromURL(@NonNull String urlString) {
        try {
            URL url = new URL(urlString);
            String filePath = url.getPath();
            int lastSlashIndex = filePath.lastIndexOf('/');
            if (lastSlashIndex == -1) return filePath;
            else return filePath.substring(lastSlashIndex + 1);
        } catch (Exception error) {
            error.printStackTrace();
            return null;
        }
    }

    /**
     * Validates whether a string is a properly formatted domain name.
     * @param domain The domain string to validate
     * @return true if the domain is valid, false otherwise
     */
    public static boolean isValidDomain(String domain) {
        String domainRegex = "^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        return domain.matches(domainRegex);
    }

    /**
     * Ensures a URL uses HTTPS protocol.
     * @param url The URL to process
     * @return HTTPS version of the URL, or null if input is not a valid domain
     */
    @Nullable
    public static String ensureHttps(@NonNull String url) {
        if (!isValidDomain(url)) return null;
        String nakedDomain = url.replaceFirst("^(https?://)?(www\\.)?", "");
        if (!nakedDomain.startsWith("https://")) {
            nakedDomain = "https://" + nakedDomain;
        }
        return nakedDomain;
    }

    /**
     * Checks if a URL is accessible by making a HEAD request.
     * @param urlString The URL to check
     * @return true if the URL responds with HTTP OK (200), false otherwise
     */
    public static boolean isUrlAccessible(@NonNull String urlString) {
        try {
            HttpURLConnection connection = (HttpURLConnection)
                    new URL(urlString).openConnection();
            connection.setRequestMethod(HEAD);
            int responseCode = connection.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (Throwable error) {
            error.printStackTrace();
            return false;
        }
    }

    /**
     * Extracts all URLs from a text string using Android's WEB_URL pattern.
     * @param text The text to scan for URLs
     * @return Array of found URLs
     */
    @NonNull
    public static String[] extractLinks(@NonNull String text) {
        List<String> links = new ArrayList<>();
        Pattern pattern = Patterns.WEB_URL;
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String url = matcher.group();
            links.add(url);
        }
        return links.toArray(new String[0]);
    }

    /**
     * Gets the file size from a URL using standard HttpURLConnection.
     * @param url The URL to check
     * @return File size in bytes, or -1 if unavailable
     */
    public static long getFileSizeFromUrl(@NonNull URL url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(HEAD);
            connection.connect();
            return connection.getContentLength();
        } catch (IOException error) {
            error.printStackTrace();
            return -1;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Gets the file size from a URL using OkHttp client.
     * @param url The URL to check
     * @return File size in bytes, or -1 if unavailable
     */
    public static long getFileSizeFromURL_OkHttp(@NonNull URL url) {
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .followRedirects(true).followSslRedirects(true).build();
            Request request = new Request.Builder().url(url).head().build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String contentLength = response.header(CONTENT_LENGTH);
                    if (contentLength != null) {
                        return Long.parseLong(contentLength);
                    } else {
                        throw new IOException("Content-Length header is missing");
                    }
                } else {
                    throw new IOException("Failed to fetch file size: "
                            + response.message());
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
            return -1;
        }
    }

    /**
     * Checks if a URL supports multipart downloads by examining Accept-Ranges header.
     * @param fileUrl The URL to check
     * @return true if server accepts range requests, false otherwise
     * @throws IOException if connection fails
     */
    public static boolean supportsMultipartDownload(
            @NonNull String fileUrl) throws IOException {
        HttpURLConnection connection =
                (HttpURLConnection) new URL(fileUrl).openConnection();
        connection.setRequestMethod(HEAD);
        connection.connect();

        boolean supportsMultipart = false;
        String acceptRanges = connection.getHeaderField(ACCEPT_RANGES);
        if (acceptRanges != null && acceptRanges.equals(BYTES)) {
            supportsMultipart = true;
        }

        connection.disconnect();
        return supportsMultipart;
    }

    /**
     * Checks if a URL supports resumable downloads by examining headers.
     * @param fileUrl The URL to check
     * @return true if server supports resume, false otherwise
     * @throws IOException if connection fails
     */
    public static boolean supportsResumableDownload(
            @NonNull String fileUrl) throws IOException {
        HttpURLConnection connection =
                (HttpURLConnection) new URL(fileUrl).openConnection();
        connection.setRequestMethod(HEAD);
        connection.connect();

        boolean supportsResume = false;
        String acceptRanges = connection.getHeaderField(ACCEPT_RANGES);
        String eTag = connection.getHeaderField(E_TAG);
        String lastModified = connection.getHeaderField(LAST_MODIFIED);
        if ((acceptRanges != null && acceptRanges.equals(BYTES)) ||
                eTag != null || lastModified != null) {
            supportsResume = true;
        }

        connection.disconnect();
        return supportsResume;
    }

    /**
     * Normalizes a URL by ensuring it ends with a forward slash.
     * @param url The URL to normalize
     * @return Normalized URL
     */
    @NonNull
    public static String normalizeUrl(@NonNull String url) {
        if (!url.endsWith("/") && url.contains("."))
            return url.replaceAll("/$", "") + "/";
        return url;
    }

    /**
     * Extracts the domain name from a URL.
     * @param url The URL to parse
     * @return Domain name, or empty string if parsing fails
     */
    @NonNull
    public static String extractDomainName(@NonNull String url) {
        try {
            URL parsedUrl = new URL(url);
            return parsedUrl.getHost();
        } catch (Throwable error) {
            error.printStackTrace();
            return "";
        }
    }

    /**
     * Appends a path segment to a base URL.
     * @param baseUrl The base URL
     * @param path The path to append
     * @return Combined URL
     */
    @NonNull
    public static String appendPath(@NonNull String baseUrl,
                                    @NonNull String path) {
        if (!baseUrl.endsWith("/") && !path.startsWith("/")) baseUrl += "/";
        return baseUrl + path;
    }

    /**
     * Removes query parameters from a URL.
     * @param url The URL to process
     * @return URL without query parameters
     */
    @NonNull
    public static String removeQueryParams(@NonNull String url) {
        try {
            URL parsedUrl = new URL(url);
            return parsedUrl.getProtocol() + "://" +
                    parsedUrl.getHost() + parsedUrl.getPath();
        } catch (Throwable error) {
            error.printStackTrace();
            return "";
        }
    }

    /**
     * Adds a query parameter to a URL.
     * @param url The base URL
     * @param param The parameter name
     * @param value The parameter value
     * @param encode Whether to URL-encode the value
     * @return URL with added query parameter
     */
    @NonNull
    public static String addQueryParam(@NonNull String url, @NonNull String param,
                                       @NonNull String value, boolean encode) {
        try {
            URL baseUrl = new URL(url);
            StringBuilder newUrl = new StringBuilder(baseUrl.toString());

            if (baseUrl.getQuery() == null) newUrl.append('?');
            else newUrl.append('&');

            newUrl.append(param);
            newUrl.append('=');

            if (encode) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    newUrl.append(encode(value, StandardCharsets.UTF_8));
                } else {
                    //noinspection CharsetObjectCanBeUsed
                    newUrl.append(encode(value, "UTF-8"));
                }
            } else newUrl.append(value);
            return newUrl.toString();
        } catch (Throwable error) {
            error.printStackTrace();
            return url;
        }
    }

    /**
     * Generates possible URL variations by appending different TLDs.
     * @param baseUrl The base URL without TLD
     * @return List of possible URLs with different TLDs
     */
    @NonNull
    public static List<String> generatePossibleURLs(@NonNull String baseUrl) {
        List<String> possibleURLs = new ArrayList<>();
        for (String domainEnd : URLDomains.getTopLevelDomains()) {
            possibleURLs.add(baseUrl + domainEnd);
        }
        return possibleURLs;
    }

    /**
     * Follows URL redirects to get the original URL.
     * @param fileURL The URL that might redirect
     * @return Final URL after following redirects, or null if no redirect
     */
    @Nullable
    public static String getOriginalURL(@NonNull String fileURL) {
        try {
            URLConnection urlConnection = new URL(fileURL).openConnection();
            HttpURLConnection connection = (HttpURLConnection) urlConnection;
            connection.setInstanceFollowRedirects(false);
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                    responseCode == HttpURLConnection.HTTP_CREATED) {
                return connection.getHeaderField("Location");
            }
            return null;
        } catch (Exception error) {
            error.printStackTrace();
            return null;
        }
    }

    /**
     * Fetches the Content-Disposition header from a URL.
     * @param url The URL to check
     * @return Content-Disposition header value, or null if not found
     */
    @Nullable
    public static String fetchContentDispositionHeader(@NonNull String url) {
        HttpURLConnection connection = null;
        try {
            URL urlObj = new URL(url);
            connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String contentDisposition =
                        connection.getHeaderField(CONTENT_DISPOSITION);
                if (contentDisposition != null) return contentDisposition;
            }
        } catch (IOException error) {
            error.printStackTrace();
        } finally {
            if (connection != null) connection.disconnect();
        }
        return null;
    }

    /**
     * URL-encodes a string using UTF-8 encoding.
     * Requires Android TIRAMISU (API 33) or higher.
     * @param url The string to encode
     * @return Encoded string, or empty string if encoding fails
     */
    @NonNull
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public static String encodeURL(@NonNull String url) {
        try {
            return encode(url, StandardCharsets.UTF_8);
        } catch (Exception error) {
            error.printStackTrace();
            return "";
        }
    }

    /**
     * URL-decodes a string using UTF-8 encoding.
     * Requires Android TIRAMISU (API 33) or higher.
     * @param url The string to decode
     * @return Decoded string, or empty string if decoding fails
     */
    @NonNull
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public static String decodeURL(@NonNull String url) {
        try {
            return URLDecoder.decode(url, StandardCharsets.UTF_8);
        } catch (Exception error) {
            error.printStackTrace();
            return "";
        }
    }
}