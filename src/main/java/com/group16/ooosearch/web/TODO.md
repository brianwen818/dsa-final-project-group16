# web/ — Spring Boot 網站後端

- **Stage**：5
- **負責人**：劉又綺、鄭年芮（程式組完成後支援）
- **期限**：11/19
- **依賴**：只透過 `engine/SearchEngine` 呼叫，不直接使用底層類別
- **說明**：只有這個 package 可以使用 Spring。架構參考 HW4 Bonus 的 BMI 專案。

## 要建立的檔案

- [x] `OooSearchApplication.java`：Spring Boot 進入點（Stage 0 已建立最小版本）
- [ ] `controller/SearchController.java`：處理搜尋請求
- [ ] `service/SearchService.java`：呼叫 `SearchEngine`（開發前端時先用 `FakeSearchEngine`）

## 其他工作

- [ ] 前端頁面（見 `src/main/resources/static/TODO.md`）
- [ ] 部署到 Render 免費方案，或本機加 Cloudflare Tunnel

## Stage 完成後

- [ ] 在 `notes/stage-reports/` 撰寫 `stage5-report.md`（複製 `TEMPLATE.md`）
