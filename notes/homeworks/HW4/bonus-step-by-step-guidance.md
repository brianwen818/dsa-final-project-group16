# HW4 Bonus 新手教學：做出一個 BMI 計算網頁

> 這份教學寫給**完全沒做過網頁、沒碰過 Spring Boot** 的同學。跟著步驟一步一步做，大約 30～60 分鐘可以完成。
> 各檔案的完整程式碼在 [bonus.md](bonus.md)，這份教學會告訴你「要做什麼、放哪裡、怎麼跑起來」。

---

## 目錄

- [0. 我們要做出什麼？](#0-我們要做出什麼)
- [1. 先搞懂幾個名詞（3 分鐘）](#1-先搞懂幾個名詞3-分鐘)
- [2. 準備工具](#2-準備工具)
- [3. 建立專案](#3-建立專案)
- [4. 用 VS Code 打開專案](#4-用-vs-code-打開專案)
- [5. 新增程式檔案](#5-新增程式檔案)
- [6. 啟動網站](#6-啟動網站)
- [7. 打開網頁測試](#7-打開網頁測試)
- [8. 截圖並上傳](#8-截圖並上傳)
- [9. 卡住了怎麼辦？](#9-卡住了怎麼辦)

---

## 0. 我們要做出什麼？

一個在瀏覽器裡打開的 BMI 計算器：

```
┌──────────────────────────────┐
│          BMI 計算器           │
│                              │
│  身高（公分）: [ 170       ]  │
│  體重（公斤）: [ 65        ]  │
│                              │
│  [        計算BMI        ]   │
│                              │
│    BMI: 22.49 (正常範圍)      │
└──────────────────────────────┘
```

**最後要交的東西**：一張像上面這樣的截圖（要看得到輸入的身高體重**和**算出來的結果），上傳到 Moodle。

---

## 1. 先搞懂幾個名詞（3 分鐘）

不用背，看過有個印象就好。

| 名詞 | 白話解釋 |
| --- | --- |
| **網站伺服器 (Server)** | 一支「一直開著、等別人來問問題」的程式。我們寫的 Java 程式啟動後就會變成伺服器。 |
| **瀏覽器 (Browser)** | Chrome、Edge 這些。它負責顯示網頁，並把使用者輸入的資料送去給伺服器。 |
| **localhost** | 「我自己這台電腦」。`http://localhost:8080` 的意思是：連到我自己電腦上的伺服器。只有你自己看得到，不會公開到網路上。 |
| **8080 (Port)** | 一台電腦上可以同時跑很多伺服器，port 就像「房間號碼」。我們的伺服器預設住在 8080 號房。 |
| **API** | 伺服器提供的一個「窗口」。例如 `/api/bmi?height=170&weight=65` 就是在對伺服器說：「身高 170、體重 65，幫我算 BMI」。 |
| **JSON** | 伺服器回答的格式，長得像 `{"bmi":22.49,"category":"正常範圍"}`。 |
| **Spring Boot** | 一套 Java 工具，幫我們把「寫一個網站伺服器」這件事變得很簡單。 |
| **Maven** | 自動幫你**下載 Spring Boot 需要的套件**、並**編譯執行程式**的工具。你不用自己安裝它，專案裡會附一個。 |

### 整個網站怎麼運作？

```
① 你在網頁輸入身高、體重，按下按鈕
            │
            ▼
② 瀏覽器 (index.html) 對伺服器說：
   「/api/bmi?height=170&weight=65」
            │
            ▼
③ BmiController（櫃台）收到請求，轉交給 ↓
            │
            ▼
④ BmiService（計算的人）算出 BMI = 22.49、分類 = 正常範圍
            │
            ▼
⑤ 伺服器把答案用 JSON 回傳給瀏覽器
            │
            ▼
⑥ 網頁把「BMI: 22.49 (正常範圍)」顯示出來
```

你要寫的檔案就是：**index.html（網頁）**、**BmiController（櫃台）**、**BmiService（計算）**。程式碼 [bonus.md](bonus.md) 都給了，你只要複製貼上到正確的位置。

---

## 2. 準備工具

### 2-1 檢查 Java 版本

1. 按鍵盤 `Win` 鍵，搜尋 **PowerShell** 並打開
2. 輸入下面這行，按 Enter：

   ```powershell
   java -version
   ```

3. 看第一行的數字：
   - 顯示 `17`、`21`、`25` 或更大的數字（例如 `java version "21.0.5"`）→ ✅ 可以，跳到 2-2
   - 顯示 `1.8`、`11`，或出現「無法辨識 java」→ ❌ 需要安裝新版 Java：
     1. 到 <https://adoptium.net/>，按大大的下載按鈕（會自動選 Windows 版的最新 LTS）
     2. 執行安裝檔，安裝過程中如果看到 **Set JAVA_HOME variable**，把它改成「Will be installed」（勾起來）
     3. 安裝完**關掉 PowerShell 再重開**，重新輸入 `java -version` 確認

### 2-2 安裝 VS Code 擴充套件

1. 打開 VS Code
2. 點左邊工具列的「方塊」圖示（Extensions），或按 `Ctrl + Shift + X`
3. 搜尋並安裝這兩個（按 **Install**）：
   - **Extension Pack for Java**（Microsoft 出的）
   - **Spring Boot Extension Pack**（VMware 出的）

---

## 3. 建立專案

我們用 Spring 官方的網頁工具，幫我們產生一個「空白的 Spring Boot 專案」。

1. 打開瀏覽器，進入 https://start.spring.io
2. 照下面設定（**沒提到的欄位不用動**）：

   | 畫面上的欄位 | 要選 / 要填 |
   | --- | --- |
   | Project | 點 **Maven** |
   | Language | 點 **Java** |
   | Spring Boot | 不用動（用預設的就好） |
   | Group | 改成 `com.example` |
   | Artifact | 改成 `bmi` |
   | Packaging | **Jar** |
   | Java | 選 **17** 或 **21**（你的 Java 版本要 ≥ 這個數字） |

   改完 Artifact 後，下面的 **Package name** 會自動變成 `com.example.bmi`，請確認一下。

3. 加入需要的套件：
   1. 按右邊的 **ADD DEPENDENCIES...**
   2. 搜尋框輸入 `web`，點選 **Spring Web**
   3. 再按一次 **ADD DEPENDENCIES...**，輸入 `thymeleaf`，點選 **Thymeleaf**

   完成後右邊應該列著 **Spring Web** 和 **Thymeleaf** 兩個。

4. 按最下面的 **GENERATE**，瀏覽器會下載一個 `bmi.zip`
5. **把 zip 解壓縮到一個簡單的位置**，例如：

   ```
   C:\projects\bmi
   ```

   > ⚠️ **建議不要**放在 Google 雲端硬碟 / OneDrive 的同步資料夾，也**不要**放在含有中文或空白的路徑下。
   > 專案執行時會產生很多檔案，放在雲端同步資料夾會很慢；中文路徑偶爾會讓工具出錯。

   解壓縮後，`C:\projects\bmi` 裡面應該**直接**看到 `pom.xml`、`mvnw.cmd`、`src` 這些東西。
   如果多包了一層（變成 `C:\projects\bmi\bmi\pom.xml`），也沒關係，之後打開的是**有 `pom.xml` 的那一層**就好。

---

## 4. 用 VS Code 打開專案

1. VS Code 上方選單：**File → Open Folder...**
2. 選 `C:\projects\bmi`（有 `pom.xml` 的那一層），按「選擇資料夾」
3. 如果跳出「Do you trust the authors...?」→ 按 **Yes, I trust the authors**
4. **等它準備好**：第一次打開時，VS Code 會在背景下載 Spring Boot 需要的套件（右下角會有轉圈圈或進度訊息）。這可能要 **1～5 分鐘**，視網路速度而定。下載完之前程式碼出現紅色底線是正常的，**等一下就會消失**。

打開後，左邊的檔案總管長這樣：

```
BMI
 ├── .mvn
 ├── src
 │   ├── main
 │   │   ├── java
 │   │   │   └── com
 │   │   │       └── example
 │   │   │           └── bmi
 │   │   │               └── BmiApplication.java   ← 已經幫你寫好了，不用改
 │   │   └── resources
 │   │       ├── static          ← 等一下 index.html 放這裡
 │   │       ├── templates
 │   │       └── application.properties
 │   └── test
 ├── mvnw
 ├── mvnw.cmd
 └── pom.xml
```

> 💡 `BmiApplication.java` 是整個網站的「啟動開關」，內容和 [bonus.md](bonus.md) 裡的一模一樣，不需要修改。

---

## 5. 新增程式檔案

要新增 3 個檔案。**檔案放的位置非常重要**，放錯地方網站會找不到它們。

> ⚠️ **複製程式碼時，不要把 \`\`\`java、\`\`\`html 和最後的 \`\`\` 一起複製進去。**
> 那是 Markdown 用來標示「這裡是一段程式碼」的符號，不是程式的一部分。如果在 VS Code 直接打開 `bonus.md` 複製，很容易不小心選到它們；
> 貼上後請確認：Java 檔的第一行是 `package ...`，`index.html` 的第一行是 `<!DOCTYPE html>`。
> （如果 `index.html` 多貼了 \`\`\`html，網頁左邊就會多出一串 \`\`\`html 文字。）

### 5-1 新增 `BmiService.java`（負責計算）

1. 在左邊檔案總管，對 `src/main/java/com/example/bmi` 這個資料夾**按右鍵 → New Folder**，輸入 `service`
2. 對剛建好的 `service` 資料夾**按右鍵 → New File**，輸入 `BmiService.java`
3. 打開 [bonus.md](bonus.md)，找到 **## BmiService.java** 這一段，把程式碼**整段複製**，貼進去
4. `Ctrl + S` 存檔

### 5-2 新增 `BmiController.java`（負責接收請求）

1. 對 `src/main/java/com/example/bmi` **按右鍵 → New Folder**，輸入 `controller`
2. 對 `controller` 資料夾**按右鍵 → New File**，輸入 `BmiController.java`
3. 從 [bonus.md](bonus.md) 的 **## BmiController.java** 段落複製程式碼貼上
4. `Ctrl + S` 存檔

### 5-3 新增 `index.html`（網頁畫面）

1. 對 `src/main/resources/static` 資料夾**按右鍵 → New File**，輸入 `index.html`
2. 從 [bonus.md](bonus.md) 的 **## index.html** 段落複製程式碼貼上
3. `Ctrl + S` 存檔

### 5-4 檢查：完成後應該長這樣

```
src
 └── main
     ├── java
     │   └── com
     │       └── example
     │           └── bmi
     │               ├── BmiApplication.java
     │               ├── controller
     │               │   └── BmiController.java     ← 新增
     │               └── service
     │                   └── BmiService.java        ← 新增
     └── resources
         ├── static
         │   └── index.html                         ← 新增
         ├── templates
         └── application.properties
```

再打開兩個 Java 檔確認**第一行**：

| 檔案 | 第一行必須是 |
| --- | --- |
| `BmiController.java` | `package com.example.bmi.controller;` |
| `BmiService.java` | `package com.example.bmi.service;` |

> 💡 **為什麼位置這麼重要？**
> Spring Boot 啟動時，只會去 `BmiApplication.java` 所在的資料夾（`com/example/bmi`）**和它底下的子資料夾**找程式。放在外面的檔案它根本不會看到。

---

## 6. 啟動網站

1. 打開 `BmiApplication.java`
2. 在 `public static void main` 那一行的**正上方**，會看到一行小小的灰色文字 **Run | Debug**
3. 點 **Run**
4. VS Code 下方會跳出終端機，開始跑出一大串文字。等到最後出現類似這兩行，就代表**啟動成功**：

   ```
   Tomcat started on port 8080 (http) with context path '/'
   Started BmiApplication in 2.345 seconds
   ```

> 💡 **程式跑完沒有結束、一直停在那裡，是正常的！**
> 以前寫的作業程式跑完就結束了，但「伺服器」的工作就是**一直開著等別人來連線**。測試期間請讓它繼續跑，不要關掉。
>
> 想停止時：按終端機上方的紅色方塊 ⏹，或在終端機裡按 `Ctrl + C`。

<details>
<summary>看不到 Run 按鈕？改用指令啟動（點我展開）</summary>

1. VS Code 上方選單 **Terminal → New Terminal**
2. 確認終端機目前的位置是專案資料夾（有 `pom.xml` 的那層），然後輸入：

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

3. 第一次執行會下載很多東西，請耐心等待，直到看到 `Started BmiApplication`

</details>

---

## 7. 打開網頁測試

### 7-1 先測後端有沒有正常算

打開瀏覽器，網址列貼上下面這行，按 Enter：

```
http://localhost:8080/api/bmi?height=170&weight=65
```

應該會看到一行文字（這就是 JSON）：

```json
{"bmi":22.49134948096886,"category":"正常範圍"}
```

✅ 看到這行 → 後端（Controller + Service）都沒問題
❌ 看到錯誤頁 → 跳到 [第 9 節](#9-卡住了怎麼辦)

### 7-2 測網頁

網址列改成：

```
http://localhost:8080/
```

1. 身高輸入 `170`
2. 體重輸入 `65`
3. 按 **計算BMI**
4. 按鈕下方出現 **BMI: 22.49 (正常範圍)** 🎉

---

## 8. 截圖並上傳

1. 確認畫面上**同時看得到**：身高、體重的輸入框（有填數字）＋ 下方的 BMI 結果
2. 按 `Win + Shift + S`，拖曳框選網頁區域
3. 截完後右下角會跳出通知，點它 → 按儲存圖示存成檔案
4. 上傳到 Moodle

做完就可以回到 VS Code 把伺服器停掉（⏹ 或 `Ctrl + C`）。

---

## 9. 卡住了怎麼辦？

先看**錯誤訊息裡的關鍵字**，對照下表：

| 你看到的狀況 | 原因 | 怎麼解決 |
| --- | --- | --- |
| 程式碼一堆紅色底線，`org.springframework` 找不到 | VS Code 還在下載套件 | 等右下角的進度跑完。等很久還是紅的 → 按 `Ctrl + Shift + P`，輸入 `Java: Clean Java Language Server Workspace`，選它後按 Restart |
| 啟動時出現 `Port 8080 was already in use` | 8080 號房有人住了（通常是你上次沒關掉的伺服器） | 先把之前的終端機都關掉再重跑；或打開 `application.properties`，加一行 `server.port=8081`，之後網址改用 `http://localhost:8081/` |
| 啟動時出現 `invalid target release` 或 `wrong version` | Java 版本太舊，或比 start.spring.io 選的版本小 | 回到 [2-1](#2-1-檢查-java-版本) 安裝新版 Java |
| 打開 `http://localhost:8080/` 出現 **Whitelabel Error Page** | 找不到 `index.html` | 確認它在 `src/main/resources/static/` 裡，檔名是 `index.html`（不是 `index.html.txt`） |
| 打開 `/api/bmi?...` 出現 **Whitelabel Error Page** 和 `404` | 找不到 Controller，或 Controller 有編譯錯誤（例如 `BmiService.java` 放錯資料夾，Controller 找不到它）。VS Code 遇到編譯錯誤時**還是會讓伺服器啟動**，所以看起來像正常 | 看 VS Code 下方「**問題 (Problems)**」分頁旁邊的數字，不是 0 就點進去看錯誤；照 [5-4](#5-4-檢查完成後應該長這樣) 檢查資料夾位置和第一行的 `package`。修好後**停止伺服器再重新 Run** |
| 瀏覽器顯示「無法連上這個網站」 | 伺服器沒有在跑 | 回到 VS Code 確認終端機有 `Started BmiApplication`，而且沒有被關掉 |
| 網頁顯示「計算失敗，請檢查輸入值。」 | 網頁呼叫後端失敗 | 先用 [7-1](#7-1-先測後端有沒有正常算) 測後端；後端正常的話，按 `F12` → Console 分頁看紅字錯誤 |
| 中文變成亂碼（例如 `æ­£å¸¸`） | 檔案編碼不是 UTF-8 | 看 VS Code 右下角狀態列，點編碼名稱 → **Save with Encoding** → 選 **UTF-8** |
| 改了程式，網頁卻沒變 | 伺服器還在跑舊的程式 | 停止伺服器（⏹）再重新 Run；瀏覽器按 `Ctrl + F5` 強制重新整理 |

還是解決不了的話，把**終端機最後的錯誤訊息**（通常是紅字、或 `ERROR` / `Exception` 開頭的那幾行）截圖或複製下來，詢問助教。
