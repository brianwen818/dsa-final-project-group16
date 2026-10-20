package com.group16.ooosearch.fetch;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Set;

import org.jsoup.parser.Parser;

/**
 * 從 HTML 抽取使用者看得到的正文，概念來自 HW5 的 HTML 標籤配對（Stack）。
 *
 * - script、style、title 等「原始文字」標籤：內容可能含有「<」，因此直接跳到對應的結束標籤。
 * - nav、footer、svg 等不屬於正文的區塊：開始標籤 push、結束標籤 pop；Stack 不是空的時候，文字都不收。
 * - 註解（<!-- -->）、<!doctype>、<?xml?> 直接跳過；其他標籤以空白取代，避免前後文字黏在一起。
 * - 最後還原 HTML 實體（&amp; → &、&nbsp; → 空白）並合併多餘空白。
 */
public class TextExtractor {
    /**
     * 注意：template 不在這裡。它的內容是一般 HTML，而且有些網站（例如萌娘百科）把整篇文章放在 template 裡，
     * 由前端 JavaScript 顯示，跳過的話會整篇文章都不見。
     */
    private static final Set<String> RAW_TEXT_TAGS = Set.of("script", "style", "title", "noscript", "textarea");
    private static final Set<String> SKIPPED_TAGS = Set.of("nav", "footer", "svg");

    public String extract(String html) {
        StringBuilder text = new StringBuilder();
        Deque<String> skipped = new ArrayDeque<>(); // 當作 Stack 使用：push、pop、peek
        int n = html.length();
        int i = 0;
        while (i < n) {
            int lt = html.indexOf('<', i);
            if (lt < 0) {
                lt = n;
            }
            if (skipped.isEmpty()) {
                text.append(html, i, lt);
            }
            if (lt >= n) {
                break;
            }
            if (!looksLikeTag(html, lt)) {
                // 例如「1 < 2」：「<」只是一般文字
                if (skipped.isEmpty()) {
                    text.append('<');
                }
                i = lt + 1;
                continue;
            }
            if (html.startsWith("<!--", lt)) {
                int end = html.indexOf("-->", lt + 4);
                i = end < 0 ? n : end + 3;
                continue;
            }
            int gt = html.indexOf('>', lt);
            if (gt < 0) {
                break;
            }
            String tag = html.substring(lt + 1, gt);
            i = gt + 1;
            if (tag.startsWith("!") || tag.startsWith("?")) {
                continue;
            }
            boolean closing = tag.startsWith("/");
            boolean selfClosing = tag.endsWith("/");
            String name = tagName(tag, closing);

            if (!closing && !selfClosing && RAW_TEXT_TAGS.contains(name)) {
                i = skipRawText(html, gt + 1, name);
            } else if (SKIPPED_TAGS.contains(name)) {
                if (!closing && !selfClosing) {
                    skipped.push(name);
                } else if (closing && skipped.contains(name)) {
                    // 正常情況下 Stack 頂端就是它；HTML 沒有正確關閉時，一路 pop 到對應的開始標籤
                    while (!skipped.pop().equals(name)) {
                        // 繼續 pop
                    }
                }
            }
            text.append(' ');
        }
        String decoded = Parser.unescapeEntities(text.toString(), false);
        return decoded.replace(' ', ' ').replaceAll("\\s+", " ").strip();
    }

    /** 「<」後面接英文字母、/、! 或 ? 才當作標籤。 */
    private static boolean looksLikeTag(String html, int lt) {
        if (lt + 1 >= html.length()) {
            return false;
        }
        char c = html.charAt(lt + 1);
        return Character.isLetter(c) || c == '/' || c == '!' || c == '?';
    }

    private static String tagName(String tag, boolean closing) {
        int start = closing ? 1 : 0;
        int end = start;
        while (end < tag.length() && (Character.isLetterOrDigit(tag.charAt(end)) || tag.charAt(end) == '-')) {
            end++;
        }
        return tag.substring(start, end).toLowerCase(Locale.ROOT);
    }

    /** 從 from 開始找 </name>（忽略大小寫），回傳結束標籤之後的位置。 */
    private static int skipRawText(String html, int from, String name) {
        String closeTag = "</" + name;
        for (int k = from; k + closeTag.length() <= html.length(); k++) {
            if (html.charAt(k) == '<' && html.regionMatches(true, k, closeTag, 0, closeTag.length())) {
                int gt = html.indexOf('>', k);
                return gt < 0 ? html.length() : gt + 1;
            }
        }
        return html.length();
    }
}
