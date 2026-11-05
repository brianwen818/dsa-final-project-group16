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
import com.group16.ooosearch.tree.SiteResult;
import com.group16.ooosearch.tree.SiteResults;

/**
 * Stage 4 命令列 Demo：語意分析與關鍵字擴充（2026-11-05 修正版）。
 *
 * <p>透過正式版搜尋引擎執行一次完整搜尋：從前幾名網站的相關段落斷詞、建共現圖找擴充詞，
 * 用新擴充詞再搜尋，並以「OOO 關鍵字 + 擴充詞」重新排名，最多 2 輪。
 * 印出每一輪的擴充詞（權重、相關度）與最終排名；結果存到 data/results/stage4-results.csv。
 *
 * <p>用法：Stage4Main [查詢字串]，預設查詢為「假面騎士OOO」。
 */
public class Stage4Main {
    private static final String DEFAULT_QUERY = "假面騎士OOO";
    private static final Path RESULTS_FILE = Path.of("data", "results", "stage4-results.csv");
    private static final String CSV_HEADER = "查詢,輪次,擴充詞,相關度,權重,再搜尋新網址數";

    public static void main(String[] args) throws IOException {
        String query = args.length > 0 ? String.join(" ", args) : DEFAULT_QUERY;

        System.out.println("==================================================");
        System.out.println("    Stage 4: 語意分析與自動關鍵字擴充系統");
        System.out.println("==================================================\n");

        RealSearchEngine engine = RealSearchEngine.createDefault().onProgress(m -> System.out.println("⏳ " + m));
        SearchResponse response = engine.search(query);

        List<String> rows = new ArrayList<>();
        System.out.println("\n==================================================");
        System.out.println("               關鍵字擴充結果（查詢：" + query + "）");
        System.out.println("==================================================");
        if (engine.getLastRounds().isEmpty()) {
            System.out.println("  （無符合門檻的新擴充詞）");
        }
        for (RealSearchEngine.ExpansionRound round : engine.getLastRounds()) {
            System.out.println("第 " + round.round() + " 輪（再搜尋找到 " + round.newUrls().size() + " 個新網址）：");
            for (Keyword term : round.terms()) {
                double rel = round.relevance().getOrDefault(term.getName(), 0.0);
                System.out.printf("  %s  權重 %.1f  相關度 %.2f%n", term.getName(), term.getWeight(), rel);
                rows.add(String.join(",", SearchMain.cell(query), String.valueOf(round.round()),
                        SearchMain.cell(term.getName()), String.format("%.2f", rel),
                        String.format("%.1f", term.getWeight()), String.valueOf(round.newUrls().size())));
            }
        }

        System.out.println("\n最終排名（前 5 名）：");
        int rank = 0;
        for (SiteResult site : response.getResults()) {
            if (!SiteResults.isScorable(site) || rank >= 5) {
                break;
            }
            System.out.printf("  %d. %s（%.2f）%n", ++rank, site.getTree().getRoot().getWebPage().getTitle(),
                    site.getTotalScore());
        }

        writeRows(query, rows);
        System.out.println("\n🎉 Stage 4 執行完成！擴充結果已存到 " + RESULTS_FILE);
    }

    private static void writeRows(String query, List<String> rows) throws IOException {
        List<String> kept = new ArrayList<>();
        if (Files.exists(RESULTS_FILE)) {
            List<String> lines = Files.readAllLines(RESULTS_FILE, StandardCharsets.UTF_8);
            // 舊格式（4 欄）的檔案整個換掉
            if (!lines.isEmpty() && lines.get(0).contains("輪次")) {
                for (int i = 1; i < lines.size(); i++) {
                    if (!lines.get(i).isBlank() && !lines.get(i).startsWith(SearchMain.cell(query) + ",")) {
                        kept.add(lines.get(i));
                    }
                }
            }
        }
        kept.addAll(rows);
        StringBuilder csv = new StringBuilder("﻿").append(CSV_HEADER).append('\n');
        kept.forEach(line -> csv.append(line).append('\n'));
        Files.createDirectories(RESULTS_FILE.getParent());
        Files.writeString(RESULTS_FILE, csv, StandardCharsets.UTF_8);
    }
}
