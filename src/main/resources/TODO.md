# resources/ — 設定檔與前端

- **Stage**：5
- **負責人**：劉又綺、鄭年芮

## 要建立的檔案

- [x] `application.properties`：Spring Boot 設定（Stage 0 已建立，`server.port=8080`）
- [ ] `static/index.html`：搜尋頁面
- [ ] `static/` 的 CSS、JS

## 頁面功能

- [ ] 輸入任何關鍵字（例如「安卡 結局」、「貓咪」）
- [ ] 每個結果的「為什麼相關」卡片：前 3 段段落（查詢詞與 OOO 關鍵字以螢光標示）、標籤、圖片與來源、跳到該章節的連結（資料來自 `SearchResponse.getPassages(rootUrl)`，開發時可先用 `FakeSearchEngine` 的示範段落）
- [ ] 顯示排名、分數、各關鍵字次數
- [ ] 可展開的網站樹狀結構
- [ ] 「系統自動擴充的關鍵字」區塊
