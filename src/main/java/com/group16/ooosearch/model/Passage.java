package com.group16.ooosearch.model;

import java.util.List;

/**
 * 網頁中的一個相關段落（段落摘要），用來告訴使用者「為什麼這個結果和查詢有關」。
 *
 * <p>例如搜尋「貓咪」時，中文維基「假面騎士OOO角色列表」的一段：
 * 章節路徑「主要角色 › 貪婪 › 卡佳里」、摘要「…貓系幹部。下半身不完全，代表色是黃色…」、標籤「卡佳里」、卡佳里的圖片。
 *
 * @param text           摘要文字（已裁成查詢詞前後一小段）
 * @param headingPath    段落所在的章節標題，由外到內
 * @param anchorUrl      直接跳到該章節的網址（沒有章節錨點時就是網頁網址）
 * @param pageUrl        段落所在網頁的網址
 * @param pageTitle      段落所在網頁的標題
 * @param score          段落分數 P_q(t)
 * @param queryCount     查詢詞在段落中出現的次數
 * @param tags           這段在講誰：段落中最重要的 OOO 關鍵字
 * @param imageUrl       圖片網址；沒有圖片時為 null
 * @param imageSource    圖片來源名稱（例如「Kamen Rider Wiki」）
 * @param imageSourceUrl 圖片來源網頁
 * @param sectionImageUrl 段落所在章節的第一張圖（備用：章節可能同時介紹好幾樣東西，所以排在標籤查到的圖之後）
 */
public record Passage(
        String text,
        List<String> headingPath,
        String anchorUrl,
        String pageUrl,
        String pageTitle,
        double score,
        int queryCount,
        List<String> tags,
        String imageUrl,
        String imageSource,
        String imageSourceUrl,
        String sectionImageUrl) {

    public Passage {
        headingPath = List.copyOf(headingPath);
        tags = List.copyOf(tags);
    }

    public boolean hasImage() {
        return imageUrl != null && !imageUrl.isEmpty();
    }

    /** 回傳附上圖片的新段落。 */
    public Passage withImage(String url, String source, String sourceUrl) {
        return new Passage(text, headingPath, anchorUrl, pageUrl, pageTitle, score, queryCount, tags,
                url, source, sourceUrl, sectionImageUrl);
    }

    /** 章節路徑以「 › 」連接，例如「主要角色 › 貪婪 › 卡佳里」。 */
    public String headingText() {
        return String.join(" › ", headingPath);
    }
}
