# Stage 0 Report：專案骨架

## 基本資訊

| 項目 | 內容 |
|---|---|
| Stage | 0 |
| 負責人 | 温亮達 |
| 原定期限 | 10/22 |
| 實際完成日期 | 2026-10-20 |
| 相關 PR／分支 | 待提交 |

## 1. 這個 Stage 做了什麼

建好全組共用的專案骨架，讓其他組員 clone 後就能編譯、執行，並且**不用等上一個 Stage 完成**就能開始開發自己的部分：

- 可以用 Maven Wrapper 編譯、測試、執行的 Maven 專案（不必另外安裝 Maven）
- 共用的資料類別（`model/`）與抓網頁、快取功能（`fetch/`）
- 5 個介面，每個都附一個**不連網路**的假實作
- `Stage1Main` 命令列 Demo（目前使用假資料）與最小的 Spring Boot 進入點
- `data/` 的關鍵字表、同義詞表初版
- 12 個單元測試

## 2. 如何完成

### 新增或修改的類別

| 檔案 | 說明 |
|---|---|
| `pom.xml`、`mvnw`、`mvnw.cmd`、`.mvn/`、`.gitattributes` | Maven 設定與 Wrapper，沿用 HW4 Bonus BMI 專案的設定（Spring Boot 4.1.1、Java 21（2026-10-20 改為 25）、Maven 3.10.0） |
| `model/Keyword`、`KeywordList` | 由 HW4、HW6 整理而來；`KeywordList.fromFile` 讀取 `data/keywords.txt` |
| `model/WebPage` | 網址、標題、原始 HTML。與 HW6 不同，不會自己抓網頁 |
| `model/PageResult` | 單頁分數與各關鍵字次數 |
| `fetch/PageCache` | 網頁存到 `data/cache/`（檔名為網址的 SHA-256），記憶體用 `HashMap` 當索引 |
| `fetch/PageFetcher` | 先查快取，沒有才用 Jsoup 下載；設定 User-Agent、逾時、兩次連線至少間隔 1 秒 |
| `scoring/PageScorer`、`FakePageScorer` | Stage 1 介面與假實作 |
| `tree/WebNode`、`WebTree` | 由 HW6 搬來的樹狀資料結構（只有結構，計分與走訪留給 Stage 2） |
| `tree/SiteResult`、`SiteRanker`、`FakeSiteRanker` | Stage 2 結果類別、介面與假實作 |
| `search/SearchProvider`、`FakeSearchProvider` | Stage 3 介面與假實作 |
| `semantic/KeywordExpander`、`FakeKeywordExpander` | Stage 4 介面與假實作 |
| `engine/SearchEngine`、`SearchResponse`、`FakeSearchEngine` | 總入口介面、結果類別與假實作（用其他假實作組成） |
| `cli/Stage1Main` | 讀取關鍵字、用 `FakePageScorer` 計分並印出排名表 |
| `web/OooSearchApplication`、`application.properties` | 最小的 Spring Boot 進入點，port 8080 |
| `data/keywords.txt`、`synonyms.txt`、`seed-urls.txt`、`stopwords.txt` | 共用資料檔（後兩者只有格式說明） |

### 用到的資料結構與演算法

- `HashMap`：`PageCache` 的記憶體索引（網址 → HTML）
- `LinkedHashMap`：`PageResult` 保留關鍵字順序
- 樹：`WebNode`／`WebTree`（HW6），`WebTree.size()` 用遞迴計算節點數
- 介面與多型（HW2）：5 個介面各有假實作，日後換成真的實作時，使用端不用改

### 使用的套件或外部資源

- Jsoup 1.21.1（抓網頁、解析 HTML）
- Spring Boot 4.1.1（webmvc；只有 `web/` 使用）
- JUnit 5（透過 `spring-boot-starter-test`）
- exec-maven-plugin 3.5.0（執行 CLI Demo）

## 3. 與原始計畫的比較

原始計畫見 [README 第 4 節](../../README.md#4-各-stage-目標做法與負責人)。

**是否與原始計畫相同：** 部分不同

### 改動紀錄

| 改動日期 | 原始計畫 | 實際做法 | 改動原因 | 是否影響其他 Stage |
|---|---|---|---|---|
| 2026-10-20 | `SiteResult`、`SearchResponse` 放在 `model/` | `SiteResult` 放 `tree/`，`SearchResponse` 放 `engine/` | 兩者都用到 `WebTree`。放在 `model/` 會讓 `model/` 依賴 `tree/`，違反 README 5.2 的依賴方向 | Stage 2、5：import 路徑不同，README 第 6 節已更新 |
| 2026-10-20 | `WebNode`、`WebTree` 全部由 Stage 2 建立 | Stage 0 先從 HW6 搬來資料結構（節點、父子關係、`nodeScore`），計分、走訪、印樹仍由 Stage 2 實作 | 沒有它們，`SiteResult` 與假實作無法編譯 | Stage 2（廖若羽）：直接在現有類別上擴充；根節點深度改為 0（HW6 是 1），方便計算 `0.5^深度` |
| 2026-10-20 | `SearchProvider` 介面由廖若羽先定 | Stage 0 直接建立 | README 已講好簽名，先建立可以減少 Stage 2 的前置工作 | Stage 2、3：`tree/TODO.md`、`search/TODO.md` 已更新 |
| 2026-10-20 | `web/OooSearchApplication` 由 Stage 5 建立 | Stage 0 建立最小版本（沒有 controller） | 專案有多個 `main`，沒有指定 Spring Boot 進入點的話，`mvnw package` 會失敗 | Stage 5：controller、service 仍由 Stage 5 新增 |
| 2026-10-20 | `pom.xml` 包含 HanLP、Selenium | 只放現在用得到的 Jsoup、Spring Boot、JUnit | 避免下載用不到的套件；版本由實際使用的人決定 | Stage 3、4：需要時自行加入 `pom.xml` |
| 2026-10-20 | README 寫 JDK 17 以上 | JDK 21 以上 | 沿用 BMI 專案的設定（`java.version` 21），組員做 HW4 Bonus 時已用過這個環境 | 所有人：JDK 需為 21 以上 |
| 2026-10-20 | JDK 21 以上 | JDK 25 以上 | `pom.xml` 的 `java.version` 改為 25 | 所有人：JDK 需為 25 以上，README 第 7 節已更新 |

## 4. 執行結果

### 如何執行

```bash
./mvnw test
./mvnw compile exec:java -Dexec.mainClass=com.group16.ooosearch.cli.Stage1Main
./mvnw spring-boot:run
```

（PowerShell 的寫法見 README 第 7 節。）

### 輸出範例

```
關鍵字：[[假面騎士OOO,5.0], [火野映司,3.0], [安卡,3.0], [核心硬幣,2.0], [貪婪,2.0], [變身,1.0]]
名次	網址	分數	假面騎士OOO	火野映司	安卡	核心硬幣	貪婪	變身
1	https://example.com/ooo/1	10.00	0	0	0	0	0	0
2	https://example.com/ooo/2	10.00	0	0	0	0	0	0
```

### 測試

12 個測試全部通過（`Tests run: 12, Failures: 0, Errors: 0`）：

- `model/KeywordListTest`（4）：讀檔、略過註解與空行、缺少權重或權重不是數字時拋出例外、專案的 `data/keywords.txt` 格式正確
- `fetch/PageCacheTest`（3）：沒有快取時回傳空值、存取 HTML、重新建立物件後仍能從磁碟讀到
- `FakeImplementationsTest`（5）：每個假實作都回傳格式正確的結果

另外確認了 `mvnw spring-boot:run` 能在 8080 啟動（目前沒有頁面，回傳 404 屬正常），以及 `mvnw package` 能產生 jar。`PageFetcher` 的實際連網沒有寫測試，因為結果會受網路影響而不穩定。

## 5. 遇到的問題與解法

| 問題 | 解法 |
|---|---|
| 專案有多個 `main`（`Stage1Main`、Spring Boot），`package` 時 Spring Boot 不知道要用哪一個 | 在 `pom.xml` 的 `spring-boot-maven-plugin` 指定 `mainClass` |
| `exec:java` 不會自動編譯，直接執行會找不到類別 | 指令寫成 `compile exec:java` |
| PowerShell 會把 `-Dexec.mainClass=...` 拆開解析 | README 中 PowerShell 版的參數加上引號 |

## 6. 未完成事項與交接

- **未完成或延後的項目：** 尚未提交 PR。`TextExtractor` 依計畫留給 Stage 1。
- **給下一個 Stage 的說明：**
  - **所有人**：先讀 README 5.1「如何把假實作換成真的」。新增實作類別後，把使用端的 `new FakeXxx()` 換掉即可，**不要刪除 `FakeXxx`**。
  - **Stage 1（温亮達）**：`Stage1Main` 目前用範例網頁，要改成用 `PageFetcher` 抓真實網頁。注意 `synonyms.txt` 裡「OOO」是「假面騎士OOO」的子字串，合併計數時要避免重複計算。
  - **Stage 2（廖若羽）**：`WebNode`／`WebTree` 已有資料結構，請直接擴充。`WebNode` 已有 `pageResult` 欄位，可以存每個節點的關鍵字次數，供印樹時使用。
  - **Stage 3、5（劉又綺、鄭年芮）**：Stage 2 完成前可以用 `FakeSiteRanker`；做前端時用 `FakeSearchEngine` 取得完整格式的假資料。
  - **Stage 3、4**：HanLP、Selenium 需要時自行加入 `pom.xml`。
  - 抓網頁時請透過 `PageFetcher`，這樣才會自動使用快取並控制連線頻率。
