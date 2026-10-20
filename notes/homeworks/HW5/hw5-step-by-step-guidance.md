# HW5 Step-by-Step Guidance：用 Stack 檢查 HTML 標籤是否配對

> 這份文件從零開始講：先介紹什麼是 Stack、什麼是 HTML 標籤，再一步一步帶你把程式寫完。
> 題目規格請以 [hw5.md](hw5.md) 為準。

---

## 0. 一句話說明這份作業

> **給一個網址 → 把網頁原始碼抓下來 → 檢查裡面的 `<標籤>` 有沒有「一開一關」正確配對。**

要完成它，你需要先懂兩件事：**Stack 是什麼**、**HTML 標籤長什麼樣子**。下面先分別介紹。

---

## 1. 什麼是 Stack（堆疊）？

### 1-1 用「疊盤子」來想

想像餐廳裡一疊盤子：

- 洗好的盤子只能**放在最上面**
- 要拿盤子也只能**從最上面拿**
- 你沒辦法直接抽出中間或最底下那一個

```
   ┌─────┐  ← 頂端 (top)：最後放上去的，也是第一個會被拿走的
   │  C  │
   ├─────┤
   │  B  │
   ├─────┤
   │  A  │  ← 底部：最早放上去的，最後才會被拿走
   └─────┘
```

這種「**最後放進去的，最先拿出來**」的特性叫做 **LIFO（Last In, First Out）**，這就是 Stack。

### 1-2 Stack 的三個基本動作

| 動作 | 意思 | 比喻 |
| --- | --- | --- |
| **push** | 把東西放到最上面 | 放一個盤子上去 |
| **pop** | 把最上面的東西拿走 | 拿走最上面的盤子 |
| **peek** | 看一下最上面是什麼，但**不拿走** | 瞄一眼最上面的盤子 |

### 1-3 實際操作一次

```
一開始          push(A)        push(B)        push(C)        pop()          peek()
                                              ┌───┐
                               ┌───┐          │ C │          ┌───┐          ┌───┐
               ┌───┐           │ B │          │ B │          │ B │          │ B │ ← 回傳 B，但 B 還在
  (空)         │ A │           │ A │          │ A │          │ A │          │ A │
               └───┘           └───┘          └───┘          └───┘          └───┘
                                                          拿走了 C
```

### 1-4 在 Java 裡怎麼用 Stack

```java
import java.util.Stack;

Stack<String> stack = new Stack<>();   // 建立一個裝 String 的空 stack

stack.push("A");          // 放入 A          → [A]
stack.push("B");          // 放入 B          → [A, B]（B 在頂端）
String top = stack.peek(); // 看頂端是什麼    → top = "B"，stack 不變
stack.pop();              // 拿走頂端         → [A]
stack.isEmpty();          // 是不是空的？     → false
```

本作業只會用到 `push`、`pop`、`peek`、`isEmpty` 這四個方法。

---

## 2. Stack 可以拿來做什麼？—— 括號配對

這是理解本作業**最關鍵**的一節。

### 2-1 問題

判斷一串括號是不是正確配對：

| 例子 | 正確嗎？ | 原因 |
| --- | --- | --- |
| `( [ ] )` | ✅ | 每個左括號都有對應的右括號，順序也對 |
| `( ]` | ❌ | `(` 和 `]` 種類不同 |
| `( [ ) ]` | ❌ | `[` 還沒關，就先關 `(`，順序錯了 |

規律是：**最後打開的括號，必須最先被關閉。** 這正好就是 Stack「後進先出」的特性！

### 2-2 做法

從左到右一個一個看：

1. 看到**左括號** → push 進 stack
2. 看到**右括號** → 看 stack 頂端是不是對應的左括號
   - 是 → pop 掉（這一對配對成功）
   - 不是 → **不合法**
3. 全部看完後，stack 是空的 → **合法**；還有剩 → 有括號沒被關 → **不合法**

### 2-3 實際跑一次：`( [ ] )`

| 讀到 | 動作 | stack（左邊是底、右邊是頂） |
| --- | --- | --- |
| `(` | 左括號 → push | `(` |
| `[` | 左括號 → push | `( [` |
| `]` | 右括號，頂端是 `[` → 配對成功，pop | `(` |
| `)` | 右括號，頂端是 `(` → 配對成功，pop | （空） |
| 結束 | stack 是空的 | ✅ 合法 |

### 2-4 再跑一次：`( [ ) ]`

| 讀到 | 動作 | stack |
| --- | --- | --- |
| `(` | push | `(` |
| `[` | push | `( [` |
| `)` | 右括號，但頂端是 `[`，不是 `(` | ❌ 不合法，停止 |

> 💡 **HW5 就是這個問題**，只是把 `(` 換成 `<div>`、把 `)` 換成 `</div>`。只要懂了這一節，作業就懂一大半了。

---

## 3. 什麼是 HTML 標籤？

網頁的原始碼叫 HTML，裡面用 `<...>` 包起來的東西叫**標籤（tag）**。在瀏覽器上按右鍵 →「檢視網頁原始碼」就可以看到。

```html
<html>
  <head>
    <title>我的網頁</title>
  </head>
  <body>
    <p>Hello</p>
  </body>
</html>
```

### 3-1 開標籤和閉標籤

| 種類 | 長相 | 對應到括號 |
| --- | --- | --- |
| 開標籤 | `<p>` | 左括號 `(` |
| 閉標籤 | `</p>`（多一個 `/`） | 右括號 `)` |

`<p>` 要用 `</p>` 關閉，`<title>` 要用 `</title>` 關閉，而且**後開的要先關**——跟括號一模一樣。

### 3-2 標籤裡可以有「屬性」

```html
<a href="http://www.google.com">
```

`<a` 後面空一格接著的 `href="..."` 是屬性（額外資訊）。**標籤的名稱只算 `<` 後面到第一個空白之前**，所以這個標籤叫 `a`。它的閉標籤一樣是 `</a>`。

### 3-3 要跳過的標籤

| 標籤 | 為什麼跳過 |
| --- | --- |
| `<meta ...>` | 描述網頁資訊用的，沒有閉標籤 |
| `<!doctype html>` | 宣告這是 HTML 文件，沒有閉標籤 |

---

## 4. 把兩件事合起來：本作業的規則

| 遇到 | 做什麼 |
| --- | --- |
| 開標籤 `<div>` | `push("div")` |
| 閉標籤 `</div>`，而 stack 頂端是 `div` | 配對成功 → `pop()` |
| 閉標籤 `</div>`，而 stack 頂端**不是** `div` | ❌ 印 `False` 和整個 stack，結束 |
| 閉標籤 `</div>`，而 stack 是空的 | ❌ 印 `False`，結束 |
| `meta`、`!doctype` | 跳過 |
| 全部看完，stack 是空的 | ✅ 印 `True` |
| 全部看完，stack 還有東西 | ❌ 印 `False` 和整個 stack |

**輸出格式：** `False` 後面空一格，接著把 stack 的內容**由底到頂**印在同一行，用空白隔開。例如：

```
False html head link
```

意思是 stack 由底到頂是 `html`、`head`、`link`，最上面的 `link` 就是出問題的那個標籤。

---

## 5. 看懂範本程式

範本有兩個檔案：

| 檔案 | 要改嗎？ | 說明 |
| --- | --- | --- |
| `Main.java` | ❌ 不用改 | 讀入網址，交給 `HtmlMatcher` 檢查 |
| `HtmlMatcher.java` | ✏️ **要改** | 「抓網頁」和「最後判斷 True / False」已經寫好，你只要補 `match()` 裡標了 **1～9 號**的空格 |

`match()` 的整體流程：

```java
content = 整個網頁的原始碼（一個很長的字串）
int indexOfOpen = 0;

while (從 indexOfOpen 往後還找得到 "<") {
    // 空格 1：     把這個 <...> 整個取出來，叫 fullTag
    // 空格 2、3：  從 fullTag 取出標籤名稱，叫 tagName
    // （範本已寫）是 meta 或 !doctype 就跳過
    // 空格 4：     開標籤 → push
    // 空格 5～8：  閉標籤 → 和 stack 頂端比對
    // 空格 9：     往後移，準備找下一個標籤
}

// （範本已寫）全部看完後，判斷 stack 是不是空的，印 True 或 False
```

### 5-1 會用到的字串方法

把字串想成一格一格的字，**每一格有編號，從 0 開始**：

```
字串:   <  l  i  >
編號:   0  1  2  3
```

| 方法 | 做什麼 | 例子 |
| --- | --- | --- |
| `s.indexOf("x", from)` | 從第 `from` 格開始往後找 `"x"`，回傳它在第幾格；找不到回傳 `-1` | `"<li>".indexOf(">", 0)` → `3` |
| `s.substring(a, b)` | 取出第 `a` 格到第 `b-1` 格（**不包含第 b 格**） | `"<li>".substring(1, 3)` → `"li"` |
| `s.substring(a)` | 從第 `a` 格取到最後 | `"/li".substring(1)` → `"li"` |
| `s.length()` | 字串長度 | `"<li>".length()` → `4` |

---

## 6. 一格一格補空格

### 空格 1：取出完整的標籤

`indexOfOpen` 是目前這個 `<` 的位置。從這裡往後找到第一個 `>`，兩者之間（包含 `<` 和 `>`）就是一個完整標籤。

```java
int indexOfClose = content.indexOf(">", indexOfOpen);              // 找到 > 在哪
String fullTag = content.substring(indexOfOpen, indexOfClose + 1); // 取出 <...>
```

為什麼要 `+1`？因為 `substring` 不包含結束那一格，不 `+1` 就會少掉 `>`。

```
content:  . . . < d i v > . . .
編號:          10 11 12 13 14
indexOfOpen = 10, indexOfClose = 14
substring(10, 15) → "<div>"
```

> ⚠️ 變數名稱一定要叫 `indexOfClose`，因為範本後面跳過 `meta` 的程式碼有用到它。

### 空格 2：標籤裡沒有空白 → 去掉頭尾

例如 `<li>`、`</div>`，把第一個字 `<` 和最後一個字 `>` 去掉就是名稱：

```java
tagName = fullTag.substring(1, fullTag.length() - 1);
```

| fullTag | tagName |
| --- | --- |
| `<li>` | `li` |
| `</li>` | `/li` ← 斜線先留著，下一步要靠它判斷是不是閉標籤 |

### 空格 3：標籤裡有空白 → 取到空白前

例如 `<a href="...">`，名稱是 `<` 後面到第一個空白之前：

```java
tagName = fullTag.substring(1, indexOfSpace);
```

| fullTag | tagName |
| --- | --- |
| `<li id='theID'>` | `li` |
| `<a href='http://www.google.com.tw/'>` | `a` |

`indexOfSpace`（第一個空白的位置）範本已經在 `if` 條件裡算好了，直接用就可以。

### 空格 4：開標籤 → 放進 stack

`tagName` 裡沒有 `/`，代表是開標籤，就像遇到左括號：

```java
tagStack.push(tagName);
```

### 空格 5：閉標籤 → 先把斜線拿掉

stack 裡放的是 `"div"`，但閉標籤的名稱是 `"/div"`，兩個不一樣。要先把 `/` 拿掉才能比對：

```java
tagName = tagName.substring(1);   // "/div" → "div"
```

> ⚠️ 這步最常忘記。忘了的話每個閉標籤都會配對失敗。

### 空格 6：stack 是空的

遇到閉標籤，但 stack 裡沒有任何開標籤可以配對（就像一開始就遇到 `)`）：

```java
System.out.println("False");
```

（下一行的 `return;` 範本已經寫好了，作用是直接結束這個方法。）

### 空格 7：頂端名稱一樣 → 配對成功

就像 `(` 遇到 `)`，這一對完成了，把頂端拿掉：

```java
tagStack.pop();
```

### 空格 8：頂端名稱不一樣 → 配對失敗

印出 `False` 和目前整個 stack。範本已經提供 `getStackString()`，會幫你把 stack 由底到頂、用空白隔開變成一個字串：

```java
System.out.println("False " + getStackString(tagStack));
```

（`return;` 範本已經寫好了。）

### 空格 9：往後移，找下一個標籤

把搜尋起點移到這個標籤的 `>`，下一輪迴圈就會從這裡往後找下一個 `<`：

```java
indexOfOpen = indexOfClose;
```

> ⚠️ 忘了這行的話，每一輪都會找到同一個 `<`，程式會**永遠跑不完**（無窮迴圈）。

---

## 7. 編譯與執行

在 `HtmlMatcher.java` 和 `Main.java` 所在的資料夾打開終端機：

```bash
javac -encoding UTF-8 *.java
java Main
```

接著輸入網址，一行一個，按 Enter：

```
http://www.example.com
http://info.cern.ch/
http://soslab.nccu.edu.tw/Welcome.html
```

> 💡 編譯時如果出現 `uses or overrides a deprecated API`，那只是提醒，不是錯誤，可以忽略。
>
> 💡 如果輸入的最後多按了一次 Enter，程式結尾可能會跳出 `NoSuchElementException`。這是 `Main.java` 讀輸入的方式造成的，前面的輸出還是正確的。

---

## 8. 對答案

預期輸出（已實際執行驗證）：

```
False html head link
True
False ?xml !DOCTYPE html head link !--[if link ![endif]-- !--[if link ![endif]-- style !--
	@import
```

### 8-1 範例 1 慢動作：`http://www.example.com`

這個網頁原始碼的開頭是：

```html
<!doctype html><html lang=en><head><meta charset=utf-8><link rel=icon href=data:,><meta ...><title>Example Domain</title><style>...</style></head>...
```

一個一個標籤看：

| 讀到 | 標籤名稱 | 做什麼 | stack（左邊是底、右邊是頂） |
| --- | --- | --- | --- |
| `<!doctype html>` | `!doctype` | 跳過 | （空） |
| `<html lang=en>` | `html` | 開標籤 → push | `html` |
| `<head>` | `head` | 開標籤 → push | `html head` |
| `<meta charset=utf-8>` | `meta` | 跳過 | `html head` |
| `<link rel=icon ...>` | `link` | 開標籤 → push | `html head link` |
| `<meta ...>` | `meta` | 跳過 | `html head link` |
| `<title>` | `title` | 開標籤 → push | `html head link title` |
| `</title>` | `title` | 頂端是 `title`，一樣 → pop | `html head link` |
| `<style>` | `style` | 開標籤 → push | `html head link style` |
| `</style>` | `style` | 頂端是 `style`，一樣 → pop | `html head link` |
| `</head>` | `head` | 頂端是 `link`，**不一樣** → ❌ | 印 `False html head link` |

在真正的 HTML 裡，`<link>` 本來就不需要閉標籤。但這份作業的規則假設**每個標籤都要配對**，所以它造成了失敗。這是**預期的結果**，不是你寫錯。

### 8-2 範例 2：`http://info.cern.ch/`

這個網頁很簡單，所有標籤都有正確關閉，最後 stack 是空的 → 印 `True`。

### 8-3 範例 3：為什麼輸出長得這麼怪？

`http://soslab.nccu.edu.tw/Welcome.html` 的輸出看起來很亂，但它就是照範本規則跑出來的正確答案：

- **`?xml`、`!DOCTYPE` 被放進 stack**：範本只跳過**小寫**的 `!doctype`，大寫的 `!DOCTYPE` 不算，所以被當成開標籤。
- **`!--[if`、`![endif]--` 被放進 stack**：這些是 HTML 的註解，但範本沒有特別處理，所以也被當成開標籤。
- **最後一個 `!--` 換了一行才接 `@import`**：範本只用「空白」切出標籤名稱，換行不算空白，所以名稱裡包含了換行，印出來就變成兩行。

> ⚠️ **不要自己「修正」這些規則**（例如改成忽略大小寫、跳過註解），否則你的輸出會和範例不一樣。

---

## 9. 寫完後的檢查清單

- [ ] 空格 1 的 `substring` 有 `+1`，`fullTag` 有包含 `>`
- [ ] 空格 1 的變數叫 `indexOfClose`
- [ ] 空格 5 有把閉標籤的 `/` 拿掉
- [ ] 比較字串用的是 `equals`（範本已寫好，不要改成 `==`）
- [ ] 空格 9 有寫，程式不會跑不完
- [ ] `False` 和 stack 之間有一個空白
- [ ] 沒有自己加規則去跳過 `!DOCTYPE` 或註解
- [ ] 三個範例的輸出都和第 8 節一樣

---

## 10. 繳交

- 上傳 Moodle
- **First Submission**：10/11 (Sun.) 12:00
- **Deadline**：10/19 (Mon.) 00:00
