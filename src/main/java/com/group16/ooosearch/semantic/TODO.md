# semantic/ — 語意分析與關鍵字擴充

- **Stage**：4
- **負責人**：蔡妙青
- **期限**：11/12
- **依賴**：`model/`、`search/`

## 要建立的檔案

- [ ] `Tokenizer.java`：用 HanLP 或 jieba-analysis 斷詞，**加自訂詞典**（塔托巴組合、鴻上集團…）；替代方案為 n-gram
- [ ] `TfIdfAnalyzer.java`：自己實作詞頻、TF-IDF、共現次數（`HashMap` 計數、`PriorityQueue` 取 Top-K）
- [ ] `KeywordExpander.java`：介面與實作，把高相關詞以較低權重加入關鍵字集合
- [ ] 過濾停用詞（`data/stopwords.txt`）
- [ ] 設定相關度門檻、限制迭代次數，避免擴充偏題（例如「假面騎士W」）

## 延伸（選做）

- [ ] 把共現關係建成 Graph（鄰接表）

## Stage 完成後

- [ ] 在 `notes/stage-reports/` 撰寫 `stage4-report.md`（複製 `TEMPLATE.md`）
