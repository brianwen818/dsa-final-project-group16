package com.group16.ooosearch.snippet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.group16.ooosearch.model.WebPage;

class PassageExtractorTest {
    private final PassageExtractor extractor = new PassageExtractor();

    private List<PassageExtractor.Unit> extract(String body) {
        return extractor.extract(new WebPage("https://zh.wikipedia.org/wiki/OOO", "OOO", "<html><body>" + body + "</body></html>"));
    }

    @Test
    void headingStackGivesNestedPathAndAnchor() {
        List<PassageExtractor.Unit> units = extract(
                "<h2 id=\"角色\">主要角色</h2><p>這是主要角色的介紹段落。</p>"
                + "<h3><span class=\"mw-headline\" id=\"貪婪\">貪婪</span></h3>"
                + "<h4 id=\"卡佳里\">卡佳里</h4><p>卡佳里是貓系的幹部，代表色是黃色。</p>"
                + "<h2 id=\"聯組\">聯組</h2><p>獅虎豹是貓系聯組的組合。</p>");

        assertEquals(3, units.size());
        assertEquals(List.of("主要角色"), units.get(0).headingPath());
        assertEquals(List.of("主要角色", "貪婪", "卡佳里"), units.get(1).headingPath());
        assertEquals("卡佳里", units.get(1).anchorId());
        // 遇到新的 h2 時，h3、h4 都要 pop 掉
        assertEquals(List.of("聯組"), units.get(2).headingPath());
        assertEquals("聯組", units.get(2).anchorId());
    }

    @Test
    void tableRowsAndListItemsBecomeUnitsWithoutDuplicates() {
        List<PassageExtractor.Unit> units = extract(
                "<table><tr><th>LaTorarTar COMBO 獅虎豹</th><td>貓系聯組，頭部能放出強光</td></tr></table>"
                + "<ul><li>第 3 集 貓‧進化‧貪吃鬼<ul><li>ネコヤミー 登場的一集</li></ul></li></ul>"
                + "<p>短</p>");

        assertEquals(2, units.size());
        assertEquals("LaTorarTar COMBO 獅虎豹 貓系聯組，頭部能放出強光", units.get(0).text());
        assertEquals("ネコヤミー 登場的一集", units.get(1).text()); // 外層 li 含子清單，略過
    }

    @Test
    void largeImagesAreKeptForTheSectionButIconsAreIgnored() {
        List<PassageExtractor.Unit> units = extract(
                "<h2 id=\"a\">卡佳里</h2><img src=\"//upload.test/icon.png\" width=\"20\">"
                + "<img src=\"//upload.test/kazari.png\" width=\"220\"><p>卡佳里是貓系的幹部之一。</p>"
                + "<h2 id=\"b\">其他</h2><p>這一節沒有任何圖片可用。</p>");

        assertEquals("https://upload.test/kazari.png", units.get(0).image());
        assertNull(units.get(1).image());
        assertTrue(PassageExtractor.MIN_IMAGE_WIDTH > 20);
    }
}
