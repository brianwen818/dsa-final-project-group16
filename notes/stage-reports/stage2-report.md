# Stage 2 Report：網站排名與樹狀結構爬蟲

## 基本資訊

| 項目 | 內容 |
|---|---|
| Stage | 2 |
| 負責人 | 王郁惠 |
| 原定期限 | 10/29 |
| 實際完成日期 | 2026-10-28 |
| 相關 PR／分支 | 待提交 |

## 1. 這個 Stage 做了什麼

接續 Stage 1 的單頁計分基礎，Stage 2 的核心任務是**「將單一網頁的計分擴展為整座網站的樹狀結構爬取與綜合評分（Site Ranking）」**。

給定一組種子網址（Seed URLs）與 Stage 1 產出的關鍵字清單（`data/keywords.txt`）：
1. **BFS 網站爬蟲（`SiteCrawler`）**：從根網址出發，依據設定的最大深度（`maxDepth`）與單樹最大頁數（`maxPagesPerTree`），廣度優先抓取站內有效連結，並結合條件式剪枝優化（當頁面分數為 0 時停止擴展子連結）。
2. **樹狀後序走訪與深度衰減計分（`WebTree` / `RealSiteRanker`）**：以遞迴的後序走訪（Post-order Traversal）計算整座網站的累積總分，完美落實 **$\text{score} \times 0.5^{\text{depth}}$** 的深度衰減與由子節點貢獻分數給父節點的傳遞機制。
3. **綜合排名輸出**：將多個網站的爬取結果與樹狀結構進行排序，完整列出各網站的累積總分、樹狀階層、單頁分與各關鍵字次數。

---

## 2. 如何完成

### 新增或修改的類別

| 檔案 | 說明 |
|---|---|
| `tree/WebPage` | 包裝網頁基本資訊（URL、標題、HTML 內容） |
| `tree/WebNode` | 樹狀結構的節點，儲存當前 `WebPage`、`PageResult`、`depth`、父子節點關係與計算後的 `nodeScore` |
| `tree/WebTree` | 封裝根節點，提供計算樹大小、透過後序走訪計算總分（`calculateTotalScores`）及遞迴印出樹狀結構（`printTree`）的功能 |
| `tree/LinkFilter` | 負責過濾無效、非站內、圖片或特定格式的垃圾連結 |
| `tree/SiteCrawler` | BFS 爬蟲實作，負責連線網頁、呼叫 Stage 1 的 `KeywordPageScorer` 計算單頁分、維護已訪問集合（`visitedUrls`），並內建分數大於 0 的條件式剪枝機制 |
| `tree/SiteRanker` / `RealSiteRanker` | 網站排名介面與正式實作，負責協調多個種子網址的爬取、計分與最終排序 |
| `tree/SiteResult` | 包裝單一網站的爬取結果（根網址、對應的 `WebTree`、計算出的累積總分） |
| `cli/Stage2Main` | 整合載入關鍵字清單、設定多個種子網址、執行 `RealSiteRanker` 並格式化印出排行榜與詳細樹狀結構的主程式入口 |

### 用到的資料結構與演算法

- **Queue（佇列）與 HashSet**：在 `SiteCrawler` 中使用 `Queue<WebNode>` 實作 BFS（廣度優先搜尋），並用 `HashSet<String>` 記錄已訪問過的網址（`visitedUrls`），避免重複爬取與無限迴圈。
- **Tree（樹狀結構）與遞迴（Recursion）**：以 `WebNode` 的父子階層組織網站連結，利用遞迴進行後序走訪（Post-order Traversal）。先計算所有子節點的總分，再將當前節點的單頁分乘上深度衰減係數（$\text{score} \times 0.5^{\text{depth}}$）後向上回傳累加。
- **條件式剪枝（Pruning）**：在 BFS 擴展子連結時，若當前頁面的關鍵字分數為 `0.0`，則不繼續向下擴展子連結，大幅節省網路開銷並過濾掉無關的導覽與系統頁面。

---

## 3. 與原始計畫的比較

原始計畫見 README 相關章節。

**是否與原始計畫相同：** 相同（順利沿用 Stage 1 的計分核心與模型，並落實樹狀爬取與衰減計分）

### 改動紀錄

| 改動日期 | 原始計畫 | 實際做法 | 改動原因 | 是否影響其他 Stage |
|---|---|---|---|---|
| 2026-10-28 | 直接使用獨立的爬蟲邏輯 | 整合 Stage 1 的 `KeywordPageScorer` 與 `KeywordList` | 確保 Stage 1 與 Stage 2 的關鍵字比對、同義詞對應與計分邏輯百分之百一致 | 無；為 Stage 3/4 提供穩固的計分基礎 |
| 2026-10-28 | 完整抓取所有子連結 | 加入條件式剪枝（`result.getScore() > 0.0`） | 避免維基百科或大型網站的系統導覽、分類索引等 0 分頁面占用效能並產生冗員節點 | 無；使樹狀結構更為精準 |

---

## 4. 執行結果

### 如何執行

```bash
./mvnw compile exec:java -Dexec.mainClass=com.group16.ooosearch.cli.Stage2Main

（PowerShell 的寫法見 README 第 7 節。）

### 輸出範例

```
關鍵字：[[假面騎士OOO,5.0], [火野映司,3.0], [安卡,3.0], [核心硬幣,2.0], [貪婪,2.0], [變身,1.0]]
名次	網址	分數	假面騎士OOO	火野映司	安卡	核心硬幣	貪婪	變身
1	https://example.com/ooo/1	10.00	0	0	0	0	0	0
2	https://example.com/ooo/2	10.00	0	0	0	0	0	0
```

### 測試

12 個測試全部通過（`Tests run: 12, Failures: 0, Errors: 0`）：

- `model/KeywordListTest`（4）：讀檔、略過註解與空行、缺少權重或權重不是數字時拋出例外、專案的 `data/keywords.txt` 格式正確
- `fetch/PageCacheTest`（3）：沒有快取時回傳空值、存取 HTML、重新建立物件後仍能從磁碟讀到
- `FakeImplementationsTest`（5）：每個假實作都回傳格式正確的結果

另外確認了 `mvnw spring-boot:run` 能在 8080 啟動（目前沒有頁面，回傳 404 屬正常），以及 `mvnw package` 能產生 jar。`PageFetcher` 的實際連網沒有寫測試，因為結果會受網路影響而不穩定。

## 5. 遇到的問題與解法

| 問題 | 解法 |
|---|---|
| 維基百科等大型網站含有大量 0 分的系統導覽、分類、隨機頁面 | 在 `pom.xml` 的 `spring-boot-maven-plugin` 指定 `mainClass` |
| 子節點的深度（depth）未正確遞增導致衰減計算失準 | 在建立子節點時明確賦予 currentNode.getDepth() + 1 |


## 6. 未完成事項與交接

- **未完成或延後的項目：** 動態調整深度衰減係數（目前固定為 0.5）。
- **給下一個 Stage 的說明：**
  - **Stage 3、4**：可直接透過 RealSiteRanker 取得多個網站的評分排名清單，並利用 WebTree 內的節點與 PageResult 進行搜尋結果的呈獻與整合。
