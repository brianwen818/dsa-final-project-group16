package com.group16.ooosearch.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.group16.ooosearch.engine.RealSearchEngine;
import com.group16.ooosearch.engine.SearchResponse;
import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.Passage;
import com.group16.ooosearch.tree.SiteResult;
import com.group16.ooosearch.tree.SiteResults;

/**
 * 完整搜尋的命令列 Demo（正式版 SearchEngine）：排名、擴充詞，以及每個網站「為什麼相關」的段落、標籤與圖片。
 *
 * <p>用法：{@code ./mvnw compile exec:java -Dexec.mainClass=com.group16.ooosearch.cli.SearchMain -Dexec.args="貓咪"}
 * <br>段落另存到 data/results/passages.csv（同一個查詢的舊資料會被取代）。
 */
public class SearchMain {
    private static final Path PASSAGES_FILE = Path.of("data", "results", "passages.csv");
    private static final String CSV_HEADER = "查詢,名次,網站,網站總分,段落名次,章節路徑,摘要,標籤,章節連結,圖片網址,圖片來源";
    private static final int SHOWN_SITES = 10;

    public static void main(String[] args) throws IOException {
        String query = args.length > 0 ? String.join(" ", args) : "假面騎士OOO";
        RealSearchEngine engine = RealSearchEngine.createDefault().onProgress(m -> System.out.println("⏳ " + m));
        SearchResponse response = engine.search(query);

        System.out.println("\n==================== 搜尋「" + query + "」 ====================");
        for (RealSearchEngine.ExpansionRound round : engine.getLastRounds()) {
            System.out.println("第 " + round.round() + " 輪擴充：" + describe(round.terms(), round)
                    + "；再搜尋新網址 " + round.newUrls().size() + " 個");
        }
        List<String> rows = new ArrayList<>();
        int rank = 0;
        for (SiteResult site : response.getResults()) {
            if (!SiteResults.isScorable(site) || rank >= SHOWN_SITES) {
                break;
            }
            rank++;
            String title = site.getTree().getRoot().getWebPage().getTitle();
            System.out.printf("%n%d. %s   網站總分 %.2f%n   %s%n", rank, title, site.getTotalScore(), site.getRootUrl());
            int p = 0;
            for (Passage passage : response.getPassages(site.getRootUrl())) {
                p++;
                System.out.println("   ┌ " + (passage.headingPath().isEmpty() ? passage.pageTitle() : passage.headingText())
                        + (passage.tags().isEmpty() ? "" : "   標籤：" + passage.tags()));
                System.out.println("   │ " + passage.text());
                if (passage.hasImage()) {
                    System.out.println("   │ 圖片：" + passage.imageUrl() + "（來源：" + passage.imageSource() + "）");
                }
                System.out.println("   └ " + passage.anchorUrl());
                rows.add(String.join(",", cell(query), String.valueOf(rank), cell(title),
                        String.format("%.2f", site.getTotalScore()), String.valueOf(p), cell(passage.headingText()),
                        cell(passage.text()), cell(String.join("、", passage.tags())), cell(passage.anchorUrl()),
                        cell(passage.imageUrl() == null ? "" : passage.imageUrl()),
                        cell(passage.imageSource() == null ? "" : passage.imageSource())));
            }
        }
        writeRows(query, rows);
        System.out.println("\n段落已存到 " + PASSAGES_FILE);
    }

    private static String describe(List<Keyword> terms, RealSearchEngine.ExpansionRound round) {
        List<String> parts = new ArrayList<>();
        for (Keyword term : terms) {
            parts.add(String.format("%s（權重 %.1f，相關度 %.2f）", term.getName(), term.getWeight(),
                    round.relevance().getOrDefault(term.getName(), 0.0)));
        }
        return String.join("、", parts);
    }

    private static void writeRows(String query, List<String> rows) throws IOException {
        List<String> kept = new ArrayList<>();
        if (Files.exists(PASSAGES_FILE)) {
            List<String> lines = Files.readAllLines(PASSAGES_FILE, StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {
                if (!lines.get(i).isBlank() && !lines.get(i).startsWith(cell(query) + ",")) {
                    kept.add(lines.get(i));
                }
            }
        }
        kept.addAll(rows);
        StringBuilder csv = new StringBuilder("﻿").append(CSV_HEADER).append('\n');
        kept.forEach(line -> csv.append(line).append('\n'));
        Files.createDirectories(PASSAGES_FILE.getParent());
        Files.writeString(PASSAGES_FILE, csv, StandardCharsets.UTF_8);
    }

    static String cell(String value) {
        String v = value.replace("\n", " ");
        if (v.contains(",") || v.contains("\"")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
