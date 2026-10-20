# HW3 一步步完成教學（給會 Python、沒學過 Java 的人）

> 目標：輸入一個網址和一個關鍵字，印出關鍵字在該網頁原始碼中出現幾次（不分大小寫）。
> Bonus：用 **Boyer-Moore** 演算法來找字串。

```
Please type URL and Keyword:
http://soslab.nccu.edu.tw/Welcome.html Fang
Fang appears 32 times.
```

---

## 0. 先看懂你手上有什麼

`HW3_110208064_bonus/` 裡有兩個檔案，**大部分已經寫好了**，你只要填三個地方：

| 檔案 | 在做什麼 | 你要改嗎？ |
|---|---|---|
| `Main.java` | 讀使用者輸入的 URL 和關鍵字，建立 `WordCounter`，印出結果 | 不用 |
| `WordCounter.java` | 下載網頁內容 (`fetchContent`)、數關鍵字 (`countKeyword`) | **要，填 3 個 TODO** |

要填的三個地方：

1. `last(char c, String P)` — Boyer-Moore 的「最後出現位置」函式
2. `BoyerMoore(String T, String P)` — Boyer-Moore 主體
3. `countKeyword(...)` 裡的 `// 1. calculates appearances of keyword` — 呼叫上面的函式把結果存進 `retVal`

---

## 1. 環境準備：確認可以編譯、執行 Java

打開終端機（PowerShell 或 VS Code 的 Terminal），輸入：

```
java -version
javac -version
```

兩個都有印出版本就 OK（你電腦上已經有 Java 26）。

> Python 是「直接跑」，Java 要先 **編譯** 再 **執行**：
>
> | Python | Java |
> |---|---|
> | `python main.py` | `javac *.java` （編譯，產生 `.class` 檔）<br>`java Main` （執行，注意**不加** `.java`） |

進到作業資料夾，先試著編譯原始版本：

```
cd "G:\我的雲端硬碟\六上\Data Structure\ta-github\2026Fall-DS\student\HW3\HW3_110208064_bonus"
javac *.java
java Main
```

輸入 `http://soslab.nccu.edu.tw/Welcome.html Fang`，應該會印出 `Fang appears 0 times.`（因為還沒實作）。能跑到這步就代表環境沒問題。

> 編譯時看到 `Note: WordCounter.java uses or overrides a deprecated API.` 不用管，那是 `new URL(...)` 在新版 Java 被標為過時的**警告**，不是錯誤。
>
> 程式會一直等你輸入下一行；要結束按 `Ctrl + C`。

---

## 2. Java 速成：只學這份作業會用到的

### 2.1 型別要寫出來

```java
int n = 5;            // Python: n = 5
String s = "hello";   // Python: s = "hello"
char c = 'h';         // 單一字元，用單引號！Python 沒有 char 這種型別
```

### 2.2 字串操作對照表

| 想做的事 | Python | Java |
|---|---|---|
| 長度 | `len(s)` | `s.length()` |
| 第 i 個字元 | `s[i]` | `s.charAt(i)` （回傳 `char`） |
| 比較兩個字元 | `a == b` | `a == b` （`char` 可以直接用 `==`） |
| 比較兩個字串 | `a == b` | `a.equals(b)` （**不能**用 `==`） |
| 轉大寫 | `s.upper()` | `s.toUpperCase()` |
| 找子字串位置 | `s.find(p, start)` | `s.indexOf(p, start)`（找不到回傳 `-1`） |

### 2.3 迴圈、條件

```java
// Python: for k in range(len(P) - 1, -1, -1):
for (int k = P.length() - 1; k >= 0; k--) {
    ...
}

// Python: while i <= n - 1:
while (i <= n - 1) {
    if (...) {
        ...
    } else if (...) {
        ...
    } else {
        ...
    }
}
```

- 每句結尾要加 `;`
- 區塊用 `{ }`，不是縮排（但還是請好好縮排）
- `i++` 等於 `i += 1`，`i--` 等於 `i -= 1`
- 邏輯運算：`&&`（and）、`||`（or）、`!`（not）

### 2.4 函式（method）

```java
public int last(char c, String P) {   // 回傳 int，吃一個 char 和一個 String
    ...
    return -1;
}
```

等同 Python 的 `def last(self, c, P) -> int:`。在同一個 class 裡呼叫別的 method，直接寫 `last(x, P)` 就好（不用 `self.`）。

---

## 3. 先做「基本版」（非 Bonus）—— 確認整條流程是通的

在寫 Boyer-Moore 之前，建議先用最簡單的方法把答案做出來，拿到正確答案 `32` 當作對照組。

打開 `WordCounter.java`，找到 `countKeyword` 裡這行：

```java
		// 1. calculates appearances of keyword (Bonus: Implement Boyer-Moore Algorithm)
```

在它下面加入：

```java
		int idx = content.indexOf(keyword);
		while (idx != -1) {
			retVal++;
			idx = content.indexOf(keyword, idx + 1);
		}
```

Python 等價寫法：

```python
idx = content.find(keyword)
while idx != -1:
    retVal += 1
    idx = content.find(keyword, idx + 1)
```

重新編譯、執行，輸入範例，應該會看到：

```
Fang appears 32 times.
```

✅ 成功後，這段先留著或註解掉，接下來換成 Boyer-Moore。

> 注意：`content` 和 `keyword` 在這行之前已經被轉成大寫了，所以是「不分大小寫」的搜尋。

---

## 4. 理解 Boyer-Moore（Bonus 的核心）

### 4.1 直覺

暴力法是把 pattern `P` 從左到右一格一格在 text `T` 上滑動。Boyer-Moore 有兩個聰明的地方：

1. **從 pattern 的「最右邊」往左比對**（looking-glass heuristic）
2. **比對失敗時，一次跳很多格**（character-jump heuristic）：看 `T` 中對不上的那個字元 `c`，
   - 如果 `c` 在 `P` 裡有出現 → 把 `P` 移到讓「`P` 中最後一個 `c`」對齊它
   - 如果 `c` 根本不在 `P` 裡 → 直接把整個 `P` 跳過這個字元

### 4.2 變數意義

- `n = T.length()`、`m = P.length()`
- `i`：目前正在看 `T` 的哪個位置
- `j`：目前正在看 `P` 的哪個位置
- 一開始 `i = m - 1`、`j = m - 1`（都從 pattern 的最右端開始）

### 4.3 `last(c, P)` 函式

回傳字元 `c` 在 `P` 中**最後一次**出現的 index；沒出現就回傳 `-1`。

例：`P = "FANG"`

| c | last(c, P) |
|---|---|
| `'F'` | 0 |
| `'A'` | 1 |
| `'N'` | 2 |
| `'G'` | 3 |
| `'X'` | -1 |

例：`P = "ABAB"` → `last('A') = 2`、`last('B') = 3`

### 4.4 教科書版演算法（找到「第一個」符合的位置）

```
i = m - 1
j = m - 1
while i <= n - 1:
    if T[i] == P[j]:
        if j == 0:
            return i            # 找到了！i 是符合的起點
        else:
            i -= 1
            j -= 1
    else:                       # 對不上 → 跳
        l = last(T[i], P)
        i = i + m - min(j, 1 + l)
        j = m - 1
return -1                       # 沒找到
```

跳躍公式 `i = i + m - min(j, 1 + l)` 包含兩種情況：
- **Case 1：`1 + l <= j`**（`c` 在 `P` 中出現在 `j` 的左邊）→ 讓 `P[l]` 對齊 `T[i]`
- **Case 2：`j < 1 + l`**（`c` 出現在 `j` 的右邊，對齊會讓 P 往回走）→ 只保守地往右移一格

`min` 函式作業已經幫你寫好了。

### 4.5 手動跑一次（強烈建議自己畫）

`T = "XXFANGXFANG"`、`P = "FANG"`，`m = 4`

```
index:  0 1 2 3 4 5 6 7 8 9 10
T:      X X F A N G X F A N G
```

1. `i=3, j=3`：`T[3]='A'` vs `P[3]='G'` ✗ → `l = last('A') = 1`，`i = 3 + 4 - min(3, 2) = 5`，`j = 3`
2. `i=5, j=3`：`'G'=='G'` ✓ → `i=4, j=2`：`'N'=='N'` ✓ → `i=3,j=1` ✓ → `i=2,j=0`：`'F'=='F'` ✓ 且 `j==0` → **找到，起點 2**

教科書版到這裡就 `return` 了，但我們要**數次數**，所以要改一下 ↓

---

## 5. 把 Boyer-Moore 改成「數次數」

作業給的 `BoyerMoore(String T, String P)` 回傳 `int`，最簡單的設計是：**直接讓它回傳出現次數**。

改動只有一個地方：找到的時候不要 `return`，而是

1. `count++`
2. 把 pattern 往右移一格，繼續找

「往右移一格」怎麼算？找到時 `i` 指向符合的**起點** `s`。下一個要檢查的對齊位置起點是 `s + 1`，它的**右端**在 `s + 1 + (m - 1) = s + m`。所以：

```
i = i + m
j = m - 1
```

> 💡 **重疊的問題**：用上面的寫法，`"AAA"` 裡找 `"AA"` 會算到 2 次（重疊也算），跟第 3 步 `indexOf(keyword, idx + 1)` 的結果一致。
> 如果你想要「不重疊」（算 1 次），找到後改成跳到 `s + m` 當新起點，也就是 `i = i + 2*m - 1`。
> 題目沒有規定，挑一種、**跟基本版結果對得上**，並在程式註解說明即可。對 `Fang` 這種字兩種寫法結果一樣。

另外要處理邊界：`m == 0` 或 `m > n` 時直接回傳 0。

---

## 6. 動手寫！

建議順序：先寫 `last` → 再寫 `BoyerMoore` → 最後在 `countKeyword` 呼叫。每寫完一段就 `javac *.java` 編譯一次，早點抓到語法錯誤。

### 6.1 `last`

提示：從 `P` 的最後一個字元往前掃，遇到等於 `c` 的就回傳 index；掃完都沒有就回傳 `-1`。（第 2.3 節的倒數 for 迴圈就是你要的）

<details>
<summary>卡住了？點開看參考答案</summary>

```java
    public int last(char c, String P){
        for (int k = P.length() - 1; k >= 0; k--) {
            if (P.charAt(k) == c)
                return k;
        }
        return -1;
    }
```

</details>

### 6.2 `BoyerMoore`

提示：把第 4.4 節的虛擬碼逐行翻成 Java，再套用第 5 節的修改。記得 `T[i]` 要寫成 `T.charAt(i)`。

<details>
<summary>卡住了？點開看參考答案</summary>

```java
    public int BoyerMoore(String T, String P){
        int n = T.length();
        int m = P.length();
        if (m == 0 || m > n)
            return 0;

        int count = 0;
        int i = m - 1;
        int j = m - 1;

        while (i <= n - 1) {
            if (T.charAt(i) == P.charAt(j)) {
                if (j == 0) {
                    // found a match starting at i; count it and slide P right by one
                    count++;
                    i = i + m;
                    j = m - 1;
                } else {
                    i--;
                    j--;
                }
            } else {
                // character-jump heuristic
                int l = last(T.charAt(i), P);
                i = i + m - min(j, 1 + l);
                j = m - 1;
            }
        }
        return count;
    }
```

</details>

### 6.3 在 `countKeyword` 呼叫

把第 3 步的 `indexOf` 版本換掉（或註解掉），改成一行：

<details>
<summary>點開看答案</summary>

```java
		retVal = BoyerMoore(content, keyword);
```

</details>

---

## 7. 測試

```
javac *.java
java Main
```

| 輸入 | 預期輸出 |
|---|---|
| `http://soslab.nccu.edu.tw/Welcome.html Fang` | `Fang appears 32 times.` |
| `http://soslab.nccu.edu.tw/Welcome.html fang` | `fang appears 32 times.`（不分大小寫） |
| `http://soslab.nccu.edu.tw/Welcome.html aa` | 重疊算法：`aa appears 7 times.` |

建議再自己多試幾個：
- 一個網頁裡絕對不存在的字 → 應該是 `0`
- 換一個網址（例如某個 `https://` 網頁）
- 把 Boyer-Moore 的結果和第 3 步 `indexOf` 版本的結果互相比對，**兩者應該一樣**

### 常見錯誤

| 症狀 | 可能原因 |
|---|---|
| `error: ';' expected` | 少了分號 |
| `error: incompatible types: String cannot be converted to char` | 用了 `"A"`（字串）而不是 `'A'`（字元） |
| 程式卡住不動、沒輸出 | `while` 迴圈裡 `i` 沒有前進 → 無窮迴圈，檢查找到時有沒有更新 `i` |
| `StringIndexOutOfBoundsException` | `charAt` 的 index 超出範圍，檢查 `while` 條件和 `m > n` 的邊界 |
| `UnknownHostException` / 連線錯誤 | 網址打錯或沒網路 |
| 用管線 (`echo ... \| java Main`) 輸入時最後噴 `NoSuchElementException` | 是 `Main.java` 用 `hasNextLine()` + `next()` 造成的，在輸入結尾才發生，答案已正確印出，不影響 |

---

## 8. 繳交

- 因為有做 Bonus，壓縮檔命名為 **`HW3_學號_bonus.zip`**（你的是 `HW3_110208064_bonus.zip`）
- 壓縮檔裡放 `Main.java`、`WordCounter.java`（`.class` 檔通常不用，詳細規定以 Moodle 公告為準）
- 上傳 Moodle，**不接受直接複製的程式碼**，每一行都要自己看懂
- 截止：**10/5 (Mon.) 23:59**
- 完整評分與繳交規定請看 Moodle 上的「HW3 Grading & Submission Guidelines」公告

---

## 檢查清單

- [ ] `javac *.java` 編譯沒有 error
- [ ] 基本版 (`indexOf`) 範例得到 `32`
- [ ] `last()` 完成，手算幾個例子確認
- [ ] `BoyerMoore()` 完成，結果跟基本版一樣
- [ ] `countKeyword()` 改成呼叫 `BoyerMoore()`
- [ ] 大小寫不同的關鍵字結果相同
- [ ] 能用自己的話解釋跳躍公式 `i + m - min(j, 1 + l)`
- [ ] 打包成 `HW3_110208064_bonus.zip` 上傳 Moodle
