package com.group16.ooosearch.tree;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

public class LinkFilter {
    /** MediaWiki 的非條目命名空間（英文、繁簡中文、日文），比對時已轉小寫。 */
    private static final Set<String> NON_ARTICLE_NAMESPACES = Set.of(
            "special", "help", "wikipedia", "wikiproject", "portal", "talk", "user", "user talk",
            "file", "template", "category", "mediawiki", "module", "draft",
            "特殊", "幫助", "帮助", "討論", "讨论", "使用者", "用户", "檔案", "文件", "模板", "分類", "分类",
            "特別", "ヘルプ", "ノート", "利用者", "ファイル", "テンプレート", "カテゴリ", "プロジェクト",
            "萌娘百科");
    private static final Set<String> FILE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".svg", ".webp", ".pdf");

    public static boolean isValidLink(String rootUrl, String targetUrl) {
        if (targetUrl == null || targetUrl.isEmpty()) {
            return false;
        }

        String lowerUrl = targetUrl.toLowerCase(Locale.ROOT);
        // 排除非內文頁面、登入、錨點
        if (lowerUrl.contains("login") || lowerUrl.contains("signup") ||
            lowerUrl.contains("logout") || lowerUrl.contains("javascript:") ||
            lowerUrl.contains("#")) {
            return false;
        }

        try {
            URI rootUri = new URI(rootUrl);
            URI targetUri = new URI(targetUrl);

            // 限制必須為同網域
            String rootHost = rootUri.getHost();
            String targetHost = targetUri.getHost();
            if (rootHost == null || targetHost == null || !rootHost.equalsIgnoreCase(targetHost)) {
                return false;
            }

            // getPath() 會還原百分比編碼，因此 Talk%3A、ノート 都比對得到
            String path = targetUri.getPath() == null ? "" : targetUri.getPath().toLowerCase(Locale.ROOT);
            return !isFileOrScript(path) && !isNonArticlePage(path);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isFileOrScript(String path) {
        if (path.endsWith("index.php") || path.endsWith("api.php")) {
            return true; // 編輯頁、歷史頁等都走 index.php
        }
        for (String extension : FILE_EXTENSIONS) {
            if (path.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    /** 條目名稱的第一個冒號之前如果是非條目命名空間，就是系統或維護頁面。 */
    private static boolean isNonArticlePage(String path) {
        String title = path.replaceFirst("^/(wiki|zh(-[a-z]+)?)/", "").replaceFirst("^/", "");
        int colon = title.indexOf(':');
        if (colon < 0) {
            return false;
        }
        String namespace = title.substring(0, colon).replace('_', ' ');
        return NON_ARTICLE_NAMESPACES.contains(namespace) || namespace.endsWith(" talk");
    }
}
