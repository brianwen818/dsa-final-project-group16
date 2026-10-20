# data/ — 共用資料

關鍵字、同義詞等都放在這裡的文字檔，**不要寫死在程式裡**。

| 檔案 | 用途 | Stage | 負責人 |
|---|---|---|---|
| [x] `keywords.txt` | 關鍵字與權重 | 0／1 | 温亮達 |
| [x] `synonyms.txt`（初版，Stage 1 再補充） | 同義詞，例如：安卡,Ankh,アンク | 1 | 温亮達 |
| [ ] `seed-urls.txt`（已建立空檔案與格式說明） | Stage 2 的根網址、Stage 3 的備案 | 2 | 廖若羽 |
| [ ] `google_results.json` | Selenium 抓到的 Google 對照組 | 3 | 劉又綺、鄭年芮 |
| [ ] `stopwords.txt`（已建立空檔案與格式說明） | 停用詞 | 4 | 蔡妙青 |

`cache/`：網頁快取，由 `PageCache` 自動建立，不上傳 Git。
