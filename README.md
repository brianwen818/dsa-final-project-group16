# 假面騎士OOO 搜尋引擎 — Data Structure Final Project Group 16

2026 Fall 資料結構期末專案（第 16 組）

> 本文件整理第一週以來的討論結果，作為全組的開發依據。原始討論見 [notes/first-week-discussion.md](notes/first-week-discussion.md)。

---

## 目錄

1. [專案主題與目標](#1-專案主題與目標)
2. [技術選擇與原則](#2-技術選擇與原則)
3. [組員分工與時程](#3-組員分工與時程)
4. [各 Stage 目標、做法與負責人](#4-各-stage-目標做法與負責人)
5. [協作方式](#5-協作方式)
6. [Repo 架構](#6-repo-架構)
7. [開發環境與執行方式](#7-開發環境與執行方式)
8. [風險與注意事項](#8-風險與注意事項)
9. [進度追蹤](#9-進度追蹤)

---

## 1. 專案主題與目標

### 題目：Let's Beat Google!

課程要求每組自己實作一個簡化版搜尋引擎，分成 6 個 Stage 逐步升級，完成的 Stage 越多，成績門檻越高（Stage 1 30%+ … Stage 6 100%+）。

### 本組主題：假面騎士OOO

使用者輸入與「假面騎士OOO」相關的關鍵字，系統會：

1. 從網路上找出候選網站
2. 爬取網站及其子頁面，依關鍵字出現情形計分
3. 重新排名，比 Google 的原始排名更貼近「資料完整度」
4. 自動擴充相關關鍵字（如「塔托巴組合」、「鴻上集團」）再次搜尋
5. 以網站形式公開提供服務，並與 ChatGPT、Gemini、DeepSeek 等 LLM 的回答比較

### 為什麼選這個主題

- **童年回憶!!!**
- ~~**其他不重要理由:**~~
  - **資料來源多**：有中文維基、Kamen Rider Wiki（Fandom）、萌娘百科、巴哈姆特等專門內容可爬。
  - **名稱中日英混用**：如「OOO／歐茲／オーズ」、「安卡／Ankh／アンク」，適合在 Stage 1 做同義詞合併，在 Stage 4 展示語意擴充。
  - **LLM 容易答錯**：特攝作品的冷門細節（核心硬幣種類與顏色、中文譯名）常被 LLM 亂答，Stage 6 可以凸顯檢索結果的價值。

### 整體流程

```
網頁爬取 → 關鍵字計分 → 排序 → 網站樹狀結構 → 自主搜尋網址 → 關鍵字擴充 → 上線 → 與 LLM 比較
 Stage 1     Stage 1     Stage 1    Stage 2        Stage 3        Stage 4     Stage 5   Stage 6
```

### 本專案的兩個目標

1. **用到課堂所學**：HW2–HW6 的繼承、字串搜尋、有序串列、Stack、樹都要在專案中用到（對照見 [4.8](#48-課堂作業對照)）。
2. **在課堂所學上延伸**：例如用 BFS 自動建樹取代手動 `addChild`、用 Aho-Corasick 一次比對多個關鍵字、自己實作 TF-IDF。

---

## 2. 技術選擇與原則

### 程式語言：全部使用 Java

- 課堂使用 Java，作業的類別（`Keyword`、`WordCounter`、`WebPage`、`WebNode`、`WebTree`）可以直接延用。
- Stage 4 的中文處理 Java 也有現成工具（HanLP、jieba-analysis），**不需要另外使用 Python**。
- TF-IDF、共現次數等**自己用 `HashMap`、`PriorityQueue` 實作**，比呼叫 Python 套件更能展示資料結構。
- 例外：Stage 6 的評估分析（畫圖、統計）可以用 Python 腳本，因為不屬於系統本身。
- 延伸選項：若 Stage 4 完成後還有時間，可以把 Word Embedding 做成獨立的 Python 服務，讓 Java 透過 HTTP 呼叫。列為加分項目，不是必做。

### 主要套件

| 用途 | 套件 |
|---|---|
| 專案建置、套件管理 | Maven |
| 抓網頁、解析 HTML | Jsoup |
| 中文斷詞 | HanLP 或 jieba-analysis（擇一，需加自訂詞典） |
| 日文斷詞（視需要） | Kuromoji |
| 抓 Google 對照組（僅離線使用） | Selenium（Java 版） |
| 網站後端 | Spring Boot |
| 測試 | JUnit 5 |

### 原則：不花錢

除了組員個人已有的 AI 訂閱外，**不購買任何付費 API 或服務**。

| 需求 | ❌ 不採用（付費或已停止服務） | ✅ 採用（免費） |
|---|---|---|
| 搜尋網址 | Google Custom Search API（已不接受新用戶，2027/1/1 關閉）、Bing Search API（已退役）、SerpAPI／Brave（免費額度太少或須綁卡） | DuckDuckGo HTML 版、MediaWiki 搜尋 API、Selenium 離線抓 Google 結果 |
| 部署 | Railway（無長期免費方案）、GCP（須綁卡） | Render 免費方案，或本機執行加 Cloudflare Tunnel |
| LLM 比較 | 付費 API | 在已訂閱的網頁介面手動提問；需要自動化時用 Gemini API 免費額度 |

---

## 3. 組員分工與時程

### 分工

| 組別 | 成員 | 負責 |
|---|---|---|
| 程式組 | 温亮達 | Stage 0、Stage 1 |
| 程式組 | 廖若羽 | Stage 2 |
| 程式組 | 蔡妙青 | Stage 4 |
| 測試、前後端與其他工作組 | 劉又綺、鄭年芮 | Proposal、Stage 3、Stage 5、Stage 6（兩人自行協調） |
| 尚未分配 | — | PPT、上台報告 |

工作組的順序：
1. **11/1 前**：先寫 Proposal。
2. **11/1 後**（或程式組完成 Stage 0–2 後）：一起做 Stage 3，再接 Stage 5、6。

> 程式組完成自己的 Stage 後，請支援 Stage 5、6 與 PPT（見 [8.4](#84-時程與分工)）。

### 時程

| 項目 | 期限 |
|---|---|
| Stage 0 & 1 | 10/22 |
| Proposal 主題介紹 | 10/22 |
| Stage 2 | 10/27 |
| Stage 3 | 11/01 |
| **Proposal 最終版繳交** | **11/05** |
| Stage 4 | 11/12 |
| Stage 5 | 11/19 |
| Stage 6 | 11/26 |
| PPT 完稿 | 12/10 |
| **最終上台 Demo** | **12/17** |

> 完成當前 Stage 後請直接接著做下一個，不要拖延。

### Proposal 內容要求

1. Introduction：主題與動機
2. Search tricks：計分公式
3. System design：Class Diagram
4. Schedule：各 Stage 如何、何時完成
5. Challenges：需要用到、但不容易自學的技術

本文件的第 1、4、6 節可以直接當作 Proposal 的素材。

---

## 4. 各 Stage 目標、做法與負責人

### 4.0 Stage 0：專案骨架

| | |
|---|---|
| **負責人** | 温亮達 |
| **期限** | 10/22 |
| **目標** | 建好所有人共用的基礎，讓後續各 Stage 能同時開工 |
| **狀態** | ✅ 已完成（2026-10-20），報告見 [stage0-report.md](notes/stage-reports/stage0-report.md) |

**要做的事：**

1. [x] 建立 Maven 專案、`pom.xml`、`.gitignore`，以及第 6 節的 package 結構。
2. [x] 把 HW4、HW6 的 `Keyword`、`KeywordList`、`WebPage` 整理進 `model/`。
3. [x] **定好各層對外的介面**（見 [5.1](#51-介面先行)），並為每個介面寫一個回傳假資料的實作。
4. [x] 實作 `fetch/`：抓網頁（`PageFetcher`）與本機快取（`PageCache`）。
5. [x] 在 `data/` 放好關鍵字表、同義詞表的初版（Stage 1 已改為 `concepts.txt`＋自動產生的 `keywords.txt`）。
6. [x] 更新本 README 的執行方式。

與原計畫不同的地方：`SiteResult` 改放 `tree/`、`SearchResponse` 改放 `engine/`；`WebNode`／`WebTree` 的資料結構與 `SearchProvider` 介面在 Stage 0 先建立；先建立最小的 `web/OooSearchApplication`。原因見 Stage 0 報告。

### 4.1 Stage 1（30%+）：單一網頁排名

| | |
|---|---|
| **負責人** | 温亮達 |
| **期限** | 10/22 |
| **輸入** | 一組關鍵字（含權重）與一組網址 |
| **輸出** | 每個網址的名次、分數、各關鍵字出現次數 |
| **主要 package** | `fetch/`、`scoring/`、`cli/Stage1Main`、`cli/KeywordSeedAnalyzer` |
| **狀態** | ✅ 已完成（2026-10-20），報告見 [stage1-report.md](notes/stage-reports/stage1-report.md) |

**做法：**

1. 用 `PageFetcher` 抓取每個網址（自動使用快取）。
2. `TextExtractor` **用 Stack（HW5）跳過 `<script>`、`<style>`、`<nav>`、`<footer>` 等區塊**，只保留正文。
3. `WordCounter` 計算每個關鍵字（含所有同義詞）的出現次數：
   - `SynonymTable` 用 `HashMap` 把每種寫法對應到關鍵字（多對一）
   - **長的寫法先比對**，比對到的位置用 `boolean[]` 標記，避免「假面騎士OOO」裡的「OOO」被重複計算
   - 忽略大小寫、全形半形；英文寫法要求前後是字界（避免 Birth 比對到 Birthday）
4. `KeywordPageScorer` 計算 `分數 = Σ(權重 × 次數)`，再依分數排序。

**關鍵字與權重：用維基百科統計，不憑感覺決定**

| 檔案 | 誰維護 | 內容 |
|---|---|---|
| `data/concepts.txt` | **人工** | 要收哪些關鍵字、每個關鍵字有哪些寫法（台版用語為主，日文、英文、簡體為其他寫法）、依據 |
| `data/keywords.txt` | **程式產生** | `權重 \| 關鍵字 \| 其他寫法`，由 `KeywordSeedAnalyzer` 計算，請勿手改 |

權重計算（明細見 `data/keyword-candidates/concept-weights.md`）：

1. 資料來源：日版維基「仮面ライダーオーズ/OOO」、台版維基「假面騎士OOO」＋「假面騎士OOO角色列表」；背景為其他 6 部作品的條目
2. 每個語言：`專屬程度 = OOO 頻率 / (OOO 頻率 + 背景平均頻率)`、`分數 = (1 + ln 次數) × 專屬程度 × 導言加權`
3. 兩語言分數各除以該語言最高分後取平均得 `s`，**`權重 = 1 + 4s`**

其他規則：
- アンク的顯示名稱用台版維基的「**安酷**」，「安卡」列為其他寫法
- **排除詞**（權重 0）：例如「安卡拉」，佔住比對位置但不加分
- 不收容易誤判的寫法：單獨的「Birth」、「變身」

**測試網址：** `data/stage1-urls.txt`，混合維基（參考）、萌娘百科、巴哈姆特、PTT、Fandom，以及兩個對照組（台版維基「假面騎士W」「安卡拉」）。

**實際結果（2026-10-20）：**

| 名次 | 網頁 | 分數 |
|---|---|---|
| 1～3 | 日版維基、台版維基（參考）、Kamen Rider Wiki | 7384.9、2135.3、1654.3 |
| 4 | 萌娘百科（簡體） | 545.8 |
| 5 | 巴哈姆特心得文 | 464.5 |
| 6 | PTT 特攝板心得文 | 141.9 |
| 7 | 巴哈姆特動畫瘋資料庫 | 103.2 |
| 8 | 對照組：假面騎士W | 94.2 |
| 9 | 對照組：安卡拉 | 17.6 |
| 10 | 巴哈姆特心得文（內容很短） | 13.8 |

**延伸（尚未完成）：**

- **多關鍵字比對**：從 HW3 的 Boyer-Moore 延伸到 **Aho-Corasick**（Trie 加 failure link），掃一次就能數完所有關鍵字與同義詞，並比較兩者效能。
- **改良公式**：依頁面長度正規化（目前長頁面明顯占便宜）、標題加權。
- **比較**：HW6 是直接數原始 HTML（網址、標籤都會被算到），本專案改成只數正文。前後差異可以寫進報告。

### 4.2 Stage 2（50%+）：網站排名（多層搜尋）

| | |
|---|---|
| **負責人** | 廖若羽 |
| **期限** | 10/27 |
| **輸入** | 一組網站根網址與關鍵字 |
| **輸出** | 每個網站的名次、總分，以及子網頁樹狀結構（每個節點附關鍵字次數） |
| **主要 package** | `tree/`、`cli/Stage2Main` |

**做法：**

1. 從根網址開始，**用 BFS（Queue）自動往下爬**，建出 `WebTree`。HW6 是手動 `addChild`，這裡改成自動。
2. 用 `HashSet` 記錄已造訪的網址，避免重複爬取或繞圈。
3. 限制深度（例如 2 層）與每層節點數。
4. 用 `LinkFilter` 過濾連結：**只保留同網域、和主題相關的頁面**，排除「登入」、「隨機頁面」、廣告等連結，不然樹會爆炸。
5. 每個節點用 Stage 1 的 `PageScorer` 計分，再**用後序走訪（HW6）**加總。
6. **深度衰減**：越深的節點權重越低，例如節點分數乘上 `0.5^深度`。

**根網址範例：** Kamen Rider Wiki 的 OOO 主頁、萌娘百科的 OOO 主頁、粉絲部落格首頁。

**輸出範例：**

```
萌娘百科：假面騎士OOO  (OOO:18, 映司:25, 安卡:20)
├── 火野映司  (OOO:10, 映司:40, 安卡:8)
├── 安卡  (OOO:6, 映司:12, 安卡:35)
├── 貪婪（怪人列表）
│   ├── 烏伐  (…)
│   └── 卡薩利  (…)
├── 核心硬幣  (…)
└── 劇場版：MOVIE大戰…  (…)
```

預期結果：專門的角色百科，整體分數會勝過只有一篇文章的部落格。

### 4.3 Stage 3（70%+）：只給關鍵字，自己找網站（Refine the Rank of Google）

| | |
|---|---|
| **負責人** | 劉又綺、鄭年芮（`SearchProvider` 介面已在 Stage 0 定好） |
| **期限** | 11/01 |
| **輸入** | 只有關鍵字，例如「假面騎士OOO」、「火野映司」、「安卡」 |
| **輸出** | 用 Stage 2 演算法重新排名的網站清單，並與 Google 原始排名比較 |
| **主要 package** | `search/`、`cli/Stage3Main` |

**做法：分成「離線」與「線上」兩部分**

```
【離線、開發階段】Selenium + Chrome（非 headless，每次查詢之間隔幾秒）
   → 抓 Google 前 20～30 筆網址 → 存成 data/google_results.json
   → 用途：當作與 Google 原始排名比較的對照組

【線上、系統本身】SearchProvider 介面
   ├── DuckDuckGoProvider（不需要 key，用 Jsoup 就能解析）
   ├── MediaWikiProvider（維基、Fandom 的搜尋 API）
   └── CachedGoogleProvider（讀取上面存好的 json）
   → 合併結果、去除重複網址 → Stage 2 爬取並建樹 → 重新排名
```

**離線部分（Selenium 抓 Google）：**

- 只在開發期間手動執行幾次（挑 5～10 組關鍵字），之後都使用存好的檔案。
- **不使用 headless 模式**，比較不會被判定為機器人；跳出驗證碼時可以手動處理。
- 每次查詢之間 `Thread.sleep` 幾秒。Google 一頁只有 10 筆，需要翻頁（`start=10`、`start=20`…）。
- **不把 Selenium 放進線上系統**，原因：容易被驗證碼擋住、違反 Google 使用條款、網頁結構常變、速度慢，而且免費雲端平台跑不動 Chrome。

**線上部分（`SearchProvider` 介面）：**

```java
public interface SearchProvider {
    List<String> search(String query, int limit);
}
```

| 實作類別 | 從哪裡拿網址 | 說明 |
|---|---|---|
| `DuckDuckGoProvider` | `https://html.duckduckgo.com/html/?q=關鍵字` | DuckDuckGo 的純 HTML 版本，不需要 JS，用 Jsoup 就能解析。這是我們自己寫的 Java 類別，**不是瀏覽器** |
| `MediaWikiProvider` | 維基、Fandom 的官方搜尋 API，例如 `https://zh.wikipedia.org/w/api.php?action=query&list=search&srsearch=火野映司&format=json` | 免費、回傳 JSON、最穩定。只搜得到該網站自己的頁面 |
| `CachedGoogleProvider` | 讀取 `data/google_results.json` | 不會連到 Google，所以不會被擋 |

- `SearchAggregator` 呼叫所有 Provider，用 `LinkedHashSet` 合併結果並去除重複（同時保留順序）。
- 主程式只認得 `SearchProvider`，要新增來源只要寫一個新的類別即可（HW2 的介面與多型）。

**`DuckDuckGoProvider` 實作提醒：**

- 結果連結可能是轉址形式（`//duckduckgo.com/l/?uddg=...`），要取出 `uddg=` 後面的部分並做 URL decode。
- 結果的 CSS class（目前常見的是 `a.result__a`）請先用 F12 確認。
- 每次查詢間隔一兩秒，並搭配快取。

**比較重點（報告與 Demo 的核心）：**

| 網站 | Google 排名 | 我們的排名 | 我們的分數 |
|---|---|---|---|
| YouTube 影片 | 1 | 18 | 12 |
| 中文維基 OOO 條目 | 2 | 1 | 486 |
| 變身腰帶購物頁 | 3 | 25 | 3 |
| Kamen Rider Wiki 安卡 | 7 | 2 | 401 |

（數字為示意。）Google 排在前面的可能是影片或購物頁，但它們文字內容少，在我們的評分中會往下掉，資料完整的百科則會往上升。

### 4.4 Stage 4（80%+）：語意分析與關鍵字擴充

| | |
|---|---|
| **負責人** | 蔡妙青 |
| **期限** | 11/12 |
| **輸入** | Stage 3 抓回的網頁與原始關鍵字 |
| **輸出** | 擴充後的關鍵字集合，以及用它再跑一次 Stage 3 的結果 |
| **主要 package** | `semantic/`、`cli/Stage4Main` |

**做法：**

1. **斷詞**：用 HanLP 或 jieba-analysis。**一定要加自訂詞典**，否則「塔托巴組合」、「鴻上集團」這類專有名詞會被切錯。
   - 替代方案：不斷詞，改用 **n-gram 搭配 `HashMap` 計數**，更貼近資料結構課的內容。
2. **過濾停用詞**：「的」、「是」、「劇中」等，詞表放在 `data/stopwords.txt`。
3. **找出相關詞**：自己實作詞頻、TF-IDF、共現次數，用 `HashMap` 計數、`PriorityQueue` 取 Top-K。
   - 延伸：把共現關係建成 **Graph（鄰接表）**，依邊的權重挑出擴充詞。
4. 把高相關的詞以**較低權重**加入關鍵字集合，再回到 Stage 3 搜尋一次。

**可能擴充出的詞：** 塔托巴組合、鴻上集團、伊達明（Birth）、後藤慎太郎、欲望、紫色硬幣、鴻上光生。

**避免偏題：** 例如「假面騎士W」常和 OOO 一起出現在劇場版中，可能把結果帶往別的作品。要設定相關度門檻，並限制迭代次數。

### 4.5 Stage 5（90%+）：上線

| | |
|---|---|
| **負責人** | 劉又綺、鄭年芮（程式組完成後支援） |
| **期限** | 11/19 |
| **主要 package** | `web/`、`src/main/resources/static/` |

**做法：**

- 用 **Spring Boot** 建後端，架構與 HW4 Bonus 的 BMI 專案相同（controller／service）。
- Controller 只呼叫 `engine/` 的 `SearchEngine`，不直接碰底層的類別。
- 網站名稱暫定「OOO 資料搜尋器」。

**頁面功能：**

- 使用者輸入關鍵字，例如「安卡 結局」
- 顯示排名、分數、各關鍵字次數
- 可展開的網站樹狀結構
- 「系統自動擴充的關鍵字」區塊

**部署（免費）：** Render 免費方案（閒置會休眠，第一次開啟較慢），或本機執行加 Cloudflare Tunnel。

**Demo 保險：** 網頁快取（`data/cache/`）讓系統在網路或來源網站出問題時，仍能用快取資料正常展示。

### 4.6 Stage 6（100%+）：與 LLM 比較

| | |
|---|---|
| **負責人** | 劉又綺、鄭年芮 |
| **期限** | 11/26 |
| **主要位置** | `llm/`（`PromptBuilder`）、`evaluation/` |

**做法：**

1. **包成 prompt（RAG 的概念）**：把系統排名前幾的網頁摘要與分數放進 prompt，例如「根據以下檢索結果，說明安卡和映司的關係與結局」，讓 LLM 根據我們的檢索結果回答。
2. **對照組**：直接問 ChatGPT、Gemini、DeepSeek 同一個問題，不提供我們的資料。
3. **比較項目**：
   - 正確性：核心硬幣的種類與顏色、角色關係有沒有講錯
   - 有沒有附來源
   - 中文譯名是否正確
4. 設計 10～20 個問題，人工評分做成表格。

**不花錢的方式：** 在已訂閱的網頁介面手動提問。若需要自動化，使用 Gemini API 的免費額度。

**預期結論：** 純 LLM 在細節上常出錯，加上我們的檢索結果後準確度提升。

### 4.7 Stage 與 package 對照

| Stage | 負責人 | 主要 package |
|---|---|---|
| 0 | 温亮達 | `model/`、`fetch/`、`engine/`（介面） |
| 1 | 温亮達 | `scoring/` |
| 2 | 廖若羽 | `tree/` |
| 3 | 劉又綺、鄭年芮 | `search/` |
| 4 | 蔡妙青 | `semantic/` |
| 5 | 劉又綺、鄭年芮 | `web/`、`resources/static/` |
| 6 | 劉又綺、鄭年芮 | `llm/`、`evaluation/` |

### 4.8 課堂作業對照

| 作業 | 學到的內容 | 用在哪裡 | 延伸 |
|---|---|---|---|
| HW2 繼承、泛型、例外處理 | interface、`try-catch` | Stage 3 的 `SearchProvider` 介面與多種實作；網路錯誤統一處理 | 多個來源合併並去除重複 |
| HW3 WordCounter、Boyer-Moore | 字串搜尋 | Stage 1 計算關鍵字次數 | Aho-Corasick 多關鍵字比對，並比較效能 |
| HW4 KeywordList、Maven、Spring Boot | 有序串列、`Σ(count × weight)`、MVC | Stage 1 計分；Stage 5 網站 | 標題加權、長度正規化 |
| HW5 Stack 檢查 HTML 標籤 | Stack | Stage 1、2 抽取正文 | 用 Stack 跳過 `<script>`、`<style>`、`<nav>` |
| HW6 WebTree、後序走訪 | 樹、後序走訪 | Stage 2 核心 | BFS 自動建樹、`HashSet` 記錄已造訪網址、深度衰減 |
| 後續課程（Heap、Hash、Graph） | — | Stage 3 用 `PriorityQueue` 取 Top-K；Stage 4 用 `HashMap` 算詞頻 | 共現關係建成 Graph |

---

## 5. 協作方式

### 5.1 介面先行

各 Stage 是一層接一層的，如果每個人都要等上一個 Stage 寫完才能動工，時程一定會塞車。所以 **Stage 0 會先定好每一層對外的介面，並附上回傳假資料的實作**：

```java
// Stage 1 提供：分數 + 各關鍵字次數
public interface PageScorer {
    PageResult score(WebPage page, KeywordList keywords);
}

// Stage 2 提供：每個網站的總分與 WebTree
public interface SiteRanker {
    List<SiteResult> rank(List<String> rootUrls, KeywordList keywords);
}

// Stage 3 提供
public interface SearchProvider {
    List<String> search(String query, int limit);
}

// Stage 4 提供
public interface KeywordExpander {
    KeywordList expand(List<WebPage> pages, KeywordList original);
}

// Stage 5 只呼叫這一個：排名、樹、擴充出的關鍵字
public interface SearchEngine {
    SearchResponse search(String query);
}
```

**例子：** 廖若羽寫 Stage 2 時，可以先用一個「每頁都回傳 10 分」的假 `PageScorer`，不必等 Stage 1 完成。真正的實作寫好後換上去即可，Stage 2 的程式不用改。

#### 目前的假實作

這些假實作都**不連網路**，回傳固定格式的資料：

| 介面 | 位置 | 假實作 | 假實作的行為 | 誰會換成真的 |
|---|---|---|---|---|
| `PageScorer` | `scoring/` | `FakePageScorer` | 每頁 10 分，每個關鍵字次數為 0 | Stage 1 |
| `SiteRanker` | `tree/` | `FakeSiteRanker` | 每個根網址一棵單節點樹、10 分，依輸入順序排名 | Stage 2 |
| `SearchProvider` | `search/` | `FakeSearchProvider` | 回傳 `https://example.com/ooo/1`…（最多 5 個） | Stage 3 |
| `KeywordExpander` | `semantic/` | `FakeKeywordExpander` | 不擴充，原封不動回傳原關鍵字 | Stage 4 |
| `SearchEngine` | `engine/` | `FakeSearchEngine` | 用上面的假實作組出完整結果，擴充詞固定為「塔托巴組合」「鴻上集團」 | 程式組整合 |

#### 如何把假實作換成真的

1. 在同一個 package 新增一個實作同一介面的類別，例如 `scoring/KeywordPageScorer implements PageScorer`。
2. 把使用端的 `new FakePageScorer()` 改成 `new KeywordPageScorer()`。其他程式**不需要修改**，因為它們只認得介面。
3. **保留 `FakeXxx`**，不要刪除。其他人的測試可能還在用它，也可以拿來離線 Demo。

**修改共用介面前，請先在群組告知所有人。**

### 5.2 依賴方向

```
web → engine → semantic → search → tree → scoring → fetch → model
```

- **下層不能 import 上層。** 例如 `scoring/` 不可以 import `tree/`。
- **只有 `web/` 可以使用 Spring。** 其他 package 都是純 Java，可以單獨測試，也能用 `cli/` 單獨 Demo。

### 5.3 Git 流程

1. **`main` 必須隨時能編譯、能執行。** 不要直接 push 到 `main`。
2. 每個 Stage 開自己的分支，命名方式為 `stage<N>/<描述>`，例如：
   - `stage0/skeleton`
   - `stage1/page-ranking`
   - `stage2/site-tree`
3. 完成後發 **Pull Request**，至少一位組員看過才能 merge。審查時順便確認有沒有改到別人負責的 package。
4. 要修改別人負責的 package，**先跟負責人說**。
5. 開始工作前先 `git pull`，減少衝突。

### 5.4 共用資料

- 關鍵字、同義詞、種子網址、停用詞都放在 `data/` 的文字檔，**不要寫死在程式裡**。
- 要增減關鍵字或寫法，請改 `data/concepts.txt` 後執行 `KeywordSeedAnalyzer`，不要直接改 `data/keywords.txt`。
- 爬下來的網頁存在 `data/cache/`（不上傳 Git）。每個人都可以重建自己的快取，開發時也不用一直重新爬取。

### 5.5 Stage 報告

**每完成一個 Stage，負責人要在 [notes/stage-reports/](notes/stage-reports/) 寫一份報告**，檔名為 `stageN-report.md`（例如 `stage1-report.md`），跟該 Stage 最後一個 Pull Request 一起提交。

請複製 [TEMPLATE.md](notes/stage-reports/TEMPLATE.md) 來寫，內容包括：

1. 這個 Stage 做了什麼
2. 如何完成（類別、資料結構與演算法、使用的套件）
3. **是否與原始計畫相同**；如果不同，做了哪些更改、原因，以及**改動日期**
4. 執行結果與測試
5. 遇到的問題與解法
6. 未完成事項，以及給下一個 Stage 的交接說明

這些報告可以直接當作 Proposal、PPT 與期末報告的素材。

---

## 6. Repo 架構

### 原則：依「功能」分 package，不依「Stage」分

後面的 Stage 會用到前面的程式（Stage 2 用 Stage 1 的計分，Stage 3 用 Stage 2 的建樹）。如果開 `stage1/`、`stage2/` 資料夾，最後容易變成每個人各複製一份、各改各的。所以整個專案**只有一個 Maven 專案**，依功能分 package，每個 package 對應一位負責人。

### 目錄結構

```
dsa-final-project-group16/
├── pom.xml                      # Maven 設定（目前：Jsoup、Spring Boot、JUnit；HanLP、Selenium 由 Stage 3、4 加入）
├── mvnw, mvnw.cmd, .mvn/        # Maven Wrapper，不用另外安裝 Maven
├── README.md                    # 本文件
├── .gitignore
├── .gitattributes               # 固定 mvnw／mvnw.cmd 的換行格式
│
├── docs/                        # 要繳交的文件
│   ├── proposal/
│   ├── diagrams/                # Class Diagram
│   └── presentation/            # PPT
│
├── notes/                       # 會議紀錄、討論
│   └── stage-reports/           # 各 Stage 完成後的報告（見 5.5）
│
├── data/                        # 非程式的資料，大家共用
│   ├── concepts.txt             # 概念表（人工）：關鍵字與其他寫法
│   ├── keywords.txt             # 關鍵字表（程式產生）：權重 | 關鍵字 | 其他寫法
│   ├── keyword-candidates/      # 候選關鍵字清單與權重計算明細（KeywordSeedAnalyzer 產生）
│   ├── stage1-urls.txt          # Stage 1 測試網址
│   ├── results/                 # 各 Stage 的執行結果（CSV）
│   ├── seed-urls.txt            # Stage 2 的根網址、Stage 3 的備案
│   ├── stopwords.txt            # Stage 4 停用詞
│   ├── google_results.json      # Selenium 抓到的 Google 對照組
│   └── cache/                   # 網頁快取（不上傳 Git）
│
├── evaluation/                  # Stage 6：問題集、LLM 回答、評分表
│
└── src/
    ├── main/java/com/group16/ooosearch/
    │   ├── model/       Keyword, KeywordList, WebPage, PageResult
    │   ├── fetch/       PageFetcher, PageCache, TextExtractor, MediaWikiClient
    │   ├── scoring/     PageScorer, FakePageScorer, KeywordPageScorer, WordCounter, SynonymTable, TextNormalizer
    │   ├── tree/        WebNode, WebTree, SiteResult, SiteRanker, FakeSiteRanker,
    │   │                SiteCrawler, LinkFilter
    │   ├── search/      SearchProvider, FakeSearchProvider, DuckDuckGoProvider, MediaWikiProvider,
    │   │                CachedGoogleProvider, SearchAggregator, GoogleResultCollector
    │   ├── semantic/    KeywordExpander, FakeKeywordExpander, Tokenizer, TfIdfAnalyzer
    │   ├── engine/      SearchEngine, SearchResponse, FakeSearchEngine（串起所有 Stage 的總入口）
    │   ├── web/         OooSearchApplication, controller/, service/
    │   ├── llm/         PromptBuilder
    │   └── cli/         Stage1Main, Stage2Main, Stage3Main, Stage4Main,
    │                    KeywordSeedAnalyzer, ConceptWeightCalculator（離線工具）
    │
    ├── main/resources/
    │   ├── application.properties
    │   └── static/      index.html, CSS, JS（Stage 5 前端）
    │
    └── test/java/com/group16/ooosearch/   # 測試，結構與 main 對應
```

### 各 package 說明

| package | 內容 |
|---|---|
| `model/` | 資料類別，不依賴其他 package。由 HW4、HW6 的類別整理而來。用到 `WebTree` 的 `SiteResult`、`SearchResponse` 分別放在 `tree/`、`engine/`，以維持依賴方向 |
| `fetch/` | 抓網頁、快取、抽取正文（用 Stack 跳過非正文區塊） |
| `scoring/` | 文字正規化、同義詞表、關鍵字計數（長的寫法優先）、單頁計分 |
| `tree/` | BFS 爬取子頁面、連結過濾、建樹、後序走訪計分、網站排名 |
| `search/` | 各種搜尋來源與合併；`GoogleResultCollector` 是離線執行的 Selenium 程式 |
| `semantic/` | 斷詞、TF-IDF、共現分析、關鍵字擴充 |
| `engine/` | 把 Stage 1–4 串成一次完整的搜尋，Stage 5 只透過它呼叫 |
| `web/` | Spring Boot 的進入點、controller、service |
| `llm/` | 把檢索結果組成 prompt |
| `cli/` | 每個 Stage 各自的命令列 Demo，方便單獨展示與測試 |

### `.gitignore`

```gitignore
target/
data/cache/
*.class
.idea/
.vscode/
*.iml
.env
notes/homeworks/
```

> `notes/homeworks/` 是個人作業，含有學號、`.class` 與 `.zip` 檔，**不要上傳到組別的 repo**。需要參考課堂範例時，只放題目說明的 `.md` 檔即可。

---

## 7. 開發環境與執行方式

### 需要安裝

- **JDK 25 以上**（專案以 Java 25 編譯）
- **不需要安裝 Maven**：專案內附 Maven Wrapper（`mvnw`、`mvnw.cmd`），第一次執行時會自動下載 Maven 與所有套件，需要網路
- Google Chrome（只有執行 `GoogleResultCollector` 時才需要）

### 常用指令

所有指令都要在**專案根目錄**執行（程式會從 `data/` 讀取檔案）。

| 用途 | PowerShell／cmd | Git Bash／macOS／Linux |
|---|---|---|
| 編譯並執行所有測試 | `.\mvnw.cmd test` | `./mvnw test` |
| 執行 Stage 1 Demo | `.\mvnw.cmd compile exec:java "-Dexec.mainClass=com.group16.ooosearch.cli.Stage1Main"` | `./mvnw compile exec:java -Dexec.mainClass=com.group16.ooosearch.cli.Stage1Main` |
| 重新產生關鍵字表（修改 `concepts.txt` 後） | `.\mvnw.cmd compile exec:java "-Dexec.mainClass=com.group16.ooosearch.cli.KeywordSeedAnalyzer"` | `./mvnw compile exec:java -Dexec.mainClass=com.group16.ooosearch.cli.KeywordSeedAnalyzer` |
| 啟動網站，開啟 http://localhost:8080 | `.\mvnw.cmd spring-boot:run` | `./mvnw spring-boot:run` |
| 打包成 jar（輸出到 `target/`） | `.\mvnw.cmd package` | `./mvnw package` |

- 執行其他 Stage 的 Demo 時，把 `Stage1Main` 換成 `Stage2Main` 等即可。
- `exec:java` 不會自動編譯，所以前面要加 `compile`。
- 網站目前還沒有頁面，開啟後顯示 404 是正常的（Stage 5 才會加上）。按 `Ctrl+C` 停止。
- **中文變成亂碼**：在 cmd 先執行 `chcp 65001` 切換成 UTF-8。

### 預期輸出

執行 Stage 1 Demo 會抓取 `data/stage1-urls.txt` 的網址（第一次需要網路，之後使用 `data/cache/` 的快取），印出排名與各網頁的關鍵字次數，並把結果存到 `data/results/stage1-results.csv`：

```
===== 排名 =====
名次	分數	說明	網址
1	7384.9	日版維基（參考）	https://ja.wikipedia.org/wiki/...
...
4	545.8	萌娘百科（簡體）	https://zh.moegirl.org.cn/...
...

===== 各網頁關鍵字次數（只列出現過的） =====
...
6. PTT特攝板心得文：假面騎士OOO 8、安酷 5、貪婪 1、火野映司 18
```

可以用參數指定其他檔案：`Stage1Main [網址檔] [關鍵字檔]`，例如 `"-Dexec.args=data/my-urls.txt"`。

---

## 8. 風險與注意事項

### 8.1 爬蟲

- **萌娘百科有較嚴格的反爬蟲機制**，用 Jsoup 直接抓可能會被擋。以中文維基、Fandom 為主要來源，並盡量使用 MediaWiki API（`action=parse`）取得內容，比解析 HTML 穩定。
- 爬取時設定 User-Agent、每次請求間隔一段時間，並優先使用快取，避免被封鎖。
- 一定要限制爬取深度與數量。

### 8.2 搜尋來源

- 不要讓系統依賴 Google。Google 結果只當作離線取得的對照組。
- DuckDuckGo 的 HTML 結構可能改變。`MediaWikiProvider` 和 `data/seed-urls.txt` 是備案。

### 8.3 Demo 當天

- 事先用 Demo 要用的關鍵字跑一次，讓快取裡有完整資料。
- 網路或來源網站出問題時，系統改用快取資料展示。
- Render 免費方案閒置後會休眠，Demo 前先開一次網站把它喚醒。

### 8.4 時程與分工

- Stage 2 只有 5 天（10/22 → 10/27），可以直接從 HW6 的程式改起。
- Stage 3 要等 Stage 2，而工作組 11/1 前還要寫 Proposal。`SearchProvider` 介面與 `FakeSiteRanker` 已在 Stage 0 完成，工作組可以先用假資料開發，不必等 Stage 2。
- Stage 3、5、6 和 PPT 集中在同兩位組員身上。**程式組在 Stage 4 完成後（11/12）支援 Stage 5**，PPT 與上台報告也要盡早指定負責人。

### 8.5 使用 AI 工具

課程的 HW1 Bonus 要求記錄 prompt，並重視「用 AI 學習讀懂程式與除錯」，而不是直接產生程式碼。開發時請確保自己理解負責的程式碼，Demo 與報告時才講得清楚。

---

## 9. 進度追蹤

### 各資料夾的 TODO

每個資料夾都有一份 `TODO.md`，寫明負責人、期限、要建立的檔案與待辦事項。**完成一項就在該檔案打勾**，隨程式一起 commit。

| 負責人 | 要看的 TODO |
|---|---|
| 温亮達 | [model](src/main/java/com/group16/ooosearch/model/TODO.md)、[fetch](src/main/java/com/group16/ooosearch/fetch/TODO.md)、[scoring](src/main/java/com/group16/ooosearch/scoring/TODO.md)、[engine](src/main/java/com/group16/ooosearch/engine/TODO.md)、[diagrams](docs/diagrams/TODO.md) |
| 廖若羽 | [tree](src/main/java/com/group16/ooosearch/tree/TODO.md)、[search](src/main/java/com/group16/ooosearch/search/TODO.md)（`SearchProvider` 介面） |
| 蔡妙青 | [semantic](src/main/java/com/group16/ooosearch/semantic/TODO.md) |
| 劉又綺、鄭年芮 | [proposal](docs/proposal/TODO.md)、[search](src/main/java/com/group16/ooosearch/search/TODO.md)、[web](src/main/java/com/group16/ooosearch/web/TODO.md)、[resources](src/main/resources/TODO.md)、[llm](src/main/java/com/group16/ooosearch/llm/TODO.md)、[evaluation](evaluation/TODO.md) |
| 所有人 | [cli](src/main/java/com/group16/ooosearch/cli/TODO.md)、[data](data/TODO.md)、[test](src/test/java/com/group16/ooosearch/TODO.md) |
| 尚未分配 | [presentation](docs/presentation/TODO.md) |

根目錄的 `pom.xml` 與 Maven Wrapper（`mvnw`、`mvnw.cmd`）已在 Stage 0 建立。

### 整體里程碑

> 每完成一個 Stage，除了打勾，也要在 `notes/stage-reports/` 提交該 Stage 的報告（見 [5.5](#55-stage-報告)）。

- [x] 專案資料夾結構與各資料夾的 TODO
- [x] `.gitignore`
- [x] Stage 0：`pom.xml`、介面、假資料實作（2026-10-20）
- [x] Stage 1：單一網頁排名（2026-10-20）
- [ ] Proposal 主題介紹（10/22）
- [ ] Stage 2：網站排名（樹狀結構）
- [ ] Stage 3：自主搜尋網址並重新排名
- [ ] Proposal 最終版（11/05）
- [ ] Stage 4：語意分析與關鍵字擴充
- [ ] Stage 5：網站上線
- [ ] Stage 6：與 LLM 比較
- [ ] PPT 完稿（12/10）
- [ ] 最終 Demo（12/17）
- [ ] 指定 PPT 與上台報告的負責人
