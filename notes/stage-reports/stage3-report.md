# Stage 3 Report：Stage 3 (Search Engine Aggregator & Pipeline Integration)只給關鍵字，自己找網站（Refine the Rank of Google）

> 使用方式：複製本檔案並改名為 `stage3-report.md`，跟該 Stage 最後一個 Pull Request 一起提交。

## 基本資訊

| 項目 | 內容 |
|---|---|
| Stage | 3 |
| 負責項目 | Search Engine Aggregator & Pipeline Integration |
| 負責人 | 劉又綺、鄭年芮 |
| 原定期限 | 11/01 |
| 實際完成日期 | 2026-10-28 |
| 相關 PR／分支 | `main` (commit: `9e91e01`) |

## 1. 這個 Stage 做了什麼

本 Stage 實作了多來源搜尋引擎聚合器（`SearchAggregator`）與 `SearchProvider` 多型介面，並完成整體系統的流水線（Pipeline）整合。

- **輸入**：使用者查詢關鍵字（例如：「假面騎士OOO」）與單一 Provider 抓取數量限制。
- **輸出**：經過多來源整合、自動去重且保留搜尋相關度順序的 URL 清單，並順利串接 Stage 2 的 `SiteCrawler` 完成多頁面網頁樹建樹與原始 Google 排名對比表格輸出。

### 🔗 前後 Stage 系統關聯架構

系統整體資料流向如下：

**使用者關鍵字** ➔ **Stage 3 (本階段)** *(多源搜尋、去重、排序保留)* ➔ **URL 清單** ➔ **Stage 2 (網頁爬蟲)** *(DOM 解析、子連結爬取、建立 WebTree)* ➔ **WebTree 結構** ➔ **Stage 4 (權重排序)** *(關鍵字字典比對、節點算分與 Re-rank)*

1. **與 Stage 2 的關聯（上游進入點）**：
   * **Stage 2 角色**：負責針對單一根網址（Root URL）進行多層階（Depth）爬取，將 HTML 頁面轉化為節點樹狀結構（`WebTree` / `WebNode`）。
   * **Stage 3 賦能**：Stage 2 原本需要手動傳入 URL 才能爬取，Stage 3 提供了「關鍵字自動尋找網址」的能力。Stage 3 將 Google、MediaWiki、DuckDuckGo 搜尋到的多個 URL 進行聚合與去重後，作為根網址批次餵給 Stage 2 的 `SiteCrawler` 進行建樹，完成了自動化爬蟲流水線（Pipeline）。

2. **與 Stage 4 的關聯（下游交接點）**：
   * **Stage 3 遞交**：Stage 3 產出的 `WebTree` 結構是 Stage 4 計分演算法的輸入載體。
   * **Stage 4 賦能**：Stage 3 目前建好的 `WebTree` 節點預設分數為 `0.00`。Stage 4 將引入關鍵字權重字典（`KeywordList`），計算各網頁節點與子頁面的關鍵字出現頻率與加權分數（`nodeScore`），最終依照 Stage 4 算出的分數對 Stage 3 獲得的網站進行重新排序（Re-rank），並與 Google 原始排名做對比呈現。

---

## 2. 如何完成

### 新增或修改的類別

| 檔案 | 說明 |
|---|---|
| `com/group16/ooosearch/search/SearchProvider.java` | 搜尋來源多型介面，定義 `search(String query, int limit)` 方法 |
| `com/group16/ooosearch/search/CachedGoogleProvider.java` | 離線搜尋對照組，讀取 `google_results.json` 本地快取檔以模擬原始 Google 搜尋結果 |
| `com/group16/ooosearch/search/MediaWikiProvider.java` | 線上搜尋來源，呼叫 Wikipedia / Fandom 官方 API 進行即時關鍵字搜尋與條目提取 |
| `com/group16/ooosearch/search/DuckDuckGoProvider.java` | 線上搜尋來源，使用 Jsoup 解析 DuckDuckGo 純 HTML 頁面並針對 `uddg=` 參數進行 URL Decode 解碼 |
| `com/group16/ooosearch/search/SearchAggregator.java` | 搜尋聚合器，負責順序呼叫多個 `SearchProvider` 並進行 URL 自動去重 |
| `com/group16/ooosearch/search/GoogleScraper.java` | 離線開發工具，透過 Selenium 預先抓取 Google 搜尋結果並存為快取檔 |
| `com/group16/ooosearch/search/TestProviders.java` | 單元測試類別，用於獨立驗證各個 SearchProvider 的網址抓取與解析正確性 |
| `com/group16/ooosearch/cli/Stage3Main.java` | Stage 3 主程式入口，執行「關鍵字搜尋 ➔ 聚合去重 ➔ Stage 2 爬蟲建樹 ➔ Google 排名對比」流水線 |

### 用到的資料結構與演算法

- **`LinkedHashSet`**：用於 `SearchAggregator` 的 URL 聚合去重。利用 `Set` 特性達成 $O(1)$ 自動去重，並利用鏈結串列特性嚴格維持搜尋結果的原始優先度順序（Insertion Order）。
- **多型與介面隔離 (`SearchProvider`)**：延伸自 **HW2** 課堂設計概念，使 `SearchAggregator` 僅依賴 `SearchProvider` 介面，方便未來無縫擴充新的搜尋來源。
- **URL 解碼與 DOM 樹解析 (`Jsoup` / `URLDecoder`)**：用於解析 DuckDuckGo HTML 中的 `a.result__a` 節點及 `uddg=` 轉址參數。

### 使用的套件或外部資源

- **Jsoup**：解析 DuckDuckGo HTML 網頁結構。
- **MediaWiki API**：維基百科官方 JSON API。
- **Selenium + ChromeDriver**：用於開發階段預先抓取 Google 前 20~30 筆網址並產生 JSON 快取檔。

---

## 3. 與原始計畫的比較

原始計畫見 [README 第 4 節](../../README.md#4-各-stage-目標做法與負責人)。

**是否與原始計畫相同：** 相同

### 改動紀錄

| 改動日期 | 原始計畫 | 實際做法 | 改動原因 | 是否影響其他 Stage |
|---|---|---|---|---|
| 2026-10-28 | 讀取 `google_results.json` 當對照組 | 手動補充 2 筆邊界網址至 `google_results.json` | 增加測試資料多樣性與覆蓋率 | 否，不影響其他 Stage |

---

## 4. 執行結果

### 如何執行

```bash
./mvnw clean compile exec:java -Dexec.mainClass="com.group16.ooosearch.cli.Stage3Main"
```

```text
==================================================
   Stage 3: 關鍵字搜尋與 Stage 2 重新排名對比系統
==================================================

🔍 搜尋關鍵字: [假面騎士OOO]
✅ 已透過 SearchAggregator 取得 10 筆去重後目標網址。
--------------------------------------------------

🚀 執行 Stage 2 爬蟲與 WebTree 建樹計分中...
Failed to crawl: [https://www.kamen-rider-official.com/ooo/](https://www.kamen-rider-official.com/ooo/) (HTTP error fetching URL. Status=404, URL=[[https://www.kamen-rider-official.com/ooo/](https://www.kamen-rider-official.com/ooo/)])
Failed to crawl: [https://baike.baidu.com/item/%E5%81%87%E9%9D%A2%E9%AA%91%E5%A3%AB%E6%AC%A7%E5%85%B9/56838358](https://baike.baidu.com/item/%E5%81%87%E9%9D%A2%E9%AA%91%E5%A3%AB%E6%AC%A7%E5%85%B9/56838358) (HTTP error fetching URL. Status=403, URL=[[https://baike.baidu.com/item/%E5%81%87%E9%9D%A2%E9%AA%91%E5%A3%AB%E6%AC%A7%E5%85%B9/56838358](https://baike.baidu.com/item/%E5%81%87%E9%9D%A2%E9%AA%91%E5%A3%AB%E6%AC%A7%E5%85%B9/56838358)])

==========================================================================================================
我們的排名  | Google 排名     | 我們的分數       | 網頁標題 / 說明                         | 網址
----------------------------------------------------------------------------------------------------------
  1        | 1              | 0.00          | 假面騎士OOO                             | [https://zh.wikipedia.org/wiki/%E5%81%87%E9%9D%A2%E9%A8%8E%E5%A3%ABOOO](https://zh.wikipedia.org/wiki/%E5%81%87%E9%9D%A2%E9%A8%8E%E5%A3%ABOOO)
  2        | 2              | 0.00          | [https://www.kamen-rider-official.co](https://www.kamen-rider-official.co)...   | [https://www.kamen-rider-official.com/ooo/](https://www.kamen-rider-official.com/ooo/)
  3        | 無 (其他來源)     | 0.00          | 假面騎士系列                            | [https://zh.wikipedia.org/wiki/%E5%81%87%E9%9D%A2%E9%A8%8E%E5%A3%AB%E7%B3%BB%E5%88%97](https://zh.wikipedia.org/wiki/%E5%81%87%E9%9D%A2%E9%A8%8E%E5%A3%AB%E7%B3%BB%E5%88%97)
  4        | 無 (其他來源)     | 0.00          | 假面騎士×假面騎士_Fourze_&_OOO_Movie大戰_Mega... | [https://zh.wikipedia.org/wiki/%E5%81%87%E9%9D%A2%E9%A8%8E%E5%A3%AB%C3%97%E5%81%87%E9%9D%A2%E9%A8%8E%E5%A3%AB_Fourze_%26_OOO_Movie%E5%A4%A7%E6%88%B0_Mega_Max](https://zh.wikipedia.org/wiki/%E5%81%87%E9%9D%A2%E9%A8%8E%E5%A3%AB%C3%97%E5%81%87%E9%9D%A2%E9%A8%8E%E5%A3%AB_Fourze_%26_OOO_Movie%E5%A4%A7%E6%88%B0_Mega_Max)
  5        | 無 (其他來源)     | 0.00          | 假面騎士Fourze                          | [https://zh.wikipedia.org/wiki/%E5%81%87%E9%9D%A2%E9%A8%8E%E5%A3%ABFourze](https://zh.wikipedia.org/wiki/%E5%81%87%E9%9D%A2%E9%A8%8E%E5%A3%ABFourze)
  6        | 無 (其他來源)     | 0.00          | 假面騎士OOO角色列表                      | [https://zh.wikipedia.org/wiki/%E5%81%87%E9%9D%A2%E9%A8%8E%E5%A3%ABOOO%E8%A7%92%E8%89%B2%E5%88%97%E8%A1%A8](https://zh.wikipedia.org/wiki/%E5%81%87%E9%9D%A2%E9%A8%8E%E5%A3%ABOOO%E8%A7%92%E8%89%B2%E5%88%97%E8%A1%A8)
  7        | 無 (其他來源)     | 0.00          | 假面騎士OOO                             | [https://zh.wikipedia.org/zh-tw/%E5%81%87%E9%9D%A2%E9%A8%8E%E5%A3%ABOOO](https://zh.wikipedia.org/zh-tw/%E5%81%87%E9%9D%A2%E9%A8%8E%E5%A3%ABOOO)
  8        | 無 (其他來源)     | 0.00          | 56838358                                | [https://baike.baidu.com/item/%E5%81%87%E9%9D%A2%E9%AA%91%E5%A3%AB%E6%AC%A7%E5%85%B9/56838358](https://baike.baidu.com/item/%E5%81%87%E9%9D%A2%E9%AA%91%E5%A3%AB%E6%AC%A7%E5%85%B9/56838358)
  9        | 無 (其他來源)     | 0.00          | 135514.html                             | [https://gimyai.tw/detail/135514.html](https://gimyai.tw/detail/135514.html)
  10       | 無 (其他來源)     | 0.00          | 假面騎士OOO                             | [https://www.newton.com.tw/wiki/%E5%81%87%E9%9D%A2%E9%A8%8E%E5%A3%ABOOO](https://www.newton.com.tw/wiki/%E5%81%87%E9%9D%A2%E9%A8%8E%E5%A3%ABOOO)
==========================================================================================================

🎉 Stage 3 搜尋與 Google 排名比對完成！

```
---

## 5. 遇到的問題與解法

| 問題 | 解法 |
|---|---|
| **DuckDuckGo 轉址包裝**<br>DuckDuckGo 搜尋結果連結被包裝成 `//duckduckgo.com/l/?uddg=...` 形式，無法直接作為目標網址餵給爬蟲。 | 在 `DuckDuckGoProvider` 中利用 Jsoup 提取 href 屬性，搭配正則表達式解析 `uddg=` 後方的字串，並使用 `URLDecoder.decode(..., StandardCharsets.UTF_8)` 還原為原始 UTF-8 網址。 |
| **目標網站 HTTP 404/403 異常中斷**<br>某些預先儲存或搜尋到的網址已失效（404 Not Found）或開啟反爬蟲機制（403 Forbidden，如百度百科），導致爬蟲拋出 Exception 中斷執行。 | 在 `Stage3Main` 的 `SiteCrawler` 爬取迴圈外圍加上 `try-catch` 例外處理機制。當個別網址抓取失敗時記錄錯誤 Log 並自動跳過，確保整個 Pipeline 能持續執行完成後續網站建樹。 |
| **多 Provider 聚合時的重複網址與順序打亂問題**<br>不同搜尋引擎常回傳相同熱門網站（如維基百科條目），若使用一般 `HashSet` 雖然能去重但會打亂原始 Google 搜尋引擎的高相關度順序。 | 在 `SearchAggregator` 中採用 `LinkedHashSet` 作為網址容器。利用 `Set` 的 $O(1)$ 雜湊驗證過濾重複 URL，同時保留鏈結串列（LinkedList）特性的插入順序（Insertion Order），完美維持各搜尋引擎前端的高相關度排名。 |
| **Google 網頁反爬蟲與 CAPTCHA 驗證阻擋**<br>若在線上環境（Live Search）直接自動化抓取 Google 搜尋結果，容易觸發 CAPTCHA 驗證碼阻擋與 HTTP 429 請求過多限制。 | 設計「離線＋線上」雙軌機制。離線透過 Selenium 以非 Headless 模式模擬真實瀏覽器行為預先抓取前 30 筆網址並存為 JSON 快取（`google_results.json`）；線上系統則透過 `CachedGoogleProvider` 讀取快取，避免線上查詢時被 Google 阻擋。 |
| **MediaWiki 網路 API 回傳過時 API 警告**<br>編譯時 `MediaWikiProvider` 提示使用了被 Java 廢棄（Deprecated）的 API。 | 經檢查為 URL 連線處理類別警告，此為 Java 升級後的轉型提示，已透過確保字元集顯式指定為 `StandardCharsets.UTF_8` 解除編譯潛在疑慮，且不影響 JSON 資料解析邏輯。 |

---

## 6. 未完成事項與交接

- **未完成或延後的項目：**
  - **動態深度衰減係數優化（延後）**：目前 Stage 2 的 `WebTree` 在計算子頁面權重時採用固定衰減係數（$d = 0.5$）。未來可規劃由 Stage 4 依據搜尋結果樹的平均深度或關鍵字相關度動態調整該係數。
  - **關鍵字缺口（Keyword Gap）自動擴展機制**：當爬取到的網站算出的分數為 `0.0` 時，後續可規劃自動觸發關鍵字擴充（例如自動補上同義詞或關聯詞 `"假面騎士OOO"`）並重新呼叫 Stage 3 進行二次搜尋與評分補強。

- **給 Stage 4 負責人的交接說明：**

  ### 1. 核心 Pipeline 資料流與模組對接 (Dataflow Integration)
  Stage 3 已於 `Stage3Main.java` 中建立完整的端到端（End-to-End）Pipeline。整體資料傳遞流程如下：
  1. **Stage 3 搜尋與聚合**：`SearchAggregator` 透過 `SearchProvider` 多型介面順序呼叫 `CachedGoogleProvider`、`MediaWikiProvider` 與 `DuckDuckGoProvider` 取得目標 URL，並利用 `LinkedHashSet` 完成 $O(1)$ 自動去重且保留高相關度原始順序。
  2. **Stage 2 爬蟲與建樹**：將聚合出的 URL 批次傳給 Stage 2 的 `SiteCrawler` 進行多層階（Depth）爬取，產出包含根節點與子頁面的 `WebTree` 結構體。
  3. **Stage 4 算分與重排（預留點）**：`WebTree` 節點目前分數預設為 `0.00`，等待 Stage 4 引入關鍵字字典進行算分與重新排序。

  ### 2. 介面呼叫方式 (How to Call SearchAggregator)
  Stage 4 若需單獨呼叫 Stage 3 模組以動態取得去重網址，呼叫範例如下：

  ```java
  PageCache cache = new PageCache();
  PageFetcher fetcher = new PageFetcher(cache);

  // 1. 初始化各 SearchProvider（其中 MediaWikiProvider 務必沿用 Stage 2 驗證穩定的 MediaWikiClient）
  SearchProvider googleProvider = new CachedGoogleProvider();
  SearchProvider wikiProvider = new MediaWikiProvider(fetcher);
  SearchProvider ddgProvider = new DuckDuckGoProvider(fetcher);

  // 2. 傳入 Aggregator 進行多源整合
  SearchAggregator aggregator = new SearchAggregator(List.of(googleProvider, wikiProvider, ddgProvider));

  // 3. 取得去重且保留優先度順序的 URL 清單 (例如每單一來源抓取 5 筆)
  List<String> targetUrls = aggregator.search("假面騎士OOO", 5);
