package com.group16.ooosearch.scoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.group16.ooosearch.model.Keyword;
import com.group16.ooosearch.model.KeywordList;

/**
 * 同義詞表：用 HashMap 建立「寫法 → 關鍵字名稱」的對應（多對一）。
 * 例如「アンク」「Ankh」「安卡」都對應到「安酷」。
 * 同一種寫法只能屬於一個關鍵字，否則計數時無法決定要算給誰。
 */
public class SynonymTable {
    private final Map<String, String> keywordOf = new HashMap<>();
    private final List<String> formsLongestFirst;

    public SynonymTable(KeywordList keywords) {
        for (Keyword keyword : keywords) {
            for (String form : keyword.getForms()) {
                String normalized = TextNormalizer.forMatching(form);
                if (normalized.isEmpty()) {
                    continue;
                }
                String previous = keywordOf.putIfAbsent(normalized, keyword.getName());
                if (previous != null && !previous.equals(keyword.getName())) {
                    throw new IllegalArgumentException(
                            "寫法「" + form + "」同時屬於「" + previous + "」與「" + keyword.getName() + "」");
                }
            }
        }
        List<String> forms = new ArrayList<>(keywordOf.keySet());
        forms.sort(Comparator.comparingInt(String::length).reversed().thenComparing(Comparator.naturalOrder()));
        this.formsLongestFirst = Collections.unmodifiableList(forms);
    }

    /** 傳入正規化後的寫法（TextNormalizer.forMatching），回傳它所屬的關鍵字名稱；沒有則回傳 null。 */
    public String keywordOf(String normalizedForm) {
        return keywordOf.get(normalizedForm);
    }

    /** 所有正規化後的寫法，由長到短排序（長度相同時依字典順序，讓結果固定）。 */
    public List<String> formsLongestFirst() {
        return formsLongestFirst;
    }
}
