package com.group16.ooosearch.cli;

import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.model.PageResult;
import com.group16.ooosearch.tree.SiteCrawler;
import com.group16.ooosearch.tree.WebNode;
import com.group16.ooosearch.tree.WebTree;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class Stage2Main {
    private static final Path DEFAULT_URLS_FILE = Path.of("data", "stage2-urls.txt");
    private static final Path DEFAULT_KEYWORDS_FILE = Path.of("data", "keywords.txt");
    private static final Path RESULTS_FILE = Path.of("data", "results", "stage2-results.csv");

    private static class RootTarget {
        final String url;
        final String label;

        RootTarget(String url, String label) {
            this.url = url;
            this.label = label;
        }
    }

    private static class RankedTree {
        final RootTarget target;
        final WebTree tree;
        final double totalScore;

        RankedTree(RootTarget target, WebTree tree) {
            this.target = target;
            this.tree = tree;
            this.totalScore = (tree != null && tree.getRoot() != null) ? tree.getRoot().getNodeScore() : 0.0;
        }
    }

    public static void main(String[] args) throws IOException {
        Path urlsFile = args.length > 0 ? Path.of(args[0]) : DEFAULT_URLS_FILE;
        Path keywordsFile = args.length > 1 ? Path.of(args[1]) : DEFAULT_KEYWORDS_FILE;

        KeywordList keywords = KeywordList.fromFile(keywordsFile);
        List<RootTarget> targets = readRootTargets(urlsFile);
        System.out.println("關鍵字 " + keywords.size() + " 個（" + keywordsFile + "），根網址 " + targets.size() + " 個（" + urlsFile + "）");

        SiteCrawler crawler = new SiteCrawler(2, 15); // 深度限制 2 層，每棵樹最多 15 個節點
        List<RankedTree> rankedTrees = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        for (RootTarget target : targets) {
            System.out.println("開始爬取與建樹：" + target.label + " (" + target.url + ")");
            try {
                WebTree tree = crawler.crawl(target.url);
                rankedTrees.add(new RankedTree(target, tree));
            } catch (Exception e) {
                failures.add(target.label + "：" + e.getMessage() + "（" + target.url + "）");
            }
        }

        // 依據根節點的累積總分（NodeScore）由高到低排序
        rankedTrees.sort((a, b) -> Double.compare(b.totalScore, a.totalScore));

        printRankings(rankedTrees);
        printFailures(failures);
        writeCsv(rankedTrees);
    }

    private static List<RootTarget> readRootTargets(Path file) throws IOException {
        List<RootTarget> targets = new ArrayList<>();
        if (!Files.exists(file)) {
            // 若無專用 urls 檔，給個預設的示範根網址
            targets.add(new RootTarget("https://zh.wikipedia.org/wiki/%E5%81%87%E9%9D%A2%E9%AA%91%E5%A3%ABOOO", "中文維基：假面騎士OOO"));
            return targets;
        }
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\s+", 2);
            targets.add(new RootTarget(parts[0], parts.length > 1 ? parts[1].strip() : parts[0]));
        }
        return targets;
    }

    private static void printRankings(List<RankedTree> rankedTrees) {
        System.out.println();
        System.out.println("========== 排名與樹狀結構結果 ==========");
        for (int i = 0; i < rankedTrees.size(); i++) {
            RankedTree rt = rankedTrees.get(i);
            System.out.println((i + 1) + ". 說明: " + rt.target.label + " | 網址: " + rt.target.url + " | 累積總分: " + String.format("%.2f", rt.totalScore));
            if (rt.tree != null) {
                rt.tree.printTree(); // 呼叫你們寫好的印樹方法
            }
            System.out.println("-".repeat(40));
        }
    }

    private static void printFailures(List<String> failures) {
        if (failures.isEmpty()) {
            return;
        }
        System.out.println();
        System.out.println("===== 爬取失敗 =====");
        for (String failure : failures) {
            System.out.println("- " + failure);
        }
    }

    private static void writeCsv(List<RankedTree> rankedTrees) throws IOException {
        StringBuilder sb = new StringBuilder("\uFEFF名次,根網址說明,根網址,節點層級,網址,標題,單頁分,累積總分,關鍵字次數\n");

        int rank = 1;
        for (RankedTree rt : rankedTrees) {
            if (rt.tree != null && rt.tree.getRoot() != null) {
                writeNodeCsvRecursive(rt.tree.getRoot(), rank, rt.target, sb);
            }
            rank++;
        }

        Files.createDirectories(RESULTS_FILE.getParent());
        Files.writeString(RESULTS_FILE, sb, StandardCharsets.UTF_8);
        System.out.println();
        System.out.println("Stage 2 結果已成功存到 " + RESULTS_FILE);
    }

    private static void writeNodeCsvRecursive(WebNode node, int rank, RootTarget target, StringBuilder sb) {
        if (node == null || node.getWebPage() == null) return;

        String url = node.getWebPage().getUrl();
        String title = node.getWebPage().getTitle();
        if (title == null || title.trim().isEmpty()) {
            title = url;
        }
        title = title.replace(",", "，").replace("\n", " ");

        double pageScore = (node.getPageResult() != null) ? node.getPageResult().getScore() : 0.0;
        double nodeScore = node.getNodeScore();
        int depth = node.getDepth();

        StringBuilder kwBuilder = new StringBuilder();
        if (node.getPageResult() != null && node.getPageResult().getKeywordCounts() != null) {
            Map<String, Integer> counts = node.getPageResult().getKeywordCounts();
            boolean first = true;
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                if (entry.getValue() > 0) {
                    if (!first) kwBuilder.append("; ");
                    kwBuilder.append(entry.getKey()).append(":").append(entry.getValue());
                    first = false;
                }
            }
        }

        sb.append(rank).append(',')
          .append(csv(target.label)).append(',')
          .append(csv(target.url)).append(',')
          .append(depth).append(',')
          .append(csv(url)).append(',')
          .append(csv(title)).append(',')
          .append(String.format("%.2f", pageScore)).append(',')
          .append(String.format("%.2f", nodeScore)).append(',')
          .append(csv(kwBuilder.toString()))
          .append('\n');

        for (WebNode child : node.getChildren()) {
            writeNodeCsvRecursive(child, rank, target, sb);
        }
    }

    private static String csv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}