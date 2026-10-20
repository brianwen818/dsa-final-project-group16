# engine/ — 串起所有 Stage 的總入口

- **Stage**：0（介面）／Stage 4 完成後整合
- **負責人**：温亮達（介面）；整合由程式組負責
- **依賴**：`semantic/`、`search/`、`tree/`、`scoring/`

## 要建立的檔案

- [ ] `SearchEngine.java`：介面 `SearchResponse search(String query)`，附假資料實作（Stage 0，讓 Stage 5 能先開發前端）
- [ ] `SearchEngine` 的正式實作：搜尋 → 建樹排名 → 關鍵字擴充 → 再搜尋
