# search/ — 自主搜尋網址（Refine the Rank of Google）

- **Stage**：3
- **負責人**：劉又綺、鄭年芮
- **期限**：11/01
- **依賴**：`model/`、`fetch/`、`tree/`（Stage 2 完成前可用 `FakeSiteRanker`）

## 要建立的檔案

- [x] `SearchProvider.java`：介面 `List<String> search(String query, int limit)`（Stage 0 完成，假實作為 `FakeSearchProvider`）
- [ ] `DuckDuckGoProvider.java`：解析 `html.duckduckgo.com/html/`；處理 `uddg=` 轉址連結
- [ ] `MediaWikiProvider.java`：維基、Fandom 的搜尋 API
- [ ] `CachedGoogleProvider.java`：讀取 `data/google_results.json`
- [ ] `SearchAggregator.java`：呼叫所有 Provider，用 `LinkedHashSet` 合併並去除重複
- [ ] `GoogleResultCollector.java`：**離線執行**的 Selenium 程式，抓 Google 前 20～30 筆並存成 json

## 其他工作

- [ ] 挑選 5～10 組關鍵字，執行 `GoogleResultCollector` 取得對照組
- [ ] 整理「Google 排名 vs 我們的排名」比較表（報告與 Demo 用）

## 注意

- Selenium 不用 headless、每次查詢間隔幾秒、不放進線上系統

## Stage 完成後

- [ ] 在 `notes/stage-reports/` 撰寫 `stage3-report.md`（複製 `TEMPLATE.md`）
