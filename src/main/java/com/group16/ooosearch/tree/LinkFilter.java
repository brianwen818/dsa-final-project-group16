package com.group16.ooosearch.tree;

import java.net.URI;

public class LinkFilter {

    public static boolean isValidLink(String rootUrl, String targetUrl) {
        if (targetUrl == null || targetUrl.isEmpty()) {
            return false;
        }

        String lowerUrl = targetUrl.toLowerCase();
        // 排除非內文頁面、登入、錨點或檔案
        if (lowerUrl.contains("login") || lowerUrl.contains("signup") || 
            lowerUrl.contains("logout") || lowerUrl.contains("javascript:") ||
            lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".png") || lowerUrl.endsWith(".pdf") ||
            lowerUrl.contains("#")) {
            return false;
        }

        try {
            URI rootUri = new URI(rootUrl);
            URI targetUri = new URI(targetUrl);

            String rootHost = rootUri.getHost();
            String targetHost = targetUri.getHost();

            // 限制必須為同網域
            if (rootHost != null && targetHost != null) {
                return rootHost.equalsIgnoreCase(targetHost);
            }
        } catch (Exception e) {
            return false;
        }

        if (lowerUrl.contains("special:") || 
            lowerUrl.contains("help:") || 
            lowerUrl.contains("wikipedia:") || 
            lowerUrl.contains("portal:") || 
            lowerUrl.contains("talk:") || 
            lowerUrl.contains("user:")) {
            return false; // 直接排除這些系統導覽頁
        }


        return false;
    }
}