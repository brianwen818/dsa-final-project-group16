# tree/ — 網站樹狀結構與網站排名

- **Stage**：2
- **負責人**：廖若羽
- **期限**：10/27
- **依賴**：`model/`、`fetch/`、`scoring/`（Stage 1 未完成前，先用假的 `PageScorer`）

## 要建立的檔案

- [X] `WebNode.java`：樹節點資料結構（Stage 0 已從 HW6 搬來；根節點深度為 0）
- [X] `WebTree.java`：樹的資料結構（Stage 0 已建立）
- [X] 在 `WebTree` 實作後序走訪加總分數、印出樹狀結構（HW6）
- [X] `SiteCrawler.java`：用 BFS（Queue）從根網址自動建樹；`HashSet` 記錄已造訪網址；限制深度與每層節點數
- [X] `LinkFilter.java`：只留同網域、和主題相關的連結；排除登入、隨機頁面、廣告
- [X] `SiteRanker.java`、`SiteResult.java`：介面與結果類別（Stage 0 完成，假實作為 `FakeSiteRanker`）
- [X] `SiteRanker` 的正式實作，依網站總分排名
- [X] 深度衰減：節點分數乘上 `0.5^深度`
- [X] 印出樹狀結構（每個節點附關鍵字次數）

## 另外

- [X] ~~先定好 `search/SearchProvider.java` 介面~~：已在 Stage 0 完成

## Stage 完成後

- [X] 在 `notes/stage-reports/` 撰寫 `stage2-report.md`（複製 `TEMPLATE.md`）
