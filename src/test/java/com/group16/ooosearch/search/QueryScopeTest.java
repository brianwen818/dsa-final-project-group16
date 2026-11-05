package com.group16.ooosearch.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;

class QueryScopeTest {

    private final Keyword topic = new Keyword("假面騎士OOO", 5, List.of("OOO", "オーズ"));

    @Test
    void synonymLineIsFoundByAnyForm(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("query-synonyms.txt");
        Files.writeString(file, "# 註解\n貓咪 | 貓, 猫, Cat\n", StandardCharsets.UTF_8);

        assertEquals(List.of("貓咪", "貓", "猫", "Cat"), QueryScope.queryKeyword("貓咪", file).getForms());
        assertEquals(List.of("cat", "貓咪", "貓", "猫"), QueryScope.queryKeyword("cat", file).getForms());
        assertEquals(List.of("火野映司"), QueryScope.queryKeyword("火野映司", file).getForms());
    }

    @Test
    void queryThatIsAnOooKeywordReusesItsForms() throws IOException {
        KeywordList keywords = new KeywordList(List.of(
                new Keyword("核心硬幣", 4.5, List.of("コアメダル", "核心硬币")),
                new Keyword("安卡拉", 0)));

        assertEquals(List.of("核心硬幣", "コアメダル", "核心硬币"),
                QueryScope.queryKeyword("核心硬幣", Path.of("no-such-file.txt"), keywords).getForms());
        assertEquals(List.of("安卡拉"),
                QueryScope.queryKeyword("安卡拉", Path.of("no-such-file.txt"), keywords).getForms());
    }

    @Test
    void exclusionsAreSeparatedFromForms(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("query-synonyms.txt");
        Files.writeString(file, "貓咪 | 貓, 猫 | !熊貓, !貓頭鷹\n", StandardCharsets.UTF_8);

        assertEquals(List.of("貓", "貓咪", "猫"), QueryScope.queryKeyword("貓", file).getForms());
        assertEquals(List.of("熊貓", "貓頭鷹"),
                QueryScope.queryExclusions("貓咪", file).stream().map(Keyword::getName).toList());
        assertEquals(0.0, QueryScope.queryExclusions("貓咪", file).get(0).getWeight());
        assertEquals(List.of(), QueryScope.queryExclusions("熊貓", file)); // 排除詞本身不會比對到這一行
    }

    @Test
    void missingSynonymFileUsesQueryOnly() throws IOException {
        assertEquals(List.of("貓咪"), QueryScope.queryKeyword(" 貓咪 ", Path.of("no-such-file.txt")).getForms());
    }

    @Test
    void scopedQueriesAppendTopicUnlessAlreadyMentioned() {
        Keyword cat = new Keyword("貓咪", 1, List.of("貓", "猫"));
        assertEquals(List.of("貓咪 假面騎士OOO", "貓 假面騎士OOO"), QueryScope.scopedQueries(cat, topic, 2));

        Keyword ending = new Keyword("ooo 結局", 1);
        assertEquals(List.of("ooo 結局"), QueryScope.scopedQueries(ending, topic, 2));
    }
}
