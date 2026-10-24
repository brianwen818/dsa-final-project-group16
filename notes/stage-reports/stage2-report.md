# Stage 2 Report：網站排名與樹狀結構爬蟲

## 基本資訊

| 項目 | 內容 |
|---|---|
| Stage | 2 |
| 負責人 | 廖若羽 |
| 原定期限 | 10/27 |
| 實際完成日期 | 2026-10-24 |
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
```

（PowerShell 的寫法見 README 第 7 節。）

### 輸出（2026-10-24，完整結果見 `data/results/stage2-results.csv`）

========== 排名結果 ==========
1. 網址: https://ja.wikipedia.org/wiki/%E4%BB%AE%E9%9D%A2%E3%83%A9%E3%82%A4%E3%83%80%E3%83%BC%E3%82%AA%E3%83%BC%E3%82%BA/OOO | 累積總分: 7026.9
├── https://ja.wikipedia.org/wiki/%E4%BB%AE%E9%9D%A2%E3%83%A9%E3%82%A4%E3%83%80%E3%83%BC%E3%82%AA%E3%83%BC%E3%82%BA/OOO [單頁分: 7026.9, 累積總分: 7026.9] | 關鍵字次數: 假面騎士OOO:409 核心硬幣:122 安酷:178 貪婪:185 Yummy:172 火野映司:143一般硬幣:73 慾望:83 卡佳里:29 烏霸:30 梅茲魯:25 假面騎士Birth:44 佳美爾:26 鴻上基金會:24 OOO驅動器:19 伊達明:11 多國料理店:23 TaToBa COMBO:24 鴻上光生:8 真木清人:10 後藤慎太郎:7 泉比奈:8 
  ├── https://ja.wikipedia.org/wiki/%E4%BB%AE%E9%9D%A2%E3%83%A9%E3%82%A4%E3%83%80%E3%83%BC%E3%82%B7%E3%83%AA%E3%83%BC%E3%82%BA [單頁分: 0.0, 累積總分: 0.0]
  ├── https://ja.wikipedia.org/wiki/Wikipedia:IINFO [單頁分: 0.0, 累積總分: 0.0]
  ├── https://ja.wikipedia.org/wiki/Wikipedia:%E3%82%A6%E3%82%A3%E3%82%AD%E3%83%9A%E3%83%87%E3%82%A3%E3%82%A2%E3%81%AF%E4%BD%95%E3%81%A7%E3%81%AF%E3%81%AA%E3%81%84%E3%81%8B [單頁分: 0.0, 累積總分: 0.0]
  ├── https://ja.wikipedia.org/wiki/Wikipedia:%E9%81%8E%E5%89%B0%E3%81%AA%E5%86%85%E5%AE%B9%E3%81%AE%E6%95%B4%E7%90%86 [單頁分: 0.0, 累積總分: 0.0]
  ├── https://ja.wikipedia.org/wiki/%E3%83%8E%E3%83%BC%E3%83%88:%E4%BB%AE%E9%9D%A2%E3%83%A9%E3%82%A4%E3%83%80%E3%83%BC%E3%82%AA%E3%83%BC%E3%82%BA/OOO [單頁分: 0.0, 累積總分: 0.0]
  ├── https://ja.wikipedia.org/wiki/%E4%BB%AE%E9%9D%A2%E3%83%A9%E3%82%A4%E3%83%80%E3%83%BCW [單頁分: 0.0, 累積總分: 0.0]
  ├── https://ja.wikipedia.org/wiki/%E4%BB%AE%E9%9D%A2%E3%83%A9%E3%82%A4%E3%83%80%E3%83%BC%E3%83%95%E3%82%A9%E3%83%BC%E3%82%BC [單頁分: 0.0, 累積總分: 0.0]
  ├── https://ja.wikipedia.org/wiki/%E7%89%B9%E6%92%AE [單頁分: 0.0, 累積總分: 0.0]
  ├── https://ja.wikipedia.org/wiki/%E3%83%86%E3%83%AC%E3%83%93%E3%83%89%E3%83%A9%E3%83%9E [單頁分: 0.0, 累積總分: 0.0]
----------------------------------------
2. 網址: https://zh.wikipedia.org/wiki/%E5%81%87%E9%9D%A2%E9%A8%8e%E5%A3%ABOOO | 累積總分: 2120.2999999999993
├── https://zh.wikipedia.org/wiki/%E5%81%87%E9%9D%A2%E9%A8%8e%E5%A3%ABOOO [單頁分: 2120.2999999999993, 累積總分: 2120.2999999999993] | 關鍵字次數: 假面騎士OOO:102 核心硬幣:97 安酷:37 貪婪:32 Yummy:33 火野映司:36 一般硬幣:26 慾望:19 卡佳里:33 烏霸:13 梅茲魯:10 假面騎士Birth:8 佳美爾:5 鴻上基金會:12 OOO驅動器:12 伊達明:7 多國料理店:3 TaToBa COMBO:7 鴻上光生:4 真木清人:2 後藤慎太郎:6 泉比奈:2 
  ├── https://zh.wikipedia.org/w/index.php?title=%E5%81%87%E9%9D%A2%E9%A8%8E%E5%A3%ABOOO&action=edit [單頁分: 0.0, 累積總分: 0.0]
  ├── https://zh.wikipedia.org/wiki/Talk:%E5%81%87%E9%9D%A2%E9%A8%8E%E5%A3%ABOOO [單頁分: 0.0, 累積總分: 0.0]
  ├── https://zh.wikipedia.org/wiki/Wikipedia:%E5%A6%82%E4%BD%95%E7%B7%A8%E5%AF%AB%E6%95%85%E4%BA%8B%E7%B0%A1%E4%BB%8B [單頁分: 0.0, 累積總分: 0.0]
  ├── https://zh.wikipedia.org/wiki/Wikipedia:%E6%A0%BC%E5%BC%8F%E6%89%8B%E5%86%8C/%E8%99%9A%E6%9E%84 [單頁分: 0.0, 累積總分: 0.0]
  ├── https://zh.wikipedia.org/wiki/Wikipedia:%E7%BB%B4%E5%9F%BA%E7%99%BE%E7%A7%91%E4%B8%8D%E6%98%AF%E4%BB%80%E4%B9%88 [單頁分: 0.0, 累積總分: 0.0]
  ├── https://zh.wikipedia.org/wiki/WikiProject:ACG [單頁分: 0.0, 累積總分: 0.0]
  ├── https://zh.wikipedia.org/wiki/File:Tango-nosources.svg [單頁分: 0.0, 累積總分: 0.0]
  ├── https://zh.wikipedia.org/wiki/Wikipedia:%E5%88%97%E6%98%8E%E6%9D%A5%E6%BA%90 [單頁分: 0.0, 累積總分: 0.0]
  ├── https://zh.wikipedia.org/wiki/Wikipedia:%E5%8F%AF%E9%9D%A0%E6%9D%A5%E6%BA%90 [單頁分: 0.0, 累積總分: 0.0]
----------------------------------------
3. 網址: https://zh.moegirl.org.cn/zh-tw/%E5%81%87%E9%9D%A2%E9%AA%91%E5%A3%ABOOO | 累積總分: 548.9000000000002
├── https://zh.moegirl.org.cn/zh-tw/%E5%81%87%E9%9D%A2%E9%AA%91%E5%A3%ABOOO [單頁分: 548.9000000000002, 累積總分: 548.9000000000002] | 關鍵字次數: 假面騎士OOO:26 核心硬幣:4 安酷:20 貪婪:12 Yummy:4 火野映司:15 慾望:24 卡佳里:1 烏霸:1 梅茲魯:1 假面騎士Birth:4 佳美爾:1 鴻上基金會:6 OOO驅動器:2 伊達明:2 多國料理店:1 鴻上光生:4 真木清人:2 後藤慎太郎:2 泉比奈:2 
  ├── https://zh.moegirl.org.cn/%E8%90%8C%E5%A8%98%E7%99%BE%E7%A7%91:%E6%B6%88%E6%AD%A7%E4%B9%89%E6%96%B9%E9%92%88 [單頁分: 0.0, 累積總分: 0.0]
  ├── https://zh.moegirl.org.cn/%E5%81%87%E9%9D%A2%E9%AA%91%E5%A3%AB%E7%B3%BB%E5%88%97 [單頁分: 0.0, 累積總分: 0.0]
  ├── https://zh.moegirl.org.cn/%E5%81%87%E9%9D%A2%E9%AA%91%E5%A3%ABOOO(%E9%AA%91%E5%A3%AB) [單頁分: 0.0, 累積總分: 0.0]
  ├── https://zh.moegirl.org.cn/%E7%81%AB%E9%87%8E%E6%98%A0%E5%8F%B8 [單頁分: 0.0, 累積總分: 0.0]
  ├── https://zh.moegirl.org.cn/index.php?title=%E5%81%87%E9%9D%A2%E9%AA%91%E5%A3%ABOOO&action=edit [單頁分: 0.0, 累積總分: 0.0]
  ├── https://zh.moegirl.org.cn/Help:Wiki%E5%85%A5%E9%97%A8 [單頁分: 0.0, 累積總分: 0.0]
  ├── https://zh.moegirl.org.cn/%E8%90%8C%E5%A8%98%E7%99%BE%E7%A7%91:%E7%BC%96%E8%BE%91%E8%A7%84%E8%8C%83 [單頁分: 0.0, 累積總分: 0.0]
  ├── https://zh.moegirl.org.cn/%E8%90%8C%E5%A8%98%E7%99%BE%E7%A7%91:%E6%A8%A1%E6%9D%BF%E8%A7%84%E8%8C%83 [單頁分: 0.0, 累積總分: 0.0]
  ├── https://zh.moegirl.org.cn/%E5%81%87%E9%9D%A2%E9%AA%91%E5%A3%ABW [單頁分: 0.0, 累積總分: 0.0]
---------------------------------------- 

觀察：
- **條件式剪枝過濾了大量無關的系統導覽頁**：各網站的子節點雖然被抓出了諸如 action=edit（編輯頁）、Talk:（討論頁）或 Wikipedia: 政策指引頁，但因為其自身分數為 0.0，爬蟲皆自動停止向下擴展。
- 各網站皆能透過根節點與子節點的層級關係，正確計算出累積總分與單頁分。

### 測試

相關單元測試與整合測試皆順利通過：

- `tree/LinkFilterTest`（4）：驗證是否能正確過濾掉非同網域、圖片、無效格式或特殊協議的外部連結。
- `tree/SiteCrawlerTest`（3）：驗證整合 `MediaWikiClient` 的 BFS 爬蟲是否能依據設定的 `maxDepth` 與 `maxPagesPerTree` 正常運作，並測試分數為 0 時的條件式剪枝機制是否成功停止向下擴展。
- `tree/WebTreeTest`（4）：驗證後序走訪（Post-order Traversal）與深度衰減公式（$\text{score} \times 0.5^{\text{depth}}$）的遞迴加總邏輯是否正確。
- Stage 0 與 Stage 1 原有的核心計分與模型測試全數保持相容。

## 5. 遇到的問題與解法

| 問題 | 解法 |
|---|---|
| 維基百科等大型網站含有大量 0.0 分的系統導覽、分類、隨機頁面 | 在 `SiteCrawler` 中實作剪枝機制，當 `result.getScore() > 0.0` 時才往下擴展子連結 |
| 子節點的深度（depth）未正確遞增導致衰減計算失準 | 在建立子節點時明確使用 `childNode.setDepth(currentNode.getDepth() + 1)` |
| 部分 Wiki 站點與替代站點因前端 JavaScript 動態渲染或被 `403 Forbidden` 防火牆阻擋導致爬取失敗、分數為 0.0 | 將 `SiteCrawler` 整合 Stage 1 的 `MediaWikiClient`，透過官方 API 繞過前端渲染與反爬機制，成功讓萌娘百科等替代站點與跨語言維基（如日文維基）順利抓取並計分 | 


## 6. 未完成事項與交接

- **未完成或延後的項目：**
  - 依動態調整深度衰減係數（目前固定為 0.5）。
  - 爬取到的頁面或網站分數為 0.0 時，後續可規劃當作「關鍵字缺口」，自動觸發擴展機制，補充關聯詞 "假面騎士ooo" 後重新搜尋與評分。
- **給下一個 Stage 的說明：**
  - **Stage 3/4**：可直接透過 `RealSiteRanker` 取得多個網站的評分排名清單，並利用 `WebTree` 內的節點與 `PageResult` 進行搜尋結果的呈獻與整合。
