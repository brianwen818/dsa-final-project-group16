# engine/ — 串起所有 Stage 的總入口

- **Stage**：0（介面）／Stage 4 完成後整合
- **負責人**：温亮達（介面）；整合由程式組負責
- **依賴**：`semantic/`、`search/`、`tree/`、`scoring/`

## 要建立的檔案

- [x] `SearchEngine.java`、`SearchResponse.java`：介面與結果類別，假實作為 `FakeSearchEngine`（Stage 0 完成，Stage 5 可以先用它開發前端）
- [ ] `SearchEngine` 的正式實作：搜尋 → 建樹排名 → 關鍵字擴充 → 再搜尋
