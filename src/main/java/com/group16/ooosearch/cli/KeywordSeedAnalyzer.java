package com.group16.ooosearch.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.group16.ooosearch.fetch.MediaWikiClient;
import com.group16.ooosearch.fetch.PageCache;
import com.group16.ooosearch.fetch.PageFetcher;
import com.group16.ooosearch.model.WebPage;
import com.group16.ooosearch.scoring.TextNormalizer;

/**
 * 離線工具：從日版與台版維基百科的「假面騎士OOO」條目產生候選關鍵字清單，供人工審核後寫入 data/keywords.txt。
 *
 * 做法：
 * 1. 候選詞：條目名稱，以及兩篇條目中的章節標題、粗體字、定義清單（dt），
 *    還有以「名稱（含假名的原名）」開頭的段落（台版角色列表的寫法，例如「烏霸（ウヴァ）」）。
 *    編者會用這些格式標示人物、名詞。
 * 2. 分數：以其他 6 部假面騎士作品的條目當背景，比較詞在 OOO 條目與背景條目中的出現頻率。
 *       頻率     = 出現次數 / 條目字數
 *       專屬程度 = OOO 頻率 / (OOO 頻率 + 背景平均頻率)，介於 0～1
 *       分數     = (1 + ln 出現次數) × 專屬程度 × 導言加權（出現在第一個章節之前 ×1.2）
 *    每部作品都常出現的詞（如「変身」）專屬程度低；OOO 專屬的詞（如「コアメダル」）接近 1。
 *    不使用一般的 IDF（只看「有沒有出現」），因為其他作品的條目常因客串電影提到 OOO 的核心詞，
 *    IDF 會把「在 W 條目被提到 2 次」與「在 OOO 條目出現 100 次」視為相同，反而壓低核心詞。
 * 3. 日中對照：台版條目常在括號中附上日文原名，例如「鴻上基金會（鴻上ファウンデーション）」，
 *    用它建立日文 → 中文的對應；名稱相同的漢字詞（如「火野映司」）直接在另一版計數。
 *
 * 4. 權重：若 data/concepts.txt（人工決定的概念與寫法）存在，用 ConceptWeightCalculator 計算每個概念的權重，
 *    產生 data/keywords.txt。
 *
 * 輸出到 data/keyword-candidates/：ja-candidates.csv、zh-candidates.csv、summary.md、concept-weights.md，
 * 以及 data/keywords.txt。
 * 執行：mvnw compile exec:java -Dexec.mainClass=com.group16.ooosearch.cli.KeywordSeedAnalyzer
 */
public class KeywordSeedAnalyzer {
    private static final String JA_TARGET = "仮面ライダーオーズ/OOO";
    private static final String ZH_TARGET = "假面騎士OOO";
    private static final String ZH_VARIANT = "zh-tw";
    /**
     * 台版把角色介紹拆成獨立條目（例如「假面騎士OOO角色列表」），日版則寫在主條目內。
     * 為了讓兩版內容範圍一致，台版的 OOO 與背景作品都會加入「標題 + 角色列表」（不存在就略過）。
     */
    private static final String ZH_CHARACTER_LIST_SUFFIX = "角色列表";
    /** 背景作品（日版標題），台版標題透過跨語言連結自動查詢。 */
    private static final List<String> JA_BACKGROUND = List.of(
            "仮面ライダー電王", "仮面ライダーディケイド", "仮面ライダーW",
            "仮面ライダーフォーゼ", "仮面ライダーウィザード", "仮面ライダービルド");

    private static final Path OUTPUT_DIR = Path.of("data", "keyword-candidates");
    private static final Path CONCEPTS_FILE = Path.of("data", "concepts.txt");
    private static final Path KEYWORDS_FILE = Path.of("data", "keywords.txt");
    /** 出現次數少於此值的候選詞不列入。 */
    private static final int MIN_COUNT = 3;
    private static final double LEAD_BONUS = 1.2;
    private static final int SUMMARY_ROWS = 40;

    /** 這些章節（含之後的所有內容）是註腳、參考資料，不列入分析。 */
    private static final Set<String> END_SECTIONS = Set.of(
            "脚注", "注釈", "出典", "参考文献", "外部リンク", "関連項目",
            "參考資料", "參考文獻", "參考來源", "注釋", "註釋", "腳註", "外部連結", "參見", "相關條目");
    /** 導航框、維護模板、參考文獻標記等雜訊。 */
    private static final String NOISE = ".navbox, .navbox-styles, .ambox, .metadata, .hatnote, .noprint, "
            + ".mw-editsection, sup.reference, .reflist, .references, .mw-references-wrap, style, script";

    private static final Pattern PARENTHESES = Pattern.compile("\\(([^()]*)\\)");
    /** 段落以「名稱（含假名的原名）」開頭，例如「烏霸（ウヴァ）（山田悠介飾…」。 */
    private static final Pattern NAMED_PARAGRAPH = Pattern.compile(
            "^([^()，。、,:：]{2,15})\\(([^()]*[\\p{IsHiragana}\\p{IsKatakana}][^()]*)\\)");
    private static final Pattern NOT_A_TERM = Pattern.compile("^[\\d\\s年月日第作話集回期\\p{Punct}]+$");

    public static void main(String[] args) throws IOException {
        PageFetcher fetcher = new PageFetcher(new PageCache());
        MediaWikiClient ja = new MediaWikiClient("ja.wikipedia.org", fetcher);
        MediaWikiClient zh = new MediaWikiClient("zh.wikipedia.org", fetcher);

        System.out.println("抓取日版條目…");
        Article jaTarget = Article.of(ja.parse(JA_TARGET, null));
        List<Article> jaBackground = new ArrayList<>();
        for (String title : JA_BACKGROUND) {
            jaBackground.add(Article.of(ja.parse(title, null)));
        }

        System.out.println("抓取台版條目…");
        Article zhTarget = zhArticleWithCharacters(zh, ZH_TARGET);
        Map<String, String> zhTitles = ja.langLinks(JA_BACKGROUND, "zh");
        List<Article> zhBackground = new ArrayList<>();
        for (String jaTitle : JA_BACKGROUND) {
            String zhTitle = zhTitles.get(jaTitle);
            if (zhTitle == null) {
                System.out.println("  找不到「" + jaTitle + "」的中文條目，略過");
                continue;
            }
            zhBackground.add(zhArticleWithCharacters(zh, zhTitle));
        }

        List<Candidate> jaCandidates = score(jaTarget, jaBackground);
        List<Candidate> zhCandidates = score(zhTarget, zhBackground);
        linkLanguages(jaCandidates, zhCandidates, jaTarget, zhTarget);

        Files.createDirectories(OUTPUT_DIR);
        writeCsv(OUTPUT_DIR.resolve("ja-candidates.csv"), jaCandidates, "台版對應詞", "台版出現次數");
        writeCsv(OUTPUT_DIR.resolve("zh-candidates.csv"), zhCandidates, "日版對應詞", "日版出現次數");
        writeSummary(OUTPUT_DIR.resolve("summary.md"), jaTarget, jaBackground, jaCandidates,
                zhTarget, zhBackground, zhCandidates);

        System.out.println("日版候選詞 " + jaCandidates.size() + " 個、台版候選詞 " + zhCandidates.size() + " 個");
        System.out.println("已輸出到 " + OUTPUT_DIR.toAbsolutePath());

        if (Files.exists(CONCEPTS_FILE)) {
            ConceptWeightCalculator.run(CONCEPTS_FILE, corpus("日版", jaTarget, jaBackground),
                    corpus("台版", zhTarget, zhBackground), KEYWORDS_FILE, OUTPUT_DIR.resolve("concept-weights.md"));
        } else {
            System.out.println("找不到 " + CONCEPTS_FILE + "，略過權重計算");
        }
    }

    private static ConceptWeightCalculator.Corpus corpus(String name, Article target, List<Article> background) {
        List<String> backgroundTexts = new ArrayList<>();
        for (Article a : background) {
            backgroundTexts.add(a.text);
        }
        return new ConceptWeightCalculator.Corpus(name, target.text, target.lead, backgroundTexts);
    }

    // ---------- 條目處理 ----------

    /** 台版條目加上它的角色列表條目；角色列表不存在時只回傳主條目。 */
    private static Article zhArticleWithCharacters(MediaWikiClient zh, String title) throws IOException {
        Article main = Article.of(zh.parse(title, ZH_VARIANT));
        try {
            return main.combine(Article.of(zh.parse(title + ZH_CHARACTER_LIST_SUFFIX, ZH_VARIANT)));
        } catch (IOException e) {
            System.out.println("  「" + title + "」沒有角色列表條目，只使用主條目");
            return main;
        }
    }

    /** 清理後的條目：正文、導言、候選詞。 */
    private static class Article {
        /** 主條目（導言、連結以它為準）。 */
        final WebPage page;
        /** 合併進來的所有條目，包含主條目。 */
        final List<WebPage> pages = new ArrayList<>();
        final String text;
        final String lead;
        /** key 為小寫的候選詞，讓只差大小寫的寫法合併。 */
        final Map<String, Candidate> candidates = new LinkedHashMap<>();

        private Article(WebPage page, String text, String lead) {
            this.page = page;
            this.pages.add(page);
            this.text = text;
            this.lead = lead;
        }

        /** 把另一個條目的正文與候選詞併入（導言仍使用主條目的）。 */
        Article combine(Article other) {
            Article combined = new Article(page, text + " " + other.text, lead);
            combined.pages.addAll(other.pages);
            for (Article source : List.of(this, other)) {
                for (Map.Entry<String, Candidate> entry : source.candidates.entrySet()) {
                    Candidate c = combined.candidates.computeIfAbsent(entry.getKey(),
                            k -> new Candidate(entry.getValue().term));
                    c.sources.addAll(entry.getValue().sources);
                    c.aliases.addAll(entry.getValue().aliases);
                }
            }
            return combined;
        }

        static Article of(WebPage page) {
            Document doc = Jsoup.parse(page.getHtml());
            Element root = doc.selectFirst(".mw-parser-output");
            if (root == null) {
                root = doc.body();
            }
            root.select(NOISE).remove();
            removeEndSections(root);

            StringBuilder lead = new StringBuilder();
            for (Element child : root.children()) {
                if (isHeading(child)) {
                    break;
                }
                lead.append(child.text()).append(' ');
            }

            Article article = new Article(page, normalize(root.text()), normalize(lead.toString()));
            article.addCandidates(page.getTitle(), "條目名稱");
            for (Element e : root.select("h2, h3, h4")) {
                article.addCandidates(e.text(), "標題");
            }
            for (Element e : root.select("dt")) {
                article.addCandidates(e.text(), "dt");
            }
            for (Element e : root.select("b")) {
                article.addCandidates(e.text(), "粗體");
            }
            for (Element e : root.select("p, li, dd")) {
                Matcher m = NAMED_PARAGRAPH.matcher(normalize(e.text()));
                if (m.find()) {
                    article.addCandidate(m.group(1).strip(), List.of(m.group(2).split("\\s*[/／,，]\\s*")), "段落開頭");
                }
            }
            return article;
        }

        /** 「火野 映司（ひの えいじ） / 仮面ライダーオーズ」→ 候選詞「火野映司」「仮面ライダーオーズ」，括號內為別名。 */
        private void addCandidates(String raw, String source) {
            for (String part : normalize(raw).split("\\s*[/／、,，]\\s*")) {
                List<String> aliases = new ArrayList<>();
                Matcher m = PARENTHESES.matcher(part);
                while (m.find()) {
                    aliases.add(m.group(1).strip());
                }
                String term = PARENTHESES.matcher(part).replaceAll("").replaceAll("[:：]$", "").strip();
                addCandidate(term, aliases, source);
            }
        }

        private void addCandidate(String term, List<String> aliases, String source) {
            if (!isTerm(term)) {
                return;
            }
            Candidate c = candidates.computeIfAbsent(term.toLowerCase(Locale.ROOT), k -> new Candidate(term));
            c.sources.add(source);
            for (String alias : aliases) {
                String a = alias.strip();
                if (isTerm(a) && !a.equalsIgnoreCase(term)) {
                    c.aliases.add(a);
                }
            }
        }
    }

    private static boolean isHeading(Element e) {
        return e.is("h2") || e.hasClass("mw-heading2");
    }

    private static void removeEndSections(Element root) {
        boolean removing = false;
        for (Element child : new ArrayList<>(root.children())) {
            if (!removing && isHeading(child) && END_SECTIONS.contains(child.text().strip())) {
                removing = true;
            }
            if (removing) {
                child.remove();
            }
        }
    }

    /** 全形轉半形、移除漢字與假名之間的空白（「火野 映司」→「火野映司」）、合併多餘空白。 */
    static String normalize(String s) {
        return TextNormalizer.normalize(s);
    }

    private static boolean isTerm(String term) {
        return term.length() >= 2 && term.length() <= 25
                && !term.contains("。")
                && !NOT_A_TERM.matcher(term).matches();
    }

    /** 不重疊地計算 term 在 text 中出現的次數（忽略大小寫）。 */
    static int count(String text, String term) {
        if (term == null || term.isEmpty()) {
            return 0;
        }
        String t = text.toLowerCase(Locale.ROOT);
        String k = term.toLowerCase(Locale.ROOT);
        int count = 0;
        for (int i = t.indexOf(k); i >= 0; i = t.indexOf(k, i + k.length())) {
            count++;
        }
        return count;
    }

    // ---------- 計分 ----------

    private static class Candidate {
        final String term;
        final Set<String> sources = new LinkedHashSet<>();
        final Set<String> aliases = new LinkedHashSet<>();
        int count;
        int documentFrequency;
        int totalDocuments;
        double specificity;
        boolean inLead;
        double score;
        int suggestedWeight;
        String otherLanguageTerm = "";
        int otherLanguageCount;

        Candidate(String term) {
            this.term = term;
        }
    }

    private static List<Candidate> score(Article target, List<Article> background) {
        int n = background.size() + 1;
        List<Candidate> result = new ArrayList<>();
        for (Candidate c : target.candidates.values()) {
            c.count = count(target.text, c.term);
            if (c.count < MIN_COUNT) {
                continue;
            }
            double targetRate = rate(c.count, target);
            double backgroundRate = 0;
            c.documentFrequency = 1;
            for (Article other : background) {
                int otherCount = count(other.text, c.term);
                if (otherCount > 0) {
                    c.documentFrequency++;
                }
                backgroundRate += rate(otherCount, other) / background.size();
            }
            c.totalDocuments = n;
            c.specificity = targetRate / (targetRate + backgroundRate);
            c.inLead = count(target.lead, c.term) > 0;
            c.score = (1 + Math.log(c.count)) * c.specificity * (c.inLead ? LEAD_BONUS : 1.0);
            result.add(c);
        }
        result.sort((a, b) -> Double.compare(b.score, a.score));
        for (int i = 0; i < result.size(); i++) {
            result.get(i).suggestedWeight = suggestedWeight(i + 1);
        }
        return result;
    }

    /** 每萬字出現幾次。 */
    private static double rate(int count, Article article) {
        return count * 10000.0 / Math.max(1, article.text.length());
    }

    /** 依名次換算成 1～5 的建議權重，只是起點，最後由人工決定。 */
    private static int suggestedWeight(int rank) {
        if (rank <= 3) {
            return 5;
        } else if (rank <= 10) {
            return 4;
        } else if (rank <= 25) {
            return 3;
        } else if (rank <= 50) {
            return 2;
        }
        return 1;
    }

    // ---------- 日中對照 ----------

    private static void linkLanguages(List<Candidate> jaCandidates, List<Candidate> zhCandidates,
            Article jaTarget, Article zhTarget) {
        // 台版括號中的日文原名 → 台版譯名。使用全部候選詞（含出現次數不足而未列入清單的），
        // 因為台版常只在段落開頭寫一次中文譯名，內文改用英文（例如「烏霸（ウヴァ）」，內文寫 Uva）
        Map<String, String> zhNameOf = new LinkedHashMap<>();
        for (Candidate zh : zhTarget.candidates.values()) {
            for (String alias : zh.aliases) {
                zhNameOf.putIfAbsent(alias, zh.term);
            }
        }
        for (Candidate ja : jaCandidates) {
            String zhTerm = zhNameOf.getOrDefault(ja.term, ja.term);
            int zhCount = count(zhTarget.text, zhTerm);
            if (zhCount > 0) {
                ja.otherLanguageTerm = zhTerm;
                ja.otherLanguageCount = zhCount;
            }
        }
        // 台版詞本身或其括號中的寫法，在日版出現最多次的那一個
        for (Candidate zh : zhCandidates) {
            List<String> forms = new ArrayList<>(zh.aliases);
            forms.add(0, zh.term);
            for (String form : forms) {
                int jaCount = count(jaTarget.text, form);
                if (jaCount > zh.otherLanguageCount) {
                    zh.otherLanguageTerm = form;
                    zh.otherLanguageCount = jaCount;
                }
            }
        }
    }

    // ---------- 輸出 ----------

    private static void writeCsv(Path file, List<Candidate> candidates, String otherTermHeader,
            String otherCountHeader) throws IOException {
        StringBuilder sb = new StringBuilder("\uFEFF"); // BOM，讓 Excel 正確顯示中文
        sb.append("名次,候選詞,建議權重,分數,出現次數,專屬程度,出現篇數/總篇數,出現在導言,來源,括號內寫法,")
                .append(otherTermHeader).append(',').append(otherCountHeader).append('\n');
        for (int i = 0; i < candidates.size(); i++) {
            Candidate c = candidates.get(i);
            sb.append(i + 1).append(',')
                    .append(csv(c.term)).append(',')
                    .append(c.suggestedWeight).append(',')
                    .append(String.format("%.2f", c.score)).append(',')
                    .append(c.count).append(',')
                    .append(String.format("%.2f", c.specificity)).append(',')
                    .append(c.documentFrequency).append('/').append(c.totalDocuments).append(',')
                    .append(c.inLead ? "是" : "").append(',')
                    .append(csv(String.join(" ", c.sources))).append(',')
                    .append(csv(String.join(" / ", c.aliases))).append(',')
                    .append(csv(c.otherLanguageTerm)).append(',')
                    .append(c.otherLanguageCount == 0 ? "" : c.otherLanguageCount).append('\n');
        }
        Files.writeString(file, sb, StandardCharsets.UTF_8);
    }

    private static String csv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static void writeSummary(Path file, Article jaTarget, List<Article> jaBackground,
            List<Candidate> jaCandidates, Article zhTarget, List<Article> zhBackground,
            List<Candidate> zhCandidates) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# 候選關鍵字清單（待審核）\n\n")
                .append("由 `cli/KeywordSeedAnalyzer` 於 ").append(LocalDate.now()).append(" 產生。")
                .append("完整清單見同資料夾的 `ja-candidates.csv`、`zh-candidates.csv`。\n\n")
                .append("## 資料來源\n\n")
                .append("- 日版：[").append(jaTarget.page.getTitle()).append("](").append(jaTarget.page.getUrl()).append(")\n")
                .append("- 台版（`variant=").append(ZH_VARIANT).append("`）：").append(links(zhTarget.pages)).append('\n')
                .append("- 日版背景作品：").append(titles(jaBackground)).append('\n')
                .append("- 台版背景作品：").append(titles(zhBackground)).append("\n\n")
                .append("## 計分方式\n\n")
                .append("- 候選詞：條目名稱、章節標題、粗體字、定義清單（dt），以及以「名稱（含假名的原名）」開頭的段落，出現次數至少 ")
                .append(MIN_COUNT).append(" 次\n")
                .append("- 台版的 OOO 與背景作品都加入各自的「角色列表」條目（不存在則略過），因為台版把角色介紹拆成獨立條目\n")
                .append("- 頻率 = 出現次數 / 條目字數；專屬程度 = OOO 頻率 / (OOO 頻率 + 背景作品平均頻率)，介於 0～1\n")
                .append("- 分數 = (1 + ln 出現次數) × 專屬程度 × 導言加權（出現在導言 ×").append(LEAD_BONUS).append("）\n")
                .append("- 每部作品都常出現的詞（如「変身」）專屬程度低，OOO 專屬的詞接近 1。")
                .append("「出現篇數」為 OOO 加背景作品共 7 篇中有出現的篇數，僅供參考\n")
                .append("- 建議權重依名次換算：1～3 名 5、4～10 名 4、11～25 名 3、26～50 名 2、其餘 1\n\n");
        appendTable(sb, "日版前 " + SUMMARY_ROWS + " 名", jaCandidates, "台版對應");
        appendTable(sb, "台版前 " + SUMMARY_ROWS + " 名", zhCandidates, "日版對應");
        Files.writeString(file, sb, StandardCharsets.UTF_8);
    }

    /** 每個條目以「主條目＋角色列表」顯示。 */
    private static String titles(List<Article> articles) {
        List<String> titles = new ArrayList<>();
        for (Article a : articles) {
            List<String> pageTitles = new ArrayList<>();
            for (WebPage page : a.pages) {
                pageTitles.add(page.getTitle());
            }
            titles.add(String.join("＋", pageTitles));
        }
        return String.join("、", titles);
    }

    private static String links(List<WebPage> pages) {
        List<String> links = new ArrayList<>();
        for (WebPage page : pages) {
            links.add("[" + page.getTitle() + "](" + page.getUrl() + ")");
        }
        return String.join("、", links);
    }

    private static void appendTable(StringBuilder sb, String heading, List<Candidate> candidates, String otherHeader) {
        sb.append("## ").append(heading).append("\n\n")
                .append("| 名次 | 候選詞 | 建議權重 | 分數 | 次數 | 專屬程度 | 出現篇數 | 導言 | 括號內寫法 | ").append(otherHeader).append(" |\n")
                .append("|---|---|---|---|---|---|---|---|---|---|\n");
        for (int i = 0; i < Math.min(SUMMARY_ROWS, candidates.size()); i++) {
            Candidate c = candidates.get(i);
            sb.append("| ").append(i + 1)
                    .append(" | ").append(md(c.term))
                    .append(" | ").append(c.suggestedWeight)
                    .append(" | ").append(String.format("%.2f", c.score))
                    .append(" | ").append(c.count)
                    .append(" | ").append(String.format("%.2f", c.specificity))
                    .append(" | ").append(c.documentFrequency).append('/').append(c.totalDocuments)
                    .append(" | ").append(c.inLead ? "✓" : "")
                    .append(" | ").append(md(String.join(" / ", c.aliases)))
                    .append(" | ").append(c.otherLanguageCount == 0 ? ""
                            : md(c.otherLanguageTerm) + "（" + c.otherLanguageCount + "）")
                    .append(" |\n");
        }
        sb.append('\n');
    }

    private static String md(String value) {
        return value.replace("|", "\\|");
    }
}
