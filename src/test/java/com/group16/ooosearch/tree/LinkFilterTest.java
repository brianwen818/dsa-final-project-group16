package com.group16.ooosearch.tree;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LinkFilterTest {
    private static final String ROOT = "https://zh.wikipedia.org/wiki/%E5%81%87%E9%9D%A2%E9%A8%8E%E5%A3%ABOOO";

    @Test
    void keepsArticlesOnSameSite() {
        assertTrue(LinkFilter.isValidLink(ROOT, "https://zh.wikipedia.org/wiki/%E7%81%AB%E9%87%8E%E6%98%A0%E5%8F%B8"));
        assertTrue(LinkFilter.isValidLink(ROOT, "https://zh.wikipedia.org/zh-tw/%E5%AE%89%E9%85%B7"));
    }

    @Test
    void rejectsOtherSites() {
        assertFalse(LinkFilter.isValidLink(ROOT, "https://ja.wikipedia.org/wiki/OOO"));
        assertFalse(LinkFilter.isValidLink(ROOT, "https://example.com/"));
    }

    @Test
    void rejectsMaintenancePagesIncludingEncodedColon() {
        assertFalse(LinkFilter.isValidLink(ROOT, "https://zh.wikipedia.org/wiki/Talk:OOO"));
        assertFalse(LinkFilter.isValidLink(ROOT, "https://zh.wikipedia.org/wiki/Talk%3AOOO"));
        assertFalse(LinkFilter.isValidLink(ROOT, "https://zh.wikipedia.org/wiki/Wikipedia%3A%E5%8F%AF%E9%9D%A0"));
        assertFalse(LinkFilter.isValidLink(ROOT, "https://zh.wikipedia.org/wiki/Template%3ACitation_needed"));
        assertFalse(LinkFilter.isValidLink(ROOT, "https://zh.wikipedia.org/w/index.php?title=OOO&action=edit"));
        assertFalse(LinkFilter.isValidLink("https://ja.wikipedia.org/wiki/OOO",
                "https://ja.wikipedia.org/wiki/%E3%83%8E%E3%83%BC%E3%83%88%3AOOO"));
        assertFalse(LinkFilter.isValidLink("https://zh.moegirl.org.cn/OOO",
                "https://zh.moegirl.org.cn/%E8%90%8C%E5%A8%98%E7%99%BE%E7%A7%91:%E7%BC%96%E8%BE%91"));
    }

    @Test
    void keepsTitleThatOnlyContainsColon() {
        assertTrue(LinkFilter.isValidLink(ROOT, "https://zh.wikipedia.org/wiki/Movie%E5%A4%A7%E6%88%B0:Core"));
    }

    @Test
    void rejectsFilesAnchorsAndLogin() {
        assertFalse(LinkFilter.isValidLink(ROOT, "https://zh.wikipedia.org/poster.JPG"));
        assertFalse(LinkFilter.isValidLink(ROOT, "https://zh.wikipedia.org/wiki/OOO#cite"));
        assertFalse(LinkFilter.isValidLink(ROOT, "https://zh.wikipedia.org/wiki/Login"));
        assertFalse(LinkFilter.isValidLink(ROOT, null));
    }
}
