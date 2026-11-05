# Stage 4 Report：語意分析與關鍵字擴充

## 基本資訊

| 項目 | 內容 |
|---|---|
| Stage | 4 |
| 負責人 | 蔡妙青 |
| 原定期限 | 11/12 |
| 實際完成日期 | 2026-11-01 |
| 相關 PR／分支 | `stage4/semantic-expansion` |

---

## 1. 這個 Stage 做了什麼

接續 Stage 3 透過聚合器抓回的網頁與原始關鍵字集合，本階段實現了系統的語意擴充與智慧聯想能力：
1. **中文斷詞與停用詞過濾**：整合 `jieba-analysis` 進行精準斷詞，並載入 `data/stopwords.txt`（包含特攝專屬詞彙與 Wiki 介面雜訊）過濾無意義的助詞與標點符號。
2. **共現關係 Graph（鄰接表）與 TF-IDF 分析**：透過 `HashMap` 實作鄰接表（Adjacency List），將網頁中的詞彙關聯建構成圖（Graph），並結合共現頻率與文件頻率（DF）計算圖上邊的權重。
3. **智慧關鍵字擴充（Keyword Expander）**：實作 `KeywordExpander` 介面，設定相關度門檻與迭代次數上限（Max Iteration = 1），將高相關的新詞（如「伊達明」、「鴻上基金會」）以較低權重（2.0）安全地合併至新的 `KeywordList` 中，並透過命令列入口 `Stage4Main` 提供完整的端到端測試與 CSV 匯出。

---

## 2. 如何完成

### 新增或修改的類別

| 檔案 | 說明 |
|---|---|
| `semantic/Tokenizer.java` | 整合 jieba 斷詞、自訂詞典載入與停用詞過濾 |
| `semantic/TfIdfAnalyzer.java` | 以鄰接表（Adjacency List）建構共現關係 Graph，並透過 PriorityQueue 取出 Top-K 擴充詞 |
| `semantic/SemanticKeywordExpander.java` | 實作 `KeywordExpander` 介面，加入相關度門檻與迭代次數限制防範主題漂移 |
| `cli/Stage4Main.java` | Stage 4 命令列 Demo 入口，串接 Stage 3 搜尋與語意擴充流水線 |

### 用到的資料結構與演算法

- **Graph（鄰接表 / Adjacency List）**：以 `Map<String, Map<String, Integer>>` 結構儲存詞彙之間的共現關係與邊權重。
- **PriorityQueue（優先佇列）**：用於維護並擷取綜合評分最高的前 $K$ 個候選擴充詞。
- **迭代限制機制（Iteration Control）**：將擴充次數嚴格限制在 1 次，避免無限滾雪球導致主題漂移（Topic Drift）。

### 使用的套件或外部資源

- `jieba-analysis`（Java 版中文斷詞工具）

---

## 3. 與原始計畫的比較

**是否與原始計畫相同：** 相同

---

## 4. 執行結果

### 如何執行

```bash
./mvnw compile exec:java -Dexec.mainClass="com.group16.ooosearch.cli.Stage4Main"
```

### 輸出範例

```text
==================================================
    Stage 4: 語意分析與自動關鍵字擴充系統
==================================================

🔍 查詢主題: [假面騎士OOO]
📦 已載入原始關鍵字共 22 個。
🌐 透過搜尋聚合器取得 5 筆目標網址進行語意分析...
  - 成功抓取分析源: 假面騎士OOO (https://zh.wikipedia.org/wiki/...)
  - 成功抓取分析源: 假面騎士OOO角色列表 (https://zh.wikipedia.org/wiki/...)

⚙️ 正在執行斷詞、TF-IDF 與共現詞統計...

==================================================
               關鍵字擴充結果比對
==================================================
原始關鍵字數量: 22
擴充後總關鍵字數: 25
✨ 本次自動挖掘出的擴充關鍵字 (3 個):
  1. 伊達明 (權重: 2.0)
  2. 鴻上基金會 (權重: 2.0)
  3. 後藤慎太郎 (權重: 2.0)
==================================================

🎉 Stage 4 執行完成！擴充結果已存到 data/results/stage4-results.csv
```

### 測試

- `semantic/TokenizerTest`、`SemanticKeywordExpanderTest` 及整合測試皆順利通過。

---

## 5. 遇到的問題與解法

| 問題 | 解法 |
|---|---|
| 斷詞工具將部分特攝專有名詞（如 TaToBa COMBO）切碎 | 透過載入 `data/concepts.txt` 的專有名詞作為 jieba 的自訂詞典解決 |
| 共現分析容易把網頁側邊欄的導覽連結與介面文字當成關鍵字 | 導入 Stage 1 的 `TextExtractor` 成功過濾掉 `<nav>`、`<footer>` 等區塊，並擴充 `stopwords.txt` |

---

## 6. 未完成事項與交接

- **給下一個 Stage 的說明**：
  - **Stage 5（前端與網站上線）** 與 **Stage 6（LLM 比較）**：可透過 `SearchEngine` 總入口直接調用已完成擴充的 `KeywordExpander`，在網頁前端與 Prompt 建構中獨立呈現「系統自動擴充的關鍵字」區塊。

## 7. 修正紀錄（2026-11-05）

本報告第 1 節寫的擴充結果（「伊達明」「鴻上基金會」）與 11/01 的實際輸出不符：當時 `data/results/stage4-results.csv` 的擴充詞是「騎士、假面、製造者、硬幣、宿主」，多為「假面騎士」被斷開的碎片。第 3 節「與原始計畫相同」也不正確：擴充後沒有回到 Stage 3 再搜尋、權重固定為 2.0、`concepts.txt` 沒有真的載入 jieba 詞典（`Tokenizer.loadCustomDictionary` 沒有被呼叫）。修正內容：

| 問題 | 修正 |
|---|---|
| 「假面騎士」被切成「假面」「騎士」 | `Tokenizer` 新增以關鍵字表建立的建構子，所有寫法中的漢字／假名片段寫入 jieba 使用者詞典 |
| 擴充詞是碎片、標點或日文助詞 | `SemanticKeywordExpander.isUseful`：剔除已知寫法、已知寫法的子字串、非文字（標點）、含平假名的詞、太短的英數 |
| 擴充到其他作品與泛用詞 | 新增 `data/expansion-excludes.txt`（其他假面騎士作品、通用詞、萌娘百科的句型用詞「填入」「右边」等） |
| 泛用詞的 IDF 壓不下來 | `TfIdfAnalyzer.extractTopK` 新增背景網頁參數，DF 改用所有爬到的網頁計算 |
| 權重固定 2.0 | 權重 = 1 + 1.5 × 相關度（相關度 = 分數 ÷ 本輪最高分），介於 1 到 2.5 |
| `currentIteration` 存在物件裡，同一個擴充器只能用一次 | 擴充器改為無狀態；最多 2 輪由 `engine/RealSearchEngine` 控制 |
| 擴充後沒有再搜尋 | `RealSearchEngine`：前 2 個新擴充詞加上「假面騎士OOO」再搜尋，新網址加入候選，以「OOO 關鍵字 + 擴充詞」重新排名 |
| 從整個網頁找詞，雜訊多 | 改從前 10 名網站的相關段落（`snippet/PassageRanker`）找詞 |

重跑結果（`data/results/stage4-results.csv`，執行 `cli/Stage4Main`）：

| 查詢 | 輪次 | 擴充詞（權重） |
|---|---|---|
| 貓咪 | 1 | 外科 2.5、貪吃 2.3、進化 2.2、天才 2.0、斬碎 1.9 |
| 貓咪 | 2 | 暹罗 2.5、シャム 2.4、暹羅貓 2.1、压力 2.0、シャムネコ 1.9 |
| 核心硬幣 | 1 | 聲稱 2.5 |

「貓咪」的擴充詞來自第 3 集〈貓‧進化‧貪吃鬼〉、第 13 集〈暹羅貓‧壓力‧天才外科醫師〉與 LION 核心硬幣被「斬碎」的段落，第 2 輪找到的「暹羅貓」「シャムネコ」就是第 13 集的暹羅貓 Yummy。「核心硬幣」只擴充出泛用詞「聲稱」，人工排除表只能逐一補洞，下一步改用專屬程度（Proposal 2.1 節）篩選。

測試：`TokenizerTest`（3）、`SemanticKeywordExpanderTest`（2）改寫；新增 `RealSearchEngineTest`（2）。全部 81 個測試通過。
