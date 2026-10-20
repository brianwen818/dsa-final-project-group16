package com.group16.ooosearch.fetch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TextExtractorTest {

    private final TextExtractor extractor = new TextExtractor();

    @Test
    void keepsVisibleTextAndSeparatesTags() {
        assertEquals("火野映司 安酷", extractor.extract("<p>火野映司</p><p>安酷</p>"));
    }

    @Test
    void skipsScriptAndStyleEvenWhenTheyContainLessThan() {
        String html = "<script>if (a < b) { x = '<p>假面騎士OOO</p>'; }</script>"
                + "<style>p > a { color: red; }</style><p>核心硬幣</p>";
        assertEquals("核心硬幣", extractor.extract(html));
    }

    @Test
    void skipsTitleBecauseItIsNotBodyText() {
        assertEquals("正文", extractor.extract("<head><title>假面騎士OOO</title></head><body>正文</body>"));
    }

    @Test
    void skipsNestedNavAndFooterUsingStack() {
        String html = "<nav>選單<nav>子選單</nav>還是選單</nav><p>正文</p><footer>版權</footer>";
        assertEquals("正文", extractor.extract(html));
    }

    @Test
    void recoversFromUnclosedTagInsideSkippedBlock() {
        // nav 裡的 svg 沒有關閉，</nav> 會一路 pop 到 nav
        assertEquals("正文", extractor.extract("<nav><svg>圖示</nav><p>正文</p>"));
    }

    @Test
    void keepsTemplateContent() {
        // 萌娘百科把整篇文章放在 template 裡
        assertEquals("文章內容", extractor.extract("<template id=\"skin\"><div>文章內容</div></template>"));
    }

    @Test
    void skipsCommentsAndDoctype() {
        assertEquals("正文", extractor.extract("<!doctype html><!-- <p>註解</p> --><p>正文</p>"));
    }

    @Test
    void decodesEntitiesAndKeepsLiteralLessThan() {
        String text = extractor.extract("<p>OOO &amp; Birth&nbsp;1 < 2</p>");
        assertEquals("OOO & Birth 1 < 2", text);
    }

    @Test
    void isCaseInsensitiveForTagNames() {
        String text = extractor.extract("<SCRIPT>bad()</SCRIPT><P>good</P>");
        assertTrue(text.contains("good"));
        assertFalse(text.contains("bad"));
    }
}
