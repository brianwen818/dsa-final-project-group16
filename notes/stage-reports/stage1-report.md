# Stage 1 Report：單一網頁排名

## 基本資訊

| 項目 | 內容 |
|---|---|
| Stage | 1 |
| 負責人 | 温亮達 |
| 原定期限 | 10/22 |
| 實際完成日期 | 2026-10-20 |
| 相關 PR／分支 | 待提交 |

## 1. 這個 Stage 做了什麼

給一組關鍵字（含權重與同義詞）與一組網址，抓取每個網頁、只保留正文、計算每個關鍵字的出現次數與分數，再依分數排名，輸出名次、分數與各關鍵字次數（也存成 CSV）。

另外，**關鍵字與權重不是憑感覺決定**，而是從日版、台版維基百科統計出來的：人工決定「收哪些關鍵字、每個關鍵字有哪些寫法」，程式計算權重。

## 2. 如何完成

### 新增或修改的類別

| 檔案 | 說明 |
|---|---|
| `fetch/TextExtractor` | 用 Stack 抽取正文（見下方） |
| `fetch/MediaWikiClient` | MediaWiki API（取得條目 HTML、跨語言連結），供關鍵字分析使用；Stage 3 可沿用 |
| `fetch/PageFetcher` | 新增 `fetchText()`（JSON 等非 HTML 內容也走快取）；User-Agent 依 Wikimedia 規範附上專案網址 |
| `scoring/TextNormalizer` | 全形轉半形、移除漢字與假名之間的空白、轉小寫 |
| `scoring/SynonymTable` | `HashMap<寫法, 關鍵字>`（多對一）；同一寫法屬於兩個關鍵字時拋出例外 |
| `scoring/WordCounter` | 計算每個關鍵字（含所有寫法）的次數 |
| `scoring/KeywordPageScorer` | `PageScorer` 的正式實作：`分數 = Σ(權重 × 次數)` |
| `model/Keyword`、`KeywordList` | `Keyword` 新增其他寫法；`KeywordList.fromFile` 改讀 `權重 \| 關鍵字 \| 其他寫法` |
| `cli/KeywordSeedAnalyzer` | 產生候選關鍵字清單；依 `data/concepts.txt` 呼叫 `ConceptWeightCalculator` 產生 `data/keywords.txt` |
| `cli/ConceptWeightCalculator` | 計算每個關鍵字的權重 |
| `cli/Stage1Main` | 讀取網址檔與關鍵字檔、抓取、計分、排名、輸出 |
| `data/concepts.txt`、`keywords.txt`、`stage1-urls.txt` | 概念表（人工）、關鍵字表（程式產生）、測試網址；刪除 `synonyms.txt` |

### 用到的資料結構與演算法

- **Stack（HW5）**：`TextExtractor` 遇到 `nav`、`footer`、`svg` 開始標籤就 push、結束標籤就 pop，Stack 不是空的時候不收文字；HTML 沒有正確關閉時，一路 pop 到對應的開始標籤。`script`、`style`、`title` 等內容可能含有 `<`，直接跳到結束標籤。
- **HashMap**：`SynonymTable` 把每種寫法對應到關鍵字；`PageCache` 用網址當 key。
- **字串搜尋（HW3）**：`WordCounter` 用 `indexOf` 找出每種寫法的位置。
- **長的寫法優先＋`boolean[]` 標記**：所有寫法依長度由長到短比對，比對到的位置標記為已使用，較短的寫法不能再使用。因此「假面騎士OOO」不會再被算成一次「OOO」，排除詞「安卡拉」也不會被算成「安卡」。
- **字界判斷**：寫法開頭或結尾是英文字母、數字時，前後一個字元不能也是英文字母或數字（避免 Birth 比對到 Birthday）。中日文沒有空白分詞，不做這個檢查。
- **排序**：依分數由高到低排序（HW4 的有序串列概念）。

### 關鍵字與權重的產生過程

1. **候選詞**（`KeywordSeedAnalyzer`）：從日版「仮面ライダーオーズ/OOO」、台版「假面騎士OOO」＋「假面騎士OOO角色列表」中，取條目名稱、章節標題、粗體字、定義清單，以及「名稱（日文原名）」開頭的段落。
2. **候選詞分數**：以其他 6 部作品（電王、Decade、W、Fourze、Wizard、Build）的條目當背景。
   - `專屬程度 = OOO 頻率 / (OOO 頻率 + 背景平均頻率)`
   - `分數 = (1 + ln 次數) × 專屬程度 × 導言加權（×1.2）`
3. **人工整理概念表**（`data/concepts.txt`）：22 個關鍵字＋1 個排除詞，關鍵字用台版用語，其他寫法包含日文、英文（Fandom）與簡體（萌娘百科），並寫上依據（在兩版候選清單的名次）。
4. **權重**（`ConceptWeightCalculator`）：每個關鍵字用 `WordCounter` 合併計數所有寫法（與正式計分同一套規則），在兩個語言各算一次分數，各除以該語言最高分後取平均得 `s`，**`權重 = 1 + 4s`**。結果：假面騎士OOO 5.0、核心硬幣 4.5、安酷 4.4、貪婪 4.3 … 泉比奈 2.6，明細見 `data/keyword-candidates/concept-weights.md`。

### 使用的套件或外部資源

- Jsoup（抓網頁；`Parser.unescapeEntities` 還原 HTML 實體）
- Jackson 3（解析 MediaWiki API 的 JSON；Spring Boot 已內含，`pom.xml` 明確列出）
- MediaWiki API（日版、台版維基百科，免費）

## 3. 與原始計畫的比較

原始計畫見 [README 第 4 節](../../README.md#4-各-stage-目標做法與負責人)。

**是否與原始計畫相同：** 部分不同（核心做法相同，關鍵字的產生方式與檔案格式不同）

### 改動紀錄

| 改動日期 | 原始計畫 | 實際做法 | 改動原因 | 是否影響其他 Stage |
|---|---|---|---|---|
| 2026-10-20 | 關鍵字與權重人工決定（初版 6 個） | 從日版、台版維基統計：人工決定關鍵字與寫法，程式計算權重 | 讓權重有依據、可重現，報告可以說明每個權重的來源 | 無；Stage 4 可沿用分析程式 |
| 2026-10-20 | 用一般的 TF-IDF 計算專屬程度 | 改為比較出現頻率（專屬程度） | 其他作品的條目常因客串電影提到 OOO 的核心詞，IDF 只看「有沒有出現」，使コアメダル（第 109 名）、仮面ライダーオーズ（第 119 名）被壓到很後面 | 無 |
| 2026-10-20 | 台版只用「假面騎士OOO」條目 | 加入「假面騎士OOO角色列表」；背景作品也加入各自的角色列表 | 台版把角色介紹拆成獨立條目，只用主條目抓不到角色名稱；背景也加入以維持比較公平 | 無 |
| 2026-10-20 | `keywords.txt`（名稱 權重）與 `synonyms.txt` 分開 | 合併：`concepts.txt`（人工）產生 `keywords.txt`（`權重 \| 關鍵字 \| 其他寫法`），刪除 `synonyms.txt` | 兩個檔案分開維護容易對不上；權重、寫法、依據放在同一處 | `KeywordList.fromFile` 格式改變；其他 Stage 讀關鍵字時直接用 `KeywordList.fromFile` 即可 |
| 2026-10-20 | アンク譯為「安卡」 | 顯示名稱用「安酷」，安卡列為其他寫法 | 台版維基使用「安酷」；組內決定採用 | 無 |
| 2026-10-20 | `SynonymTable` 讀取 `synonyms.txt` | `SynonymTable` 由 `KeywordList` 建立 | 同義詞已併入關鍵字表 | 無 |
| 2026-10-20 | 無 | 新增排除詞（權重 0），例如「安卡拉」 | 「安卡」會誤判「安卡拉」 | 無 |
| 2026-10-20 | `TextExtractor` 跳過 `<template>` | 不跳過 `<template>` | 萌娘百科把整篇文章放在 `<template>` 裡，跳過的話分數為 0 | 無 |
| 2026-10-20 | 測試網址包含商品頁 | 不包含 | 商品網站（Amazon、Yahoo 購物、駿河屋、Premium Bandai）回傳 403／500／503 或內容由 JavaScript 產生 | Stage 3：搜尋結果中的商品頁可能抓不到內容 |
| 2026-10-20 | JDK 21 以上 | JDK 25 以上 | `pom.xml` 改為 25，文件同步更新 | 所有人：需安裝 JDK 25 以上 |

## 4. 執行結果

### 如何執行

```bash
./mvnw compile exec:java -Dexec.mainClass=com.group16.ooosearch.cli.Stage1Main
# 修改 data/concepts.txt 後重新產生關鍵字表：
./mvnw compile exec:java -Dexec.mainClass=com.group16.ooosearch.cli.KeywordSeedAnalyzer
```

### 輸出（2026-10-20，完整結果見 `data/results/stage1-results.csv`）

| 名次 | 分數 | 網頁 | 主要關鍵字次數 |
|---|---|---|---|
| 1 | 7384.9 | 日版維基（參考） | 假面騎士OOO 469、貪婪 185、安酷 180 |
| 2 | 2135.3 | 台版維基（參考） | 假面騎士OOO 105、核心硬幣 97 |
| 3 | 1654.3 | Kamen Rider Wiki（英文） | Yummy 99、假面騎士OOO 89、貪婪 43 |
| 4 | 545.8 | 萌娘百科（簡體） | 假面騎士OOO 26、慾望 24、安酷 20 |
| 5 | 464.5 | 巴哈姆特心得文（由慾望邁向明日） | 慾望 40、貪婪 22、火野映司 16 |
| 6 | 141.9 | PTT 特攝板心得文 | 火野映司 18、假面騎士OOO 8 |
| 7 | 103.2 | 巴哈姆特動畫瘋資料庫 | 假面騎士OOO 14、TaToBa COMBO 4 |
| 8 | 94.2 | 對照組：台版維基「假面騎士W」 | 假面騎士OOO 18、Yummy 1 |
| 9 | 17.6 | 對照組：台版維基「安卡拉」 | 安酷 4（誤判，見下方） |
| 10 | 13.8 | 巴哈姆特心得文（假面騎士OOO心得） | 假面騎士OOO 2、慾望 1 |

觀察：
- 以 OOO 為主題的網頁都排在兩個對照組之前，只有一篇內容很短的心得文例外。
- 英文的 Fandom、簡體的萌娘百科也能正確計分，因為關鍵字表包含英文與簡體寫法。
- **長頁面明顯占便宜**：日版維基的分數是萌娘百科的 13 倍以上，主要是因為頁面長。需要依頁面長度正規化（列為延伸）。
- 「假面騎士W」的條目提到「假面騎士OOO」18 次（客串電影），分數接近巴哈姆特資料庫頁。
- **「安卡」容易誤判**：安卡拉的條目中有「安卡科技大學」「安卡鐵」「MKE安卡華高古隊」等安卡拉的簡稱，排除詞「安卡拉」擋不住，被算成 4 次「安酷」。

### 測試

32 個測試全部通過（`Tests run: 32, Failures: 0, Errors: 0`），都不連網路：

- `fetch/TextExtractorTest`（9）：跳過含 `<` 的 script／style、title、巢狀 nav、未關閉標籤、保留 template 內容、註解、實體還原、大小寫
- `scoring/WordCounterTest`（9）：長的寫法優先、同義詞加總、排除詞、大小寫與全形、英文字界、中日文不需字界、漢字間空白、同一寫法屬於兩個關鍵字時報錯
- `scoring/KeywordPageScorerTest`（2）：只數正文的分數與次數、排除詞不列出也不加分
- `model/KeywordListTest`（4）：新格式、格式錯誤、專案的 `keywords.txt` 權重介於 0～5
- Stage 0 原有的 8 個測試

## 5. 遇到的問題與解法

| 問題 | 解法 |
|---|---|
| 維基的內部連結大多是年份、主題曲、其他作品，不適合當候選詞 | 改用章節標題、粗體字、定義清單 |
| 一般 IDF 壓低 OOO 的核心詞 | 改為比較出現頻率 |
| 台版主條目沒有角色名稱 | 加入角色列表條目；新增「名稱（日文原名）」開頭段落的候選詞來源 |
| 台版內文多用英文名（Uva、Kazari、Greeed、Yummy），中文譯名只出現一次 | 日中對照改用全部候選詞；概念表的其他寫法加入英文 |
| 「OOO」被重複計算在「假面騎士OOO」裡 | 長的寫法優先、標記已使用的位置 |
| 「安卡」誤判「安卡拉」 | 排除詞（權重 0）；但安卡拉的其他簡稱仍會誤判，見未完成事項 |
| 萌娘百科分數為 0 | 文章放在 `<template>` 裡，`TextExtractor` 不再跳過 `<template>` |
| Python 腳本透過 stdin 讀取中文時編碼錯誤 | 改用 Edit 工具修改程式碼 |

## 6. 未完成事項與交接

- **未完成或延後的項目：**
  - 依頁面長度正規化、標題加權
  - Aho-Corasick 多關鍵字比對（與目前的逐一 `indexOf` 比較效能）
  - 「安卡」的其他誤判（安卡科技大學、安卡鐵）：考慮拿掉「安卡」這個寫法，或在 Stage 4 加入前後文判斷
  - 「貪婪」「慾望」在一般網頁也是常見詞，目前依統計保留，Stage 3 用真實搜尋結果時再觀察
  - 全組再看過一次 `data/concepts.txt`
- **給下一個 Stage 的說明：**
  - **Stage 2（廖若羽）**：每個節點直接用 `new KeywordPageScorer().score(page, keywords)`，關鍵字用 `KeywordList.fromFile(Path.of("data", "keywords.txt"))`。`PageResult.getKeywordCounts()` 可用來印樹上每個節點的次數。同一個 `KeywordPageScorer` 重複使用即可，它會快取同義詞表。
  - **Stage 3**：`fetch/MediaWikiClient` 可直接用來實作 `MediaWikiProvider`。商品網站大多擋爬蟲或需要 JavaScript。
  - **Stage 4（蔡妙青）**：`KeywordSeedAnalyzer` 的候選詞抽取與專屬程度計算可以參考或沿用；擴充出的關鍵字可以用 `new Keyword(名稱, 較低權重, 其他寫法)` 加入 `KeywordList`。
