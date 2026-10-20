# fetch/ — 抓網頁、快取、抽取正文

- **Stage**：0（`TextExtractor` 在 Stage 1 完成）
- **負責人**：温亮達
- **期限**：10/22
- **依賴**：`model/`

## 要建立的檔案

- [x] `PageFetcher.java`：用 Jsoup 抓網頁；設定 User-Agent 與請求間隔
- [x] `PageCache.java`：網頁存到 `data/cache/`，用 `HashMap` 當索引；有快取就不重新抓
- [ ] `TextExtractor.java`（Stage 1）：用 Stack（HW5）跳過 `<script>`、`<style>`、`<nav>`，只保留正文

## 注意

- 萌娘百科可能擋爬蟲；維基、Fandom 盡量用 MediaWiki API（`action=parse`）取得內容
