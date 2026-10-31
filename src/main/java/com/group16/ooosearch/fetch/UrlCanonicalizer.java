package com.group16.ooosearch.fetch;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 把同一個網頁的不同寫法統一，讓去除重複時能認得它們是同一頁：
 * 網域轉小寫、拿掉 #片段與結尾斜線、統一百分比編碼，
 * 語言變體路徑（/zh-tw/、/zh-hant/…）統一：維基百科改成 /wiki/，萌娘百科改成 /。
 */
public final class UrlCanonicalizer {
    private static final Pattern LANGUAGE_VARIANT = Pattern.compile("^/zh(-[a-z]+)?/");

    private UrlCanonicalizer() {
    }

    /** 回傳正規化後的網址；無法解析時回傳去掉前後空白的原字串。 */
    public static String canonicalize(String url) {
        String trimmed = url.strip();
        try {
            URI uri = new URI(trimmed);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return trimmed;
            }
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            String path = uri.getPath() == null ? "" : uri.getPath();
            if (host.endsWith("wikipedia.org")) {
                path = LANGUAGE_VARIANT.matcher(path).replaceFirst("/wiki/");
            } else if (host.endsWith("moegirl.org.cn")) {
                path = LANGUAGE_VARIANT.matcher(path).replaceFirst("/");
            }
            if (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            String authority = uri.getPort() < 0 ? host : host + ":" + uri.getPort();
            return new URI(uri.getScheme().toLowerCase(Locale.ROOT), authority, path, uri.getQuery(), null)
                    .toASCIIString();
        } catch (URISyntaxException e) {
            return trimmed;
        }
    }
}
