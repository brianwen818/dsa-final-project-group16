package com.group16.ooosearch.model;

/**
 * 一個關鍵字與它的權重（由 HW6 的 Keyword 整理而來）。
 */
public class Keyword {
    private final String name;
    private final double weight;

    public Keyword(String name, double weight) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("關鍵字名稱不可為空");
        }
        this.name = name.strip();
        this.weight = weight;
    }

    public String getName() {
        return name;
    }

    public double getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        return "[" + name + "," + weight + "]";
    }
}
