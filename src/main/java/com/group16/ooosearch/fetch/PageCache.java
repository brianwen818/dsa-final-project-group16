package com.group16.ooosearch.fetch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

/**
 * 網頁快取：抓過的網頁存成檔案，下次直接讀取，不必重新連線。
 * 磁碟上的檔名是網址的 SHA-256；記憶體中用 HashMap 當索引，避免重複讀檔。
 */
public class PageCache {
    /** 預設的快取資料夾（已加入 .gitignore）。 */
    public static final Path DEFAULT_DIRECTORY = Path.of("data", "cache");

    private final Path directory;
    private final Map<String, String> memory = new HashMap<>();

    public PageCache() {
        this(DEFAULT_DIRECTORY);
    }

    public PageCache(Path directory) {
        this.directory = directory;
    }

    /** 取得快取的 HTML；沒有快取時回傳 Optional.empty()。 */
    public Optional<String> get(String url) throws IOException {
        String html = memory.get(url);
        if (html != null) {
            return Optional.of(html);
        }
        Path file = fileFor(url);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        html = Files.readString(file, StandardCharsets.UTF_8);
        memory.put(url, html);
        return Optional.of(html);
    }

    public void put(String url, String html) throws IOException {
        Files.createDirectories(directory);
        Files.writeString(fileFor(url), html, StandardCharsets.UTF_8);
        memory.put(url, html);
    }

    private Path fileFor(String url) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(url.getBytes(StandardCharsets.UTF_8));
            return directory.resolve(HexFormat.of().formatHex(hash) + ".html");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("此 JDK 不支援 SHA-256", e);
        }
    }
}
