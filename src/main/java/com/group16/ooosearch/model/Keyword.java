package com.group16.ooosearch.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一個關鍵字、它的權重，以及其他寫法（同義詞，例如「安酷」的「アンク」「Ankh」）。
 * 由 HW6 的 Keyword 整理而來。計數時，所有寫法都算在這個關鍵字上。
 */
public class Keyword {
    private final String name;
    private final double weight;
    private final List<String> aliases;

    public Keyword(String name, double weight) {
        this(name, weight, List.of());
    }

    public Keyword(String name, double weight, List<String> aliases) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("關鍵字名稱不可為空");
        }
        this.name = name.strip();
        this.weight = weight;
        List<String> cleaned = new ArrayList<>();
        for (String alias : aliases) {
            if (alias != null && !alias.isBlank()) {
                cleaned.add(alias.strip());
            }
        }
        this.aliases = Collections.unmodifiableList(cleaned);
    }

    public String getName() {
        return name;
    }

    public double getWeight() {
        return weight;
    }

    /** 其他寫法（不含 name 本身）。 */
    public List<String> getAliases() {
        return aliases;
    }

    /** 所有寫法：name 加上其他寫法。 */
    public List<String> getForms() {
        List<String> forms = new ArrayList<>();
        forms.add(name);
        forms.addAll(aliases);
        return forms;
    }

    @Override
    public String toString() {
        return "[" + name + "," + weight + "]";
    }
}
