# model/ — 資料類別

- **Stage**：0
- **負責人**：温亮達
- **期限**：10/22
- **說明**：所有 package 共用的資料類別，不依賴任何其他 package。由 HW4、HW6 的類別整理而來。

## 要建立的檔案

- [x] `Keyword.java`：關鍵字名稱與權重（HW4、HW6）
- [x] `KeywordList.java`：關鍵字集合（HW4）；`fromFile` 讀取 `data/keywords.txt`
- [x] `WebPage.java`：網址、標題、原始 HTML（HW6）
- [x] `PageResult.java`：單頁的分數與各關鍵字次數（Stage 1 輸出）
- [x] ~~`SiteResult.java`~~：改放在 `tree/`（因為用到 `WebTree`，放這裡會違反依賴方向）
- [x] ~~`SearchResponse.java`~~：改放在 `engine/`（理由同上）

## Stage 完成後

- [x] 在 `notes/stage-reports/` 撰寫 `stage0-report.md`（複製 `TEMPLATE.md`）
