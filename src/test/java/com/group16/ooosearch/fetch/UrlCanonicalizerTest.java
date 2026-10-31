package com.group16.ooosearch.fetch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UrlCanonicalizerTest {
    @Test
    void wikipediaVariantsAreSamePage() {
        String expected = "https://zh.wikipedia.org/wiki/%E5%AE%89";
        assertEquals(expected, UrlCanonicalizer.canonicalize("https://zh.wikipedia.org/zh-tw/%E5%AE%89"));
        assertEquals(expected, UrlCanonicalizer.canonicalize("https://zh.wikipedia.org/zh-hant/%E5%AE%89"));
        assertEquals(expected, UrlCanonicalizer.canonicalize("https://ZH.wikipedia.org/wiki/%e5%ae%89#top"));
    }

    @Test
    void trailingSlashAndMoegirlVariantAreRemovedAndQueryKept() {
        assertEquals("https://zh.moegirl.org.cn/OOO", UrlCanonicalizer.canonicalize("https://zh.moegirl.org.cn/zh-tw/OOO/"));
        assertEquals("https://a.test/x?sn=1", UrlCanonicalizer.canonicalize("https://a.test/x?sn=1"));
        assertEquals("https://a.test/", UrlCanonicalizer.canonicalize("https://a.test/"));
    }

    @Test
    void unparsableUrlIsReturnedAsIs() {
        assertEquals("not a url", UrlCanonicalizer.canonicalize(" not a url "));
    }
}
