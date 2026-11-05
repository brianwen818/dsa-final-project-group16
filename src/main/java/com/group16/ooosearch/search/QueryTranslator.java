package com.group16.ooosearch.search;

import java.io.IOException;
import java.util.List;

/**
 * 把外語查詢翻成中文、日文寫法，當成查詢詞的別名。
 *
 * <p>網頁來源以中日文為主，使用者若輸入法文 {@code chat}、韓文 {@code 고양이}，原樣比對時次數為 0。
 * 翻譯結果加入查詢詞的別名後，搜尋、計分、段落摘要都沿用原本的流程。
 */
public interface QueryTranslator {
    /** 不翻譯（測試、離線執行用）。 */
    QueryTranslator NONE = query -> List.of();

    /** 回傳查詢的中日文寫法（不含原查詢）；不需要翻譯時回傳空清單。 */
    List<String> translate(String query) throws IOException;
}
