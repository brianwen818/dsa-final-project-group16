# Final Project: 假面騎士OOO搜尋引擎

**GitHub 連結：**[https\://github.com/brianwen818/Data-Structure-Final-Project---Group-16.git](https://github.com/brianwen818/Data-Structure-Final-Project---Group-16.git)

# 

# 組別與分工

* 程式組:  
  * 温亮達: stage 0, 1  
  * 廖若羽: stage 2  
  * 蔡妙青: stage 4  
* 測試, 前後端, 與其他工作組:  
  * 工作: stage 3, stage 5, stage 6  
  * 分工自行協調但順序如下:   
    * 11/1 stage 3做完前先做proposal   
    * 11/1後或等程式組做完0-2之後一起做3  
    * 成員：劉又綺、鄭年芮  
* 未分配工作:  
  * PPT  
  * 上台報告

# 

# 時程規劃

* **開發階段（Stage）**  
  * Stage 0 & 1：10/22  
  * Stage 2：10/27  
  * Stage 3：11/01  
  * Stage 4：11/12  
  * Stage 5：11/19  
  * Stage 6：11/26  
  * *備註：完成當前階段後請直接接力進行下一階段，切勿拖延。*  
* **專案企劃（Proposal）**  
  * 主題介紹：10/22  
  * 最終版本繳交：11/05  
  * 內容要求:  
    1. Introduction /Your topic and motivation  
    2. Search tricks /Your score formulation  
    3. System design /Class diagrams \[ProposalExample\]  
    4. Schedule /How and when to accomplish stages  
    5. Challenges /Techniques that you need but may have a hard time to learn on your own  
* **簡報與展示**  
  * PPT 完稿：12/10  
  * 最終上台 Demo：12/17

# 

# 各階段簡介

## 教授原文

Let's Beat Google\!  
Stage 1 (30%+): Page Ranking

* **Input:** Given a set of keywords and URLs.  
* **Score Definition:** Define a score formula based on keyword appearances.  
* **Ranking:** Rank the URLs based on their calculated score.  
* **Output:** For each URL (a single web page), return:  
  * Its rank  
  * Its score  
  * The appearance count of each keyword

Stage 2 (50%+): Site Ranking (Multi-Level Keyword Search)

* **Input:** Given a set of websites (root URLs) and keywords.  
* **Score Definition:** Define a score formula based on keyword appearances across the main URL and all of its sub-URLs.  
* **Ranking:** Rank the websites according to their aggregate keyword appearances.  
* **Output:** For each website, return:  
  * Its rank  
  * Its total score  
  * A tree structure representing all sub-URLs, displaying the keyword count for each node

Stage 3 (70%+): Refine the Rank of Google

* **Input:** Given only a set of keywords (no initial URLs provided).  
* **URL Discovery:** Query external search engines to retrieve potential candidate URLs.  
* **Ranking:** Apply the **Stage 2** ranking algorithm to these discovered websites.

Stage 4 (80%+): Semantics Analysis

* **Keyword Expansion:** Derive related or semantically relevant keywords from the discovered websites.  
* **Iterative Search:** Feed the expanded keywords back into **Stage 3** to iteratively refine the discovery and ranking pipeline.

Stage 5 (90%+): Publish Your Work Online / App

* **Deployment:** Build and launch a web service or application interface to make your search engine publicly usable.

Stage 6 (100%+): Compare with LLM Models

* **Prompt Engineering:** Wrap your search algorithms and retrieval logic into structured prompts.  
* **Benchmarking:** Query major LLM services (e.g., ChatGPT, Gemini, DeepSeek) and compare their output quality and relevance against your search engine.

## AI 解說

這是一個自己動手做一個簡化版搜尋引擎的期末專案（常見的資料結構經典題目），本組以「假面騎士OOO」為主題。核心概念為：給定與 OOO 相關的關鍵字，找出最相關的網頁並排序，逐步升級至能與 Google 及 LLM 進行比較。分數百分比代表完成該階段可獲得的成績門檻。

## **各階段任務說明**

* **Stage 1：單一網頁排名**  
  * 給定關鍵字和網址（如中文維基百科、萌娘百科、Kamen Rider Wiki 的 OOO 條目），程式需抓取每個網頁內容、統計關鍵字出現次數，並透過自訂公式計算分數（如：分數 \= 5 ×「假面騎士OOO」+ 3 ×「火野映司」+ 3 ×「安卡」+ 2 ×「核心硬幣」+ 2 ×「貪婪」）。最後依分數排序，輸出排名、分數及各關鍵字次數。  
  * 由於作品存在中日英名稱混用（如「OOO／歐茲／オーズ」、「安卡／Ankh／アンク」），可建立同義詞表合併計數。  
  * 使用技能：HTTP 網頁抓取、字串搜尋、排序。  
* **Stage 2：整個網站排名（多層搜尋）**  
  * 給定網站首頁（如萌娘百科的 OOO 主頁），向外延伸抓取子網頁並建立樹狀結構，每個節點為一個網頁，例如「火野映司」、「安卡」、「貪婪」角色頁及其下的「烏伐」、「卡薩利」等子頁面。網站總分為所有節點分數加總，可設計為層級越深權重越低。輸出時需印出樹狀結構與各節點關鍵字次數。  
  * 使用技能：樹狀結構、遞迴或 BFS/DFS 走訪、HTML `<a href>` 解析。需設定抓取深度與數量限制，並過濾「登入」、「隨機頁面」、廣告等無關連結。  
* **Stage 3：自主搜尋網址（Refine the Rank of Google）**  
  * 僅提供關鍵字（如「假面騎士OOO」、「火野映司」、「安卡」），需向外部搜尋引擎查詢並取得前數十個候選網址，再套用 Stage 2 的排序演算法重新計算與排序。  
  * 例如 Google 排名靠前的可能是 YouTube 影片或變身腰帶購物頁，但其文字內容較少，經本系統重新評分後，資料完整的角色百科將排名上升，藉此達到優化 Google 搜尋結果的目的。  
* **Stage 4：語意分析與關鍵字擴充**  
  * 從檢索到的網頁中分析相關的新關鍵字（例如搜尋「假面騎士OOO」時自動擴充「塔托巴組合」、「鴻上集團」、「伊達明」、「欲望」、「紫色硬幣」），將其加入關鍵字集後再次執行 Stage 3 進行迭代優化。  
  * 實作方式：詞頻分析、TF-IDF、共現次數或 Word Embedding。中文需先進行斷詞（如 jieba）並過濾停用詞。另需設定門檻，避免因常一起出現在劇場版中的「假面騎士W」等其他作品而擴充偏題。  
* **Stage 5：系統上線與產品化**  
  * 將搜尋引擎開發為「OOO 資料搜尋」Web 網站或 App。使用者輸入關鍵字（如「安卡 結局」）後，頁面顯示排名、分數、各關鍵字次數、可展開的網站樹狀結構，以及系統自動擴充的關鍵字。建置前後端介面並部署至雲端，提供公開檢索服務。  
* **Stage 6：與大型語言模型（LLM）比較**  
  * 將檢索結果組合成結構化 Prompt（類似 RAG 檢索增強生成），例如「根據以下檢索結果，說明安卡與映司的關係與結局」，輸入至 ChatGPT、Gemini、DeepSeek 等 LLM 模型。  
  * 同時直接詢問 LLM 相同問題作為對照組，從核心硬幣種類與顏色、角色關係、中文譯名等細節評估正確性與相關度。LLM 在特攝這類冷門細節上容易產生幻覺，可藉此凸顯檢索結果的價值。

**專案整體流程總結**

本專案引導完成搜尋引擎的完整建置管道：

* 網頁爬取 → 評分計分 → 排序演算法 → 樹狀網站結構 → 查詢擴充 → 產品上線 → AI 比較

前兩個階段著重於核心資料結構（如字串處理、樹、排序、Heap）；後續階段則聚焦於系統工程整合與 AI 實驗設計。以「假面騎士OOO」為主題的優勢在於有大量專門百科可供爬取，且中日英名稱混雜，適合在 Stage 1 實作同義詞處理、在 Stage 4 展示語意擴充的效果。

### 

## **執行計畫草稿**

## **Stage 1：單一網頁排名**

**輸入**：你自己挑幾個網址和關鍵字。

* 關鍵字（附權重）：`假面騎士OOO`(5)、`火野映司`(3)、`安卡`(3)、`核心硬幣`(2)、`貪婪`(2)、`變身`(1)  
* 網址例如：中文維基百科的 OOO 條目、萌娘百科的 OOO 頁面、Kamen Rider Wiki (fandom) 的 OOO 頁面、巴哈姆特的某篇討論文

**做法**：抓下每個網頁的文字，數每個關鍵字出現幾次，例如：

| 名次 | 網址 | 分數 | 假面騎士OOO | 火野映司 | 安卡 | 核心硬幣 | … |
| ----- | ----- | ----- | ----- | ----- | ----- | ----- | ----- |
| 1 | 萌娘百科 | 212 | 18 | 25 | 20 | 12 | … |
| 2 | 維基百科 | 175 | 15 | 20 | 14 | 8 | … |
| 3 | 巴哈文章 | 40 | 3 | 5 | 2 | 1 | … |

分數 \= Σ(權重 × 次數)。公式由你自己設計，例如也可以讓標題中出現的次數加倍。

**要注意**：中日英名稱混用。「OOO / 歐茲 / オーズ」、「安卡 / Ankh / アンク」都是同一個東西，可以設計成同義詞合併計數。這也是報告裡很好發揮的點。

## **Stage 2：網站排名（樹狀結構）**

**輸入**：給的是網站的根網址，例如：

* Kamen Rider Wiki 的 OOO 主頁  
* 萌娘百科的 OOO 主頁  
* 某個粉絲部落格首頁

**做法**：從主頁往下爬它連結到的子頁面，限制深度（例如 2 層），建成一棵樹：

萌娘百科：假面騎士OOO  (OOO:18, 映司:25, 安卡:20)  
├── 火野映司  (OOO:10, 映司:40, 安卡:8)  
├── 安卡  (OOO:6, 映司:12, 安卡:35)  
├── 貪婪（怪人列表）  
│   ├── 烏伐  (…)  
│   └── 卡薩利  (…)  
├── 核心硬幣  (…)  
└── 劇場版：MOVIE大戰…  (…)

網站總分 \= 所有節點分數加總，可以讓越深的節點權重越低，例如乘上 0.5^深度。最後比較哪個網站對 OOO 的整體涵蓋最完整，通常專門的角色百科會贏過只有一篇文章的部落格。

**要注意**：子連結要過濾。只留同網域、和主題相關的頁面，像「登入」、「隨機頁面」、廣告這類連結要排除，不然樹會爆炸。

## **Stage 3：只給關鍵字，自己找網站** 

**輸入**：只有 `假面騎士OOO`、`火野映司`、`安卡`。

**做法**：

1. 用程式向 Google 或 Bing 查詢（Google Custom Search API、SerpAPI 等），取前 20～30 筆結果。  
2. 結果可能包含維基、萌娘、fandom、YouTube、Twitter、巴哈、購物網站（賣變身腰帶的）等。  
3. 對每個結果的網站跑 Stage 2 的演算法，重新排序。

**比較重點**：Google 排第一的可能是 YouTube 影片或購物頁，但它們文字內容少，在你的評分裡會掉下去；資料詳細的百科會往上升。這就是「改良 Google 排名」的意思。

## **Stage 4：語意分析與關鍵字擴充**

**做法**：從 Stage 3 抓回的網頁中，找出常和你的關鍵字一起出現的詞（用詞頻、TF-IDF 或共現次數），例如可能會得到：

* `塔托巴組合`、`鴻上集團`、`伊達明`（Birth）、`後藤慎太郎`、`欲望`、`紫色硬幣`、`鴻上光生`

把高相關的詞加進關鍵字集合（權重較低），再回到 Stage 3 搜尋一次，就能找到原本漏掉的頁面，例如專門介紹各組合形態的網站，或討論劇情結局的文章。

**要注意**：中文要先斷詞（例如用 jieba），並建立停用詞表過濾「的」、「是」、「劇中」這類詞。也要避免擴充偏題：例如抓到「假面騎士W」，因為兩部常一起出現在電影裡，它可能會把結果帶往別的作品。可以設定門檻，或限制迭代次數。

## **Stage 5：上線**

做一個簡單網站，例如「OOO 資料搜尋器」：

* 使用者輸入關鍵字，例如「安卡 結局」  
* 頁面顯示排名、分數、各關鍵字次數，以及可展開的網站樹  
* 可以加一區顯示「系統自動擴充的關鍵字」

技術上可以用 Flask、FastAPI 或 Spring Boot（看課程要求的語言），部署到 Render、Railway 或 GCP 這類免費或低價平台。

## **Stage 6：跟 LLM 比較**

1. **包成 prompt**：把你系統排名前幾的網頁摘要和分數放進 prompt，例如「根據以下檢索結果，說明安卡和映司的關係與結局」，讓 LLM 根據你的檢索結果回答（這就是 RAG 的概念）。  
2. **對照組**：直接問 ChatGPT、Gemini、DeepSeek 同一個問題，不提供你的資料。  
3. **比較**：  
   * 正確性：例如核心硬幣的顏色和種類、角色關係有沒有講錯。LLM 對特攝這類冷門細節常會亂掰，這是很好的展示點。  
   * 有沒有附來源  
   * 中文名稱是否正確（譯名混亂是常見錯誤）

可以設計 10～20 個問題，人工評分做成表格，結論可能會是：「純 LLM 在細節上常出錯，加上我們的檢索後準確度提升」。

---

**小建議**：OOO 這個題目的優點是有大量專門百科可以抓，加上中日英名稱混雜，很適合在 Stage 1 做同義詞處理、在 Stage 4 做語意擴充，報告會很有內容。要不要我先幫你把 Stage 1 的關鍵字表（含同義詞）和程式框架寫出來？

