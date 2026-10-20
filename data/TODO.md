# data/ — 共用資料

關鍵字、同義詞等都放在這裡的文字檔，**不要寫死在程式裡**。

| 檔案 | 用途 | Stage | 負責人 |
|---|---|---|---|
| [x] `concepts.txt` | 概念表（人工）：關鍵字與其他寫法 | 1 | 温亮達 |
| [x] `keywords.txt` | 關鍵字表（由 `KeywordSeedAnalyzer` 產生，勿手改） | 1 | 温亮達 |
| [x] `stage1-urls.txt` | Stage 1 測試網址 | 1 | 温亮達 |
| [ ] `seed-urls.txt`（已建立空檔案與格式說明） | Stage 2 的根網址、Stage 3 的備案 | 2 | 廖若羽 |
| [ ] `google_results.json` | Selenium 抓到的 Google 對照組 | 3 | 劉又綺、鄭年芮 |
| [ ] `stopwords.txt`（已建立空檔案與格式說明） | 停用詞 | 4 | 蔡妙青 |

`cache/`：網頁快取，由 `PageCache` 自動建立，不上傳 Git。

`keyword-candidates/`：由 `cli/KeywordSeedAnalyzer` 從日版、台版維基百科產生的候選關鍵字（`summary.md` 為審核用摘要）。

- [x] 依候選清單整理 `concepts.txt`，權重由程式計算（温亮達）
- [ ] 全組再看過一次 `concepts.txt` 與 `keyword-candidates/concept-weights.md`，有需要就增減關鍵字或寫法

`results/`：各 Stage 的執行結果，例如 `stage1-results.csv`。
