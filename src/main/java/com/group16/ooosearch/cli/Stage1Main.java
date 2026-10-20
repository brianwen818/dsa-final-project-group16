package com.group16.ooosearch.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.PageResult;
import com.group16.ooosearch.model.WebPage;
import com.group16.ooosearch.scoring.FakePageScorer;
import com.group16.ooosearch.scoring.PageScorer;

/**
 * Stage 1 命令列 Demo：對每個網址計分並依分數排名，印出名次、分數與各關鍵字次數。
 *
 * Stage 0 版本：使用 FakePageScorer 與不連網路的範例網頁，只用來確認專案能執行。
 * TODO(Stage 1)：改用 fetch/PageFetcher 抓真實網頁，並換成真正的 PageScorer 實作。
 */
public class Stage1Main {
    private static final Path KEYWORDS_FILE = Path.of("data", "keywords.txt");

    public static void main(String[] args) throws IOException {
        KeywordList keywords = KeywordList.fromFile(KEYWORDS_FILE);
        System.out.println("關鍵字：" + keywords);

        List<WebPage> pages = List.of(
                new WebPage("https://example.com/ooo/1", "範例網頁 1", ""),
                new WebPage("https://example.com/ooo/2", "範例網頁 2", ""));

        PageScorer scorer = new FakePageScorer();
        List<PageResult> results = new ArrayList<>();
        for (WebPage page : pages) {
            results.add(scorer.score(page, keywords));
        }
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        printTable(results, keywords);
    }

    private static void printTable(List<PageResult> results, KeywordList keywords) {
        StringBuilder header = new StringBuilder("名次\t網址\t分數");
        for (Keyword keyword : keywords) {
            header.append('\t').append(keyword.getName());
        }
        System.out.println(header);

        for (int i = 0; i < results.size(); i++) {
            PageResult result = results.get(i);
            StringBuilder row = new StringBuilder()
                    .append(i + 1).append('\t')
                    .append(result.getPage().getUrl()).append('\t')
                    .append(String.format("%.2f", result.getScore()));
            for (Keyword keyword : keywords) {
                row.append('\t').append(result.getCount(keyword.getName()));
            }
            System.out.println(row);
        }
    }
}
