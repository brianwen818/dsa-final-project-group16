# tree/ — 網站樹狀結構與網站排名

- **Stage**：2
- **負責人**：廖若羽
- **期限**：10/27
- **依賴**：`model/`、`fetch/`、`scoring/`（Stage 1 未完成前，先用假的 `PageScorer`）

## 要建立的檔案

- [ ] `WebNode.java`：樹節點（HW6）
- [ ] `WebTree.java`：樹，用後序走訪加總分數（HW6）
- [ ] `SiteCrawler.java`：用 BFS（Queue）從根網址自動建樹；`HashSet` 記錄已造訪網址；限制深度與每層節點數
- [ ] `LinkFilter.java`：只留同網域、和主題相關的連結；排除登入、隨機頁面、廣告
- [ ] `SiteRanker.java`：介面與實作，依網站總分排名
- [ ] 深度衰減：節點分數乘上 `0.5^深度`
- [ ] 印出樹狀結構（每個節點附關鍵字次數）

## 另外

- [ ] 先定好 `search/SearchProvider.java` 介面，交給 Stage 3 接手

## Stage 完成後

- [ ] 在 `notes/stage-reports/` 撰寫 `stage2-report.md`（複製 `TEMPLATE.md`）
