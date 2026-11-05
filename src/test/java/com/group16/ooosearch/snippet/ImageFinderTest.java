package com.group16.ooosearch.snippet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.group16.ooosearch.fetch.PageCache;
import com.group16.ooosearch.fetch.PageFetcher;
import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.Passage;

/** Fandom API 的回應都預先放進快取，因此測試不連網路。 */
class ImageFinderTest {
    private static final String API = "https://kamenrider.fandom.com/api.php?format=json&formatversion=2&";

    @TempDir
    Path cacheDir;

    private PageCache cache;
    private ImageFinder finder;
    private final KeywordList keywords = new KeywordList(List.of(
            new Keyword("假面騎士OOO", 5, List.of("Kamen Rider OOO")),
            new Keyword("卡佳里", 3.7, List.of("カザリ", "Kazari")),
            new Keyword("獅虎豹組合", 2.9, List.of("獅虎豹", "Latorartar Combo")),
            new Keyword("泉比奈", 2.6, List.of())));

    @BeforeEach
    void setUp() {
        cache = new PageCache(cacheDir);
        finder = new ImageFinder(new PageFetcher(cache, 0, 200));
    }

    private static Passage passage(String... tags) {
        return new Passage("段落", List.of(), "https://a.test/", "https://a.test/", "A", 1, 1, List.of(tags), null, null, null, null);
    }

    @Test
    void usesFandomPageImageOfTheTagsEnglishName() throws IOException {
        cache.put(API + "action=query&prop=pageimages&pithumbsize=300&redirects=1&titles=Kazari",
                "{\"query\":{\"pages\":[{\"title\":\"Kazari\",\"thumbnail\":{\"source\":\"https://img.test/kazari.png\"}}]}}");

        Passage result = finder.addImage(passage("假面騎士OOO", "卡佳里"), keywords);

        assertEquals("https://img.test/kazari.png", result.imageUrl());
        assertEquals("Kamen Rider Wiki", result.imageSource());
        assertEquals("https://kamenrider.fandom.com/wiki/Kazari", result.imageSourceUrl());
    }

    @Test
    void redirectToASectionFallsBackToFileSearch() throws IOException {
        cache.put(API + "action=query&prop=pageimages&pithumbsize=300&redirects=1&titles=Latorartar+Combo",
                "{\"query\":{\"redirects\":[{\"from\":\"Latorartar Combo\",\"to\":\"Kamen Rider OOO (Rider)\",\"tofragment\":\"Combos\"}],"
                + "\"pages\":[{\"title\":\"Kamen Rider OOO (Rider)\",\"thumbnail\":{\"source\":\"https://img.test/eiji.png\"}}]}}");
        cache.put(API + "action=query&list=search&srnamespace=6&srlimit=1&srsearch=Latorartar+Combo",
                "{\"query\":{\"search\":[{\"title\":\"File:Latorartar.png\"}]}}");
        cache.put(API + "action=query&prop=imageinfo&iiprop=url&iiurlwidth=300&titles=File%3ALatorartar.png",
                "{\"query\":{\"pages\":[{\"imageinfo\":[{\"thumburl\":\"https://img.test/latorartar.png\"}]}]}}");

        Passage result = finder.addImage(passage("獅虎豹組合"), keywords);

        assertEquals("https://img.test/latorartar.png", result.imageUrl()); // 不是主角的圖
    }

    @Test
    void noEnglishNameOrNoResultMeansNoImage() throws IOException {
        cache.put(API + "action=query&prop=pageimages&pithumbsize=300&redirects=1&titles=Kazari",
                "{\"query\":{\"pages\":[{\"title\":\"Kazari\",\"missing\":true}]}}");
        cache.put(API + "action=query&list=search&srnamespace=6&srlimit=1&srsearch=Kazari",
                "{\"query\":{\"search\":[]}}");

        assertNull(finder.addImage(passage("泉比奈"), keywords).imageUrl());
        assertFalse(finder.addImage(passage("卡佳里"), keywords).hasImage());
    }

    @Test
    void existingImageIsKept() {
        Passage withImage = passage("卡佳里").withImage("https://own.test/a.png", "A", "https://a.test/");
        assertEquals("https://own.test/a.png", finder.addImage(withImage, keywords).imageUrl());
    }
}
