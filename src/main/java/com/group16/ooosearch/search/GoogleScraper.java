package com.group16.ooosearch.search;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class GoogleScraper {

    public static class GoogleResultItem {
        public int rank;
        public String title;
        public String url;

        public GoogleResultItem(int rank, String title, String url) {
            this.rank = rank;
            this.title = title;
            this.url = url;
        }
    }

    public static void main(String[] args) {
        // 設定要測試的 5~10 組關鍵字
        List<String> keywords = List.of("假面騎士OOO", "火野映司", "安卡");
        Map<String, List<GoogleResultItem>> allResults = new LinkedHashMap<>();

        // 啟動 Chrome (非 headless 模式)
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);

        try {
            for (String kw : keywords) {
                System.out.println("開始搜尋關鍵字：" + kw);
                List<GoogleResultItem> items = scrapeGoogleResults(driver, kw, 20); // 抓取前 20 筆
                allResults.put(kw, items);
                
                // 搜尋關鍵字之間隨機停頓 3~5 秒，避開自動化防護
                Thread.sleep(3000 + new Random().nextInt(2000));
            }

            // 儲存為 JSON 檔案至 data/google_results.json
            saveToJson(allResults, Path.of("data", "google_results.json"));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    private static List<GoogleResultItem> scrapeGoogleResults(WebDriver driver, String keyword, int targetCount) {
        List<GoogleResultItem> items = new ArrayList<>();
        int page = 0;

        while (items.size() < targetCount && page < 3) { // 最多翻 3 頁
            int start = page * 10;
            String searchUrl = "https://www.google.com/search?q=" + java.net.URLEncoder.encode(keyword, StandardCharsets.UTF_8) + "&start=" + start;
            
            driver.get(searchUrl);

            // 如果跳出 CAPTCHA，可在啟動的 Chrome 中手動點選，完成後按 Enter 繼續
            if (driver.getPageSource().contains("recaptcha") || driver.getTitle().contains("Sorry")) {
                System.out.println("⚠️ 觸發 Google 驗證碼！請在開啟的瀏覽器中手動完成驗證，完成後回終端機按 Enter 繼續...");
                new Scanner(System.in).nextLine();
            }

            List<WebElement> searchCards = driver.findElements(By.cssSelector("div.g, div.KzT29"));

            for (WebElement card : searchCards) {
                try {
                    List<WebElement> h3List = card.findElements(By.tagName("h3"));
                    List<WebElement> aList = card.findElements(By.cssSelector("a"));

                    if (!h3List.isEmpty() && !aList.isEmpty()) {
                        String title = h3List.get(0).getText();
                        String url = aList.get(0).getAttribute("href");

                        if (url != null && url.startsWith("http") && !url.contains("google.com")) {
                            int currentRank = items.size() + 1;
                            items.add(new GoogleResultItem(currentRank, title, url));
                            System.out.println("  [" + currentRank + "] " + title + " -> " + url);

                            if (items.size() >= targetCount) break;
                        }
                    }
                } catch (Exception ignored) {}
            }

            page++;
            try {
                Thread.sleep(2000 + new Random().nextInt(2000));
            } catch (InterruptedException ignored) {}
        }

        return items;
    }

    private static void saveToJson(Map<String, List<GoogleResultItem>> data, Path outputPath) {
        try {
            if (outputPath.getParent() != null) {
                Files.createDirectories(outputPath.getParent());
            }
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (Writer writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
                gson.toJson(data, writer);
            }
            System.out.println("\n✅ 成功匯出 Google 原始排名對照資料至：" + outputPath.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("❌ 儲存 JSON 失敗：" + e.getMessage());
        }
    }
}