package com.group16.ooosearch.snippet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.group16.ooosearch.model.WebPage;
import com.group16.ooosearch.tree.SiteCrawler;

/**
 * 把網頁正文切成段落（段落摘要的第 1 步）。
 *
 * <p>依文件順序走訪標題、段落（p）、清單項目（li）、表格列（tr）與定義清單（dt、dd）：
 * <ul>
 *   <li>用 <b>Stack</b> 記錄目前所在的章節：遇到 h2 時，把 Stack 中 h2 以下（含同級）的標題都 pop 掉再 push，
 *       所以 Stack 由底到頂就是「主要角色 › 貪婪 › 卡佳里」這樣的章節路徑（HW5 標籤配對的延伸）。</li>
 *   <li>同時記下章節的錨點（id），讓結果頁能直接跳到該章節；相容 MediaWiki 新舊兩種標題 HTML。</li>
 *   <li>章節中第一張夠大的圖片（寬度 ≥ 100）記在該章節上；段落本身含大圖時優先用段落的圖。</li>
 * </ul>
 */
public class PassageExtractor {
    /** 太短的段落（例如表格的「是／否」）沒有說明效果，略過。 */
    public static final int MIN_LENGTH = 8;
    public static final int MIN_IMAGE_WIDTH = 100;

    private static final String REMOVED = SiteCrawler.NON_CONTENT_BLOCKS
            + ", script, style, noscript, .toc, #toc, .mw-references-wrap, sup.reference, .mw-cite-backlink";
    private static final String UNITS = "h1, h2, h3, h4, h5, h6, p, li, tr, dt, dd, img";

    /** 一個段落：文字、章節路徑、最近的章節錨點、段落自己的圖片、所在章節的圖片。 */
    public record Unit(String text, List<String> headingPath, String anchorId, String ownImage, String sectionImage) {
        public String image() {
            return ownImage != null ? ownImage : sectionImage;
        }
    }

    /** Stack 中的一個章節。 */
    private static final class Heading {
        final int level;
        final String text;
        final String id;
        String image;

        Heading(int level, String text, String id) {
            this.level = level;
            this.text = text;
            this.id = id;
        }
    }

    public List<Unit> extract(WebPage page) {
        Document doc = Jsoup.parse(page.getHtml(), page.getUrl());
        doc.select(REMOVED).remove();

        Deque<Heading> stack = new ArrayDeque<>(); // 當作 Stack 使用：push、pop、peek
        List<Unit> units = new ArrayList<>();
        for (Element el : doc.select(UNITS)) {
            String tag = el.normalName();
            if (tag.length() == 2 && tag.charAt(0) == 'h' && Character.isDigit(tag.charAt(1))) {
                int level = tag.charAt(1) - '0';
                while (!stack.isEmpty() && stack.peek().level >= level) {
                    stack.pop();
                }
                String text = el.text().replace("[編輯]", "").replace("[编辑]", "").replace("[编辑源代码]", "").strip();
                if (!text.isEmpty()) {
                    stack.push(new Heading(level, text, headingId(el)));
                }
                continue;
            }
            if (tag.equals("img")) {
                if (!stack.isEmpty() && stack.peek().image == null && isLargeImage(el)) {
                    stack.peek().image = imageUrl(el);
                }
                continue;
            }
            if (isNestedContainer(el, tag)) {
                continue;
            }
            String text = (tag.equals("tr") ? rowText(el) : el.text()).strip();
            if (text.length() < MIN_LENGTH) {
                continue;
            }
            units.add(new Unit(text, path(stack), anchorId(stack), firstLargeImage(el),
                    stack.isEmpty() ? null : stack.peek().image));
        }
        return units;
    }

    /** 外層清單項目（內含子清單）與放在 li、tr、dd 裡的段落，會和內層重複，略過。 */
    private static boolean isNestedContainer(Element el, String tag) {
        if (tag.equals("li") && el.selectFirst("ul, ol") != null) {
            return true;
        }
        if (tag.equals("tr") && el.selectFirst("table") != null) {
            return true;
        }
        if (tag.equals("p") || tag.equals("dd") || tag.equals("dt")) {
            for (Element parent : el.parents()) {
                String name = parent.normalName();
                if (name.equals("li") || name.equals("tr") || (name.equals("dd") && !tag.equals("dd"))) {
                    return true;
                }
            }
        }
        if (tag.equals("li")) {
            for (Element parent : el.parents()) {
                if (parent.normalName().equals("tr")) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 表格列：各儲存格以空白分開。 */
    private static String rowText(Element tr) {
        List<String> cells = new ArrayList<>();
        for (Element cell : tr.select("> th, > td")) {
            String t = cell.text().strip();
            if (!t.isEmpty()) {
                cells.add(t);
            }
        }
        return String.join(" ", cells);
    }

    /** 新版 MediaWiki：&lt;h2 id="..."&gt;；舊版：&lt;h2&gt;&lt;span class="mw-headline" id="..."&gt;。 */
    private static String headingId(Element heading) {
        if (!heading.id().isEmpty()) {
            return heading.id();
        }
        Element span = heading.selectFirst("span.mw-headline[id], span[id]");
        return span == null ? null : span.id();
    }

    private static List<String> path(Deque<Heading> stack) {
        List<String> path = new ArrayList<>();
        Iterator<Heading> it = stack.descendingIterator(); // 由底（最外層）到頂（最內層）
        while (it.hasNext()) {
            path.add(it.next().text);
        }
        return path;
    }

    private static String anchorId(Deque<Heading> stack) {
        for (Heading heading : stack) { // 由頂往底找最近有 id 的章節
            if (heading.id != null && !heading.id.isEmpty()) {
                return heading.id;
            }
        }
        return null;
    }

    private static String firstLargeImage(Element el) {
        for (Element img : el.select("img")) {
            if (isLargeImage(img)) {
                return imageUrl(img);
            }
        }
        return null;
    }

    static boolean isLargeImage(Element img) {
        String url = imageUrl(img);
        if (url == null || url.endsWith(".svg") || url.contains(".svg/")) {
            return false;
        }
        String width = img.hasAttr("width") ? img.attr("width") : img.attr("data-file-width");
        try {
            return width.isEmpty() || Integer.parseInt(width.replace("px", "").strip()) >= MIN_IMAGE_WIDTH;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Fandom 會延遲載入圖片，真正的網址在 data-src。 */
    private static String imageUrl(Element img) {
        String src = img.absUrl("src");
        if (src.isEmpty() || src.startsWith("data:")) {
            src = img.absUrl("data-src");
        }
        return src.isEmpty() || src.startsWith("data:") ? null : src;
    }
}
