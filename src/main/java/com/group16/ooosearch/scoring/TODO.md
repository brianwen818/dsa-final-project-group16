# scoring/ — 關鍵字計數與單頁計分

- **Stage**：1
- **負責人**：温亮達
- **期限**：10/22
- **依賴**：`model/`、`fetch/`

## 要建立的檔案

- [x] `PageScorer.java`：介面（Stage 0 完成，假實作為 `FakePageScorer`）
- [ ] `WordCounter.java`：計算關鍵字出現次數（HW3）
- [ ] `SynonymTable.java`：用 `HashMap` 把同義詞對應到標準名稱，讀取 `data/synonyms.txt`
- [ ] `PageScorer` 的正式實作：`分數 = Σ(權重 × 次數)`，完成後把 `cli/Stage1Main` 的 `FakePageScorer` 換掉

## 延伸（選做）

- [ ] Aho-Corasick 多關鍵字比對，並與 Boyer-Moore 比較效能
- [ ] 同義詞重疊：「OOO」是「假面騎士OOO」的子字串，合併計數時要避免重複計算
- [ ] 標題加權、依頁面長度正規化
- [ ] 比較「數原始 HTML」與「只數正文」的結果差異（寫進報告）

## Stage 完成後

- [ ] 在 `notes/stage-reports/` 撰寫 `stage1-report.md`（複製 `TEMPLATE.md`）
