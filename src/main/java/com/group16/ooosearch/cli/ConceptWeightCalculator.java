package com.group16.ooosearch.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;
import com.group16.ooosearch.scoring.WordCounter;

/**
 * 依 data/concepts.txt（人工決定的概念與寫法），用日版、台版維基計算每個概念的權重，
 * 輸出 data/keywords.txt 與計算明細。由 KeywordSeedAnalyzer 呼叫。
 *
 * 每個語言：
 *   次數     = 用 WordCounter 合併計數概念的所有寫法（與 Stage 1 正式計分相同的比對規則）
 *   專屬程度 = OOO 頻率 / (OOO 頻率 + 背景作品平均頻率)，頻率 = 次數 / 條目字數
 *   分數     = (1 + ln 次數) × 專屬程度 × 導言加權（任一寫法出現在導言 ×1.2）；次數為 0 時分數為 0
 * 綜合：
 *   s    = (日版分數 / 日版最高分 + 台版分數 / 台版最高分) / 2
 *   權重 = 1 + 4s，取小數一位
 * 排除詞（概念表中以 ! 開頭）權重固定為 0。
 */
class ConceptWeightCalculator {
    private static final double LEAD_BONUS = 1.2;

    /** 一個語言的語料：OOO 條目正文、導言，以及背景作品正文。 */
    static class Corpus {
        final String name;
        final String targetText;
        final String leadText;
        final List<String> backgroundTexts;

        Corpus(String name, String targetText, String leadText, List<String> backgroundTexts) {
            this.name = name;
            this.targetText = targetText;
            this.leadText = leadText;
            this.backgroundTexts = backgroundTexts;
        }
    }

    private static class Concept {
        final String name;
        final List<String> aliases;
        final String basis;
        final boolean excluded;
        final LanguageScore[] scores = new LanguageScore[2];
        double combined;
        double weight;

        Concept(String name, List<String> aliases, String basis, boolean excluded) {
            this.name = name;
            this.aliases = aliases;
            this.basis = basis;
            this.excluded = excluded;
        }
    }

    private static class LanguageScore {
        int count;
        double specificity;
        boolean inLead;
        double score;
        double normalized;
    }

    static void run(Path conceptsFile, Corpus ja, Corpus zh, Path keywordsFile, Path detailFile) throws IOException {
        List<Concept> concepts = readConcepts(conceptsFile);
        KeywordList forCounting = new KeywordList();
        for (Concept c : concepts) {
            forCounting.add(new Keyword(c.name, 1, c.aliases));
        }
        WordCounter counter = new WordCounter(forCounting);

        Corpus[] corpora = { ja, zh };
        for (int lang = 0; lang < corpora.length; lang++) {
            scoreLanguage(concepts, counter, corpora[lang], lang);
        }
        for (Concept c : concepts) {
            if (c.excluded) {
                c.weight = 0;
                continue;
            }
            c.combined = (c.scores[0].normalized + c.scores[1].normalized) / 2;
            c.weight = Math.round((1 + 4 * c.combined) * 10) / 10.0;
        }

        List<Concept> sorted = new ArrayList<>(concepts);
        sorted.sort((a, b) -> Double.compare(b.weight, a.weight));
        writeKeywords(keywordsFile, sorted);
        writeDetail(detailFile, sorted, corpora);
        System.out.println("已依 " + conceptsFile + " 產生 " + keywordsFile + "（" + concepts.size() + " 個關鍵字）");
    }

    private static List<Concept> readConcepts(Path file) throws IOException {
        List<Concept> concepts = new ArrayList<>();
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            boolean excluded = line.startsWith("!");
            String[] columns = (excluded ? line.substring(1) : line).split("\\|", -1);
            if (columns[0].isBlank()) {
                throw new IllegalArgumentException(file + " 第 " + (i + 1) + " 行缺少關鍵字：" + line);
            }
            List<String> aliases = columns.length > 1 ? KeywordList.splitForms(columns[1]) : List.of();
            String basis = columns.length > 2 ? columns[2].strip() : "";
            concepts.add(new Concept(columns[0].strip(), aliases, basis, excluded));
        }
        return concepts;
    }

    private static void scoreLanguage(List<Concept> concepts, WordCounter counter, Corpus corpus, int lang) {
        Map<String, Integer> targetCounts = counter.count(corpus.targetText);
        Map<String, Integer> leadCounts = counter.count(corpus.leadText);
        List<Map<String, Integer>> backgroundCounts = new ArrayList<>();
        for (String text : corpus.backgroundTexts) {
            backgroundCounts.add(counter.count(text));
        }

        double max = 0;
        for (Concept c : concepts) {
            LanguageScore s = new LanguageScore();
            c.scores[lang] = s;
            s.count = targetCounts.get(c.name);
            if (c.excluded || s.count == 0) {
                continue;
            }
            double targetRate = rate(s.count, corpus.targetText);
            double backgroundRate = 0;
            for (int i = 0; i < corpus.backgroundTexts.size(); i++) {
                backgroundRate += rate(backgroundCounts.get(i).get(c.name), corpus.backgroundTexts.get(i))
                        / corpus.backgroundTexts.size();
            }
            s.specificity = targetRate / (targetRate + backgroundRate);
            s.inLead = leadCounts.get(c.name) > 0;
            s.score = (1 + Math.log(s.count)) * s.specificity * (s.inLead ? LEAD_BONUS : 1.0);
            max = Math.max(max, s.score);
        }
        for (Concept c : concepts) {
            c.scores[lang].normalized = max > 0 ? c.scores[lang].score / max : 0;
        }
    }

    /** 每萬字出現幾次。 */
    private static double rate(int count, String text) {
        return count * 10000.0 / Math.max(1, text.length());
    }

    private static void writeKeywords(Path file, List<Concept> concepts) throws IOException {
        StringBuilder sb = new StringBuilder()
                .append("# 關鍵字表：由 cli/KeywordSeedAnalyzer 依 data/concepts.txt 於 ").append(LocalDate.now())
                .append(" 自動產生，請勿手動修改\n")
                .append("# 要增減關鍵字或寫法，請修改 data/concepts.txt 後重新執行；權重計算明細見 data/keyword-candidates/concept-weights.md\n")
                .append("# 格式：權重 | 關鍵字 | 其他寫法（以逗號分隔）；權重 0 為排除詞\n");
        for (Concept c : concepts) {
            sb.append(String.format("%.1f", c.weight)).append(" | ").append(c.name).append(" | ")
                    .append(String.join(", ", c.aliases)).append('\n');
        }
        Files.writeString(file, sb, StandardCharsets.UTF_8);
    }

    private static void writeDetail(Path file, List<Concept> concepts, Corpus[] corpora) throws IOException {
        StringBuilder sb = new StringBuilder()
                .append("# 關鍵字權重計算明細\n\n")
                .append("由 `cli/KeywordSeedAnalyzer` 依 `data/concepts.txt` 於 ").append(LocalDate.now()).append(" 產生。\n\n")
                .append("- 次數：合併計數概念的所有寫法（長的寫法優先、不重複計算，與 Stage 1 計分相同）\n")
                .append("- 專屬程度 = OOO 頻率 / (OOO 頻率 + 背景作品平均頻率)\n")
                .append("- 分數 = (1 + ln 次數) × 專屬程度 × 導言加權（×").append(LEAD_BONUS).append("）\n")
                .append("- 正規化 = 分數 / 該語言最高分；s = 日版與台版正規化分數的平均\n")
                .append("- **權重 = 1 + 4s**（取小數一位）；排除詞權重為 0\n\n")
                .append("| 關鍵字 | 權重 | s |");
        for (Corpus corpus : corpora) {
            sb.append(' ').append(corpus.name).append("次數 | ").append(corpus.name).append("專屬程度 | ")
                    .append(corpus.name).append("分數 | ").append(corpus.name).append("正規化 |");
        }
        sb.append(" 依據 |\n|---|---|---|---|---|---|---|---|---|---|---|---|\n");
        for (Concept c : concepts) {
            sb.append("| ").append(c.excluded ? "~~" + c.name + "~~（排除詞）" : c.name)
                    .append(" | ").append(String.format("%.1f", c.weight))
                    .append(" | ").append(c.excluded ? "" : String.format("%.2f", c.combined)).append(" |");
            for (LanguageScore s : c.scores) {
                sb.append(' ').append(s.count)
                        .append(" | ").append(s.count == 0 || c.excluded ? "" : String.format("%.2f", s.specificity))
                        .append(" | ").append(c.excluded ? "" : String.format("%.2f", s.score) + (s.inLead ? "（導言）" : ""))
                        .append(" | ").append(c.excluded ? "" : String.format("%.2f", s.normalized)).append(" |");
            }
            sb.append(' ').append(c.basis.replace("|", "\\|")).append(" |\n");
        }
        Files.writeString(file, sb, StandardCharsets.UTF_8);
    }
}
