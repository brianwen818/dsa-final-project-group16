package com.group16.ooosearch.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.group16.ooosearch.fetch.PageCache;
import com.group16.ooosearch.fetch.PageFetcher;

/** Wikidata 的回應都預先放進快取，測試不連網路（沒放進快取的網址若被請求，會因連線逾時而失敗）。 */
class WikidataTranslatorTest {
    private static final String EMPTY_SEARCH = "{\"search\":[]}";

    @TempDir
    Path dir;

    private PageCache cache;
    private WikidataTranslator translator;

    @BeforeEach
    void setUp() {
        cache = new PageCache(dir);
        translator = new WikidataTranslator(new PageFetcher(cache, 0, 200));
    }

    private void search(String query, String lang, String... ids) throws IOException {
        StringBuilder json = new StringBuilder("{\"search\":[");
        for (int i = 0; i < ids.length; i++) {
            json.append(i > 0 ? "," : "").append("{\"id\":\"").append(ids[i]).append("\"}");
        }
        cache.put(WikidataTranslator.searchUrl(query, lang), json.append("]}").toString());
    }

    private void emptySearches(String query) throws IOException {
        for (String lang : WikidataTranslator.LATIN_LANGUAGES) {
            cache.put(WikidataTranslator.searchUrl(query, lang), EMPTY_SEARCH);
        }
    }

    @Test
    void frenchChatBecomesCatAndOtherSensesAreKept() throws IOException {
        emptySearches("chat");
        search("chat", "en", "Q1");           // 英文 chat：線上聊天
        search("chat", "fr", "Q146", "Q1");   // 法文 chat：貓
        cache.put(WikidataTranslator.entitiesUrl(List.of("Q1", "Q146")), """
                {"entities":{
                  "Q1":{"labels":{"zh-tw":{"value":"線上聊天"},"ja":{"value":"チャット"}}},
                  "Q146":{"labels":{"zh-tw":{"value":"貓"},"zh-hans":{"value":"猫"},"ja":{"value":"ネコ"}}}
                }}""");

        List<String> result = translator.translate("chat");

        // Q1 與 Q146 最佳名次都是第 1，Q1 被兩種語言查到，所以排前面
        assertEquals(List.of("線上聊天", "チャット", "貓", "猫", "ネコ"), result);
    }

    @Test
    void chineseOrJapaneseQueryIsNotTranslated() throws IOException {
        // 快取是空的：若有發出請求就會連網，這裡直接回傳空清單證明沒有請求
        assertEquals(List.of(), translator.translate("貓咪"));
        assertEquals(List.of(), translator.translate("ネコ"));
        assertEquals(List.of(), translator.translate("  "));
    }

    @Test
    void koreanIsSearchedOnlyInKorean() throws IOException {
        search("고양이", "ko", "Q146");
        cache.put(WikidataTranslator.entitiesUrl(List.of("Q146")),
                "{\"entities\":{\"Q146\":{\"labels\":{\"zh-tw\":{\"value\":\"貓\"}}}}}");

        assertEquals(List.of("ko"), WikidataTranslator.languagesFor("고양이"));
        assertEquals(List.of("貓"), translator.translate("고양이"));
    }

    @Test
    void entitiesWithoutChineseOrJapaneseLabelsAreSkipped() throws IOException {
        emptySearches("Katze");
        search("Katze", "de", "Q4167410", "Q146");
        cache.put(WikidataTranslator.entitiesUrl(List.of("Q4167410", "Q146")), """
                {"entities":{
                  "Q4167410":{"labels":{"zh":{"value":"Katze"}}},
                  "Q146":{"labels":{"zh":{"value":"貓"},"ja":{"value":"ネコ"}}}
                }}""");

        List<String> result = translator.translate("Katze");

        assertEquals(List.of("貓", "ネコ"), result);
        assertTrue(result.stream().allMatch(WikidataTranslator::containsCjk));
    }

    @Test
    void noMatchMeansNoTranslation() throws IOException {
        emptySearches("xyzzy");
        assertEquals(List.of(), translator.translate("xyzzy"));
    }
}
