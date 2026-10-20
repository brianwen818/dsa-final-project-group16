# scoring/ — 關鍵字計數與單頁計分

- **Stage**：1
- **負責人**：温亮達
- **期限**：10/22
- **依賴**：`model/`、`fetch/`

## 要建立的檔案

- [x] `PageScorer.java`：介面（Stage 0 完成，假實作為 `FakePageScorer`）
- [x] `WordCounter.java`：計算關鍵字（含同義詞）出現次數（HW3），長的寫法優先、英文字界
- [x] `SynonymTable.java`：用 `HashMap` 把每種寫法對應到關鍵字（同義詞改寫在 `data/keywords.txt` 中）
- [x] `TextNormalizer.java`：全形轉半形、移除漢字假名間空白、忽略大小寫
- [x] `KeywordPageScorer.java`：`PageScorer` 的正式實作，`分數 = Σ(權重 × 次數)`，`Stage1Main` 已改用

## 延伸（選做）

- [ ] Aho-Corasick 多關鍵字比對，並與 Boyer-Moore 比較效能
- [x] 同義詞重疊：「OOO」是「假面騎士OOO」的子字串，合併計數時要避免重複計算
- [ ] 標題加權、依頁面長度正規化
- [ ] 比較「數原始 HTML」與「只數正文」的結果差異（寫進報告）

## Stage 完成後

- [x] 在 `notes/stage-reports/` 撰寫 `stage1-report.md`（複製 `TEMPLATE.md`）
