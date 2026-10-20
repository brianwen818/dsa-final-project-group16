# cli/ — 各 Stage 的命令列 Demo

每個 Stage 各自一個進入點，方便單獨展示與測試。

| 檔案 | Stage | 負責人 | 期限 |
|---|---|---|---|
| [x] `Stage1Main.java` | 1 | 温亮達 | 10/22 |
| [ ] `Stage2Main.java` | 2 | 廖若羽 | 10/27 |
| [ ] `Stage3Main.java` | 3 | 劉又綺、鄭年芮 | 11/01 |
| [ ] `Stage4Main.java` | 4 | 蔡妙青 | 11/12 |

## 離線工具

| 檔案 | 用途 | 負責人 |
|---|---|---|
| [x] `KeywordSeedAnalyzer.java` | 從日版、台版維基百科產生候選關鍵字，輸出到 `data/keyword-candidates/`；依 `data/concepts.txt` 產生 `data/keywords.txt` | 温亮達 |
| [x] `ConceptWeightCalculator.java` | 計算每個概念的權重（由 `KeywordSeedAnalyzer` 呼叫） | 温亮達 |
