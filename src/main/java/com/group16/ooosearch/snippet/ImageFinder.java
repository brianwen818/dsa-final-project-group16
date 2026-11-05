package com.group16.ooosearch.snippet;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import com.group16.ooosearch.fetch.PageFetcher;
import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.Passage;
import com.group16.ooosearch.search.QueryScope;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 為段落找一張圖片（段落摘要的第 4 步）。依序嘗試：
 * <ol>
 *   <li>段落本身的圖片（例如表格列裡的圖）。</li>
 *   <li>用標籤的英文寫法（關鍵字表中的別名，例如卡佳里的「Kazari」）查 Kamen Rider Wiki（Fandom）條目的代表圖。</li>
 *   <li>如果該名稱被導向到別的條目的某個章節（例如「Latorartar Combo」導向 OOO 主條目），代表圖會是別的角色，
 *       改用 Fandom 的檔案搜尋找圖。</li>
 *   <li>段落所在章節的第一張圖。排在最後，因為一個章節常同時介紹好幾樣東西
 *       （例如萌娘百科「聯組形態」的第一張圖是鍬螳蝗，不能配給獅虎豹的段落）。</li>
 * </ol>
 * 都找不到就不加圖。只記錄圖片網址與來源，不下載、不重新上傳。查詢結果依名稱存在 HashMap，同一個名稱只查一次。
 */
public class ImageFinder {
    public static final String FANDOM_HOST = "kamenrider.fandom.com";
    private static final String API = "https://" + FANDOM_HOST + "/api.php?format=json&formatversion=2&";
    private static final String SOURCE = "Kamen Rider Wiki";
    /** 英文寫法：英文字母開頭，只含英數、空白與少數符號。 */
    private static final Pattern ENGLISH = Pattern.compile("[A-Za-z][A-Za-z0-9 '\\-]*");

    public record Image(String url, String sourceUrl) {
    }

    private final PageFetcher fetcher;
    private final JsonMapper json = JsonMapper.builder().build();
    private final Map<String, Optional<Image>> cache = new HashMap<>();

    public ImageFinder(PageFetcher fetcher) {
        this.fetcher = fetcher;
    }

    /** 回傳附上圖片的段落；已有圖片或找不到時原樣回傳。 */
    public Passage addImage(Passage passage, KeywordList keywords) {
        if (passage.hasImage()) {
            return passage;
        }
        for (String tag : passage.tags()) {
            if (tag.equals(QueryScope.TOPIC)) {
                continue; // 主題本身的代表圖是主角，無法說明這一段
            }
            String english = englishName(tag, keywords);
            if (english == null) {
                continue;
            }
            Optional<Image> image = find(english);
            if (image.isPresent()) {
                return passage.withImage(image.get().url(), SOURCE, image.get().sourceUrl());
            }
        }
        if (passage.sectionImageUrl() != null) {
            return passage.withImage(passage.sectionImageUrl(), passage.pageTitle(), passage.anchorUrl());
        }
        return passage;
    }

    /** 依英文名稱找圖；網路錯誤時視為找不到。 */
    public Optional<Image> find(String englishName) {
        return cache.computeIfAbsent(englishName, name -> {
            try {
                return lookup(name);
            } catch (Exception e) {
                System.err.println("ImageFinder 查詢失敗: " + name + " (" + e.getMessage() + ")");
                return Optional.empty();
            }
        });
    }

    private Optional<Image> lookup(String name) throws IOException {
        JsonNode query = request("action=query&prop=pageimages&pithumbsize=300&redirects=1&titles=" + encode(name));
        boolean redirectedToSection = false;
        for (JsonNode redirect : query.path("redirects")) {
            if (redirect.has("tofragment")) {
                redirectedToSection = true;
            }
        }
        if (!redirectedToSection) {
            for (JsonNode page : query.path("pages")) {
                String thumb = page.path("thumbnail").path("source").asString("");
                if (!thumb.isEmpty()) {
                    return Optional.of(new Image(thumb, pageUrl(page.path("title").asString(name))));
                }
            }
        }
        return searchFile(name);
    }

    /** 在檔案命名空間（ns 6）搜尋名稱，取第一個檔案的縮圖。 */
    private Optional<Image> searchFile(String name) throws IOException {
        JsonNode search = request("action=query&list=search&srnamespace=6&srlimit=1&srsearch=" + encode(name));
        JsonNode hits = search.path("search");
        if (hits.isEmpty()) {
            return Optional.empty();
        }
        String fileTitle = hits.get(0).path("title").asString("");
        if (fileTitle.isEmpty()) {
            return Optional.empty();
        }
        JsonNode info = request("action=query&prop=imageinfo&iiprop=url&iiurlwidth=300&titles=" + encode(fileTitle));
        for (JsonNode page : info.path("pages")) {
            JsonNode first = page.path("imageinfo").path(0);
            String url = first.path("thumburl").asString(first.path("url").asString(""));
            if (!url.isEmpty()) {
                return Optional.of(new Image(url, pageUrl(fileTitle)));
            }
        }
        return Optional.empty();
    }

    private JsonNode request(String params) throws IOException {
        return json.readTree(fetcher.fetchText(API + params)).path("query");
    }

    /** 標籤對應的關鍵字中，第一個英文寫法。 */
    static String englishName(String tag, KeywordList keywords) {
        for (Keyword keyword : keywords) {
            if (keyword.getName().equals(tag)) {
                for (String form : keyword.getForms()) {
                    if (ENGLISH.matcher(form).matches() && form.length() > 3) {
                        return form;
                    }
                }
            }
        }
        return null;
    }

    private static String pageUrl(String title) {
        return "https://" + FANDOM_HOST + "/wiki/" + encode(title.replace(' ', '_')).replace("+", "%20");
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
