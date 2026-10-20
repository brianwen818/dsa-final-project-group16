package com.group16.ooosearch.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.group16.ooosearch.fetch.PageCache;
import com.group16.ooosearch.fetch.PageFetcher;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.PageResult;
import com.group16.ooosearch.model.WebPage;
import com.group16.ooosearch.scoring.KeywordPageScorer;
import com.group16.ooosearch.scoring.PageScorer;

/**
 * Stage 1 命令列 Demo：抓取每個網址、計算分數並排名，印出名次、分數與各關鍵字次數。
 *
 * 用法：Stage1Main [網址檔] [關鍵字檔]
 * 預設為 data/stage1-urls.txt 與 data/keywords.txt；結果另存到 data/results/stage1-results.csv。
 * 抓取失敗的網址會印出原因並略過，不會中斷整個程式。
 */
public class Stage1Main {
    private static final Path DEFAULT_URLS_FILE = Path.of("data", "stage1-urls.txt");
    private static final Path DEFAULT_KEYWORDS_FILE = Path.of("data", "keywords.txt");
    private static final Path RESULTS_FILE = Path.of("data", "results", "stage1-results.csv");

    /** 網址檔中的一行：網址與說明。 */
    private static class Target {
        final String url;
        final String label;

        Target(String url, String label) {
            this.url = url;
            this.label = label;
        }
    }

    private static class Ranked {
        final Target target;
        final PageResult result;

        Ranked(Target target, PageResult result) {
            this.target = target;
            this.result = result;
        }
    }

    public static void main(String[] args) throws IOException {
        Path urlsFile = args.length > 0 ? Path.of(args[0]) : DEFAULT_URLS_FILE;
        Path keywordsFile = args.length > 1 ? Path.of(args[1]) : DEFAULT_KEYWORDS_FILE;

        KeywordList keywords = KeywordList.fromFile(keywordsFile);
        List<Target> targets = readTargets(urlsFile);
        System.out.println("關鍵字 " + keywords.size() + " 個（" + keywordsFile + "），網址 " + targets.size() + " 個（" + urlsFile + "）");

        PageFetcher fetcher = new PageFetcher(new PageCache());
        PageScorer scorer = new KeywordPageScorer();
        List<Ranked> ranked = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        for (Target target : targets) {
            System.out.println("抓取：" + target.label);
            try {
                WebPage page = fetcher.fetch(target.url);
                ranked.add(new Ranked(target, scorer.score(page, keywords)));
            } catch (IOException | IllegalArgumentException e) {
                failures.add(target.label + "：" + e.getMessage() + "（" + target.url + "）");
            }
        }
        ranked.sort((a, b) -> Double.compare(b.result.getScore(), a.result.getScore()));

        printRanking(ranked);
        printCounts(ranked);
        printFailures(failures);
        writeCsv(ranked);
    }

    private static List<Target> readTargets(Path file) throws IOException {
        List<Target> targets = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\s+", 2);
            targets.add(new Target(parts[0], parts.length > 1 ? parts[1].strip() : parts[0]));
        }
        return targets;
    }

    private static void printRanking(List<Ranked> ranked) {
        System.out.println();
        System.out.println("===== 排名 =====");
        System.out.println("名次\t分數\t說明\t網址");
        for (int i = 0; i < ranked.size(); i++) {
            Ranked r = ranked.get(i);
            System.out.println((i + 1) + "\t" + String.format("%.1f", r.result.getScore()) + "\t"
                    + r.target.label + "\t" + r.target.url);
        }
    }

    private static void printCounts(List<Ranked> ranked) {
        System.out.println();
        System.out.println("===== 各網頁關鍵字次數（只列出現過的） =====");
        for (int i = 0; i < ranked.size(); i++) {
            Ranked r = ranked.get(i);
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, Integer> e : r.result.getKeywordCounts().entrySet()) {
                if (e.getValue() > 0) {
                    parts.add(e.getKey() + " " + e.getValue());
                }
            }
            System.out.println((i + 1) + ". " + r.target.label + "：" + (parts.isEmpty() ? "（無）" : String.join("、", parts)));
        }
    }

    private static void printFailures(List<String> failures) {
        if (failures.isEmpty()) {
            return;
        }
        System.out.println();
        System.out.println("===== 抓取失敗 =====");
        for (String failure : failures) {
            System.out.println("- " + failure);
        }
    }

    private static void writeCsv(List<Ranked> ranked) throws IOException {
        StringBuilder sb = new StringBuilder("﻿名次,說明,網址,分數"); // BOM，讓 Excel 正確顯示中文
        List<String> names = ranked.isEmpty() ? List.of()
                : new ArrayList<>(ranked.get(0).result.getKeywordCounts().keySet());
        for (String name : names) {
            sb.append(',').append(csv(name));
        }
        sb.append('\n');
        for (int i = 0; i < ranked.size(); i++) {
            Ranked r = ranked.get(i);
            sb.append(i + 1).append(',').append(csv(r.target.label)).append(',').append(csv(r.target.url))
                    .append(',').append(String.format("%.1f", r.result.getScore()));
            for (String name : names) {
                sb.append(',').append(r.result.getCount(name));
            }
            sb.append('\n');
        }
        Files.createDirectories(RESULTS_FILE.getParent());
        Files.writeString(RESULTS_FILE, sb, StandardCharsets.UTF_8);
        System.out.println();
        System.out.println("結果已存到 " + RESULTS_FILE);
    }

    private static String csv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
