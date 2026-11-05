package com.group16.ooosearch.tree;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 排名結果的共用整理工具（Stage 3 的命令列與正式版搜尋引擎共用）。 */
public final class SiteResults {
    private SiteResults() {
    }

    /** 根網址抓不到時，根節點沒有計分結果，代表無法評分。 */
    public static boolean isScorable(SiteResult result) {
        return result.getTree().getRoot().getPageResult() != null;
    }

    /** 可評分的網站在前（保留原本的分數順序），無法評分的排最後。 */
    public static List<SiteResult> sortScorableFirst(List<SiteResult> results) {
        List<SiteResult> sorted = new ArrayList<>(results);
        sorted.sort(Comparator.comparing((SiteResult r) -> !isScorable(r))); // 穩定排序
        return sorted;
    }

    /**
     * 同一個網站、首頁標題相同的結果只留第一個（分數最高的）。
     * 例如維基的「假面騎士OOO 10th 復活的核心硬幣」會重新導向到「OOO 10th」，兩個網址其實是同一頁。
     * 不同網域（例如萌娘百科的 zh 與 mzh）可能是不同條目，不合併。
     */
    public static List<SiteResult> dropRedirectDuplicates(List<SiteResult> results) {
        Set<String> seen = new HashSet<>();
        List<SiteResult> kept = new ArrayList<>();
        for (SiteResult result : results) {
            if (!isScorable(result)) {
                kept.add(result);
                continue;
            }
            String key = host(result.getRootUrl()) + "|" + result.getTree().getRoot().getWebPage().getTitle();
            if (seen.add(key)) {
                kept.add(result);
            }
        }
        return kept;
    }

    private static String host(String url) {
        try {
            String host = URI.create(url).getHost();
            return host == null ? url : host;
        } catch (IllegalArgumentException e) {
            return url;
        }
    }
}
