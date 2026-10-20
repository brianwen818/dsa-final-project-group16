package com.group16.ooosearch.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * 一組關鍵字（由 HW4 的 KeywordList 整理而來）。保留加入的順序。
 */
public class KeywordList implements Iterable<Keyword> {
    private final List<Keyword> keywords = new ArrayList<>();

    public KeywordList() {
    }

    public KeywordList(List<Keyword> keywords) {
        this.keywords.addAll(keywords);
    }

    /**
     * 讀取關鍵字檔（例如 data/keywords.txt），檔案須為 UTF-8。
     * 每行格式為「權重 | 關鍵字 | 其他寫法1, 其他寫法2, ...」，其他寫法可省略。
     * 空行與 # 開頭的行會被略過。
     */
    public static KeywordList fromFile(Path path) throws IOException {
        KeywordList list = new KeywordList();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            list.add(parseLine(line, path, i + 1));
        }
        return list;
    }

    private static Keyword parseLine(String line, Path path, int lineNumber) {
        String[] columns = line.split("\\|", -1);
        if (columns.length < 2 || columns[1].isBlank()) {
            throw new IllegalArgumentException(path + " 第 " + lineNumber + " 行格式錯誤，應為「權重 | 關鍵字 | 其他寫法」：" + line);
        }
        String weight = columns[0].strip();
        List<String> aliases = columns.length > 2 ? splitForms(columns[2]) : List.of();
        try {
            return new Keyword(columns[1], Double.parseDouble(weight), aliases);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(path + " 第 " + lineNumber + " 行的權重不是數字：" + weight, e);
        }
    }

    /** 以半形或全形逗號分隔多個寫法。 */
    public static List<String> splitForms(String text) {
        List<String> forms = new ArrayList<>();
        for (String form : text.split("[,，]")) {
            if (!form.isBlank()) {
                forms.add(form.strip());
            }
        }
        return forms;
    }

    public void add(Keyword keyword) {
        keywords.add(keyword);
    }

    public Keyword get(int index) {
        return keywords.get(index);
    }

    public int size() {
        return keywords.size();
    }

    public boolean isEmpty() {
        return keywords.isEmpty();
    }

    /** 回傳唯讀的關鍵字清單。 */
    public List<Keyword> toList() {
        return Collections.unmodifiableList(keywords);
    }

    @Override
    public Iterator<Keyword> iterator() {
        return toList().iterator();
    }

    @Override
    public String toString() {
        return keywords.toString();
    }
}
