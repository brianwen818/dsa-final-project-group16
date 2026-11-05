# engine/ — 串起所有 Stage 的總入口

- **Stage**：0（介面）／Stage 4 完成後整合
- **負責人**：温亮達（介面）；整合由程式組負責
- **依賴**：`semantic/`、`search/`、`tree/`、`scoring/`

## 要建立的檔案

- [x] `SearchEngine.java`、`SearchResponse.java`：介面與結果類別，假實作為 `FakeSearchEngine`（Stage 0 完成，Stage 5 可以先用它開發前端）
- [x] `SearchEngine` 的正式實作：`RealSearchEngine`（搜尋 → 建樹排名 → 段落 → 關鍵字擴充 → 再搜尋，最多 2 輪 → 段落與圖片；2026-11-05）
- [x] `SearchResponse` 加上每個網站的段落摘要：`getPassages(rootUrl)`（2026-11-05；舊建構子保留，`FakeSearchEngine` 附示範段落）
