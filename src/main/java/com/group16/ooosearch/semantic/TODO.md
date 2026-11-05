# semantic/ — 語意分析與關鍵字擴充

- **Stage**：4
- **負責人**：蔡妙青
- **期限**：11/12
- **依賴**：`model/`、`search/`

## 要建立的檔案

- [x] `Tokenizer.java`：用 HanLP 或 jieba-analysis 斷詞，**加自訂詞典**（塔托巴組合、鴻上集團…）；替代方案為 n-gram
- [x] `TfIdfAnalyzer.java`：自己實作詞頻、TF-IDF、共現次數（`HashMap` 計數、`PriorityQueue` 取 Top-K）
- [x] `KeywordExpander.java`：介面（Stage 0 完成，假實作為 `FakeKeywordExpander`）
- [x] `KeywordExpander` 的正式實作：把高相關詞以較低權重加入關鍵字集合
- [x] 過濾停用詞（`data/stopwords.txt`）
- [x] 設定相關度門檻、限制迭代次數，避免擴充偏題（例如「假面騎士W」）

## 延伸（選做）

- [x] 把共現關係建成 Graph（鄰接表）
- [x] 改從 `snippet/` 挑出的相關段落找擴充詞，而不是整個網頁（2026-11-05）
- [x] 修正（2026-11-05）：關鍵字表寫法加入 jieba 詞典；剔除碎片、標點、日文助詞碎片與 `data/expansion-excludes.txt` 中的其他作品與泛用詞；權重改為 1 + 1.5 × 相關度；DF 改用所有爬到的網頁計算；擴充器改為無狀態，最多 2 輪由 `engine/RealSearchEngine` 控制

## Stage 完成後

- [x] 在 `notes/stage-reports/` 撰寫 `stage4-report.md`（複製 `TEMPLATE.md`）
