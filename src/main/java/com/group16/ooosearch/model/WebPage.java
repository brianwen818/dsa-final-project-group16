package com.group16.ooosearch.model;

/**
 * 一個網頁的內容。
 * 與 HW6 不同，WebPage 不會自己抓網頁，而是由 fetch/PageFetcher 建立。
 */
public class WebPage {
    private final String url;
    private final String title;
    private final String html;

    public WebPage(String url, String title, String html) {
        this.url = url;
        this.title = title == null ? "" : title;
        this.html = html == null ? "" : html;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    /** 原始 HTML。抽取正文（跳過 script、style 等）由 Stage 1 的 TextExtractor 負責。 */
    public String getHtml() {
        return html;
    }

    @Override
    public String toString() {
        return title.isEmpty() ? url : title + " (" + url + ")";
    }
}
