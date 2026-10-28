# Stage N Report：Stage 3 (Search Engine Aggregator & Pipeline Integration)

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
$$\text{使用者關鍵字} \xrightarrow{\quad} \underbrace{\text{Stage 3 (本階段)}}_{\text{多源搜尋、去重、排序保留}} \xrightarrow{\quad\text{URL 清單}\quad} \underbrace{\text{Stage 2 (網頁爬蟲)}}_{\text{DOM 解析、子連結爬取、建立 WebTree}} \xrightarrow{\quad\text{WebTree 結構}\quad} \underbrace{\text{Stage 4 (權重排序)}}_{\text{關鍵字字典比對、節點算分與 Re-rank}}$$

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
| `com/group16/ooosearch/cli/Stage3Main.java` | Stage 3 主程式入口，執行「關鍵字搜尋 $\rightarrow$ 聚合去重 $\rightarrow$ Stage 2 爬蟲建樹 $\rightarrow$ Google 排名對比」流水線 |

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
