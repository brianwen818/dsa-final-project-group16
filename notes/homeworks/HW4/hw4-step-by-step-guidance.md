# HW4 Step-by-Step Guidance：維護一個排序好的 Keyword 清單

> 本文件說明 HW4 在做什麼，以及如何一步一步完成。題目規格請以 [hw4.md](hw4.md) 為準。

---

## 0. 作業在做什麼？

你要用 Java 的 `LinkedList<Keyword>` 維護一個**永遠保持排序**的關鍵字清單，並支援「新增 / 查詢輸出 / 刪除」三類操作。

程式會從一個 txt 檔（例如 `input.txt`）一行一行讀入指令，例如：

```txt
add Fang 3 1.5
outputScore
deleteCount 3
```

範本已經提供三個檔案：

| 檔案 | 狀態 | 說明 |
| --- | --- | --- |
| `Main.java` | ✅ 已完成，不需修改 | 讀檔、解析指令、呼叫 `KeywordList` 對應的方法 |
| `Keyword.java` | ✅ 已完成，不需修改 | 一個關鍵字 `[name, count, weight]`，`toString()` 會輸出 `[Fang,3,1.5]` |
| `KeywordList.java` | ✏️ **你要完成的檔案** | 12 個方法裡面是空的（只有註解），要由你實作 |

---

## 1. 先了解 `Keyword` 與輸出格式

```java
Keyword k = new Keyword("Fang", 3, 1.5f);
System.out.println(k);   // [Fang,3,1.5]
```

- `k.name`、`k.count`、`k.weight` 都是 `public`，可以直接存取（也可以用 `getName()` 等 getter）。
- 要一次輸出多個 keyword 時，請使用範本已提供的 `printKeywordList(LinkedList<Keyword>)`，它會用**一個空白**隔開每個 keyword 並換行：

```
[MIS,2,1.2] [UCSB,2,2.2]
```

> 💡 小技巧：「輸出多個」的方法都可以先把符合條件的 keyword 收集到一個暫存的 `LinkedList`，再交給 `printKeywordList` 印出來。

---

## 2. Step 1：`add` —— 依規則插入到正確位置（最重要）

### 排序規則

1. **count 小的在前面**
2. count 相同 → **weight 小的在前面**
3. count 與 weight 都相同 → **保持加入的先後順序**（先加入的在前）

### 思路

從頭開始往後走，找到**第一個比新 keyword「嚴格大」的元素**，把新 keyword 插在它前面；如果都沒有比它大的，就放到最後面。

> ⚠️ 為什麼是「嚴格大於」？
> 如果用「大於等於」，遇到 count、weight 都相同的元素時，新的會被插到舊的**前面**，就違反了第 3 條規則（保持插入順序）。

### 程式碼

```java
public void add(Keyword keyword){
    int idx = 0;
    for (Keyword k : lst) {
        if (k.count > keyword.count ||
           (k.count == keyword.count && k.weight > keyword.weight)) {
            break;          // 找到第一個比新 keyword 大的位置
        }
        idx++;
    }
    lst.add(idx, keyword);  // 插在該位置（idx == size 時等同加到最後）
}
```

### 自我檢查

把範例 `input.txt` 前 9 行 `add` 跑完後，清單應該是：

```
[MIS,2,1.2] [UCSB,2,2.2] [Food,3,0.1] [Data,3,0.3] [NCCU,3,0.8] [Fang,3,1.5] [Structure,4,2.1] [Badminton,4,2.3] [Yu,5,1.2]
```

---

## 3. Step 2：輸出類方法

### 3-1 `outputIndex(int i)` —— 輸出第 i 個（index 從 0 開始）

- `i < 0` 或 `i >= lst.size()` → 印 `InvalidOperation`
- 否則印出 `lst.get(i)`

```java
public void outputIndex(int i){
    if (i < 0 || i >= lst.size()) {
        System.out.println("InvalidOperation");
        return;
    }
    System.out.println(lst.get(i));
}
```

### 3-2 `outputCount(int c)` —— 輸出所有 count == c 的 keyword

- 找不到任何一個 → 印 `NotFound`

```java
public void outputCount(int c){
    LinkedList<Keyword> res = new LinkedList<>();
    for (Keyword k : lst) {
        if (k.count == c) res.add(k);
    }
    if (res.isEmpty()) System.out.println("NotFound");
    else printKeywordList(res);
}
```

### 3-3 `outputHas(String s)` —— 輸出 name **包含** s 的 keyword

和 `outputCount` 幾乎一樣，只是條件換成 `k.name.contains(s)`。

- `contains` 有**區分大小寫**：`"Badminton".contains("a")` 是 true，`"MIS".contains("a")` 是 false。
- 找不到 → 印 `NotFound`

### 3-4 `outputName(String s)` —— 輸出 name **等於** s 的 keyword

條件換成 `k.name.equals(s)`。

> ⚠️ Java 比較字串內容要用 `equals`，**不要用 `==`**（`==` 比的是是否為同一個物件）。

- 找不到 → 印 `NotFound`

### 3-5 `outputFirstN(int n)` —— 輸出前 n 個

- `n > lst.size()` → 印 `InvalidOperation`
- 否則把前 n 個放進暫存 list 後用 `printKeywordList` 印出

```java
public void outputFirstN(int n){
    if (n < 0 || n > lst.size()) {
        System.out.println("InvalidOperation");
        return;
    }
    LinkedList<Keyword> res = new LinkedList<>();
    for (int i = 0; i < n; i++) res.add(lst.get(i));
    printKeywordList(res);
}
```

### 3-6 `outputScore()` —— 輸出整個清單的分數

分數 = Σ(count × weight)，輸出到小數點後兩位（範本已幫你寫好 `String.format("%.2f", score)`）。

```java
public void outputScore(){
    double score = 0;
    for (Keyword k : lst) {
        score += k.count * k.weight;
    }
    System.out.println(String.format("%.2f", score));
}
```

---

## 4. Step 3：刪除類方法

### 4-1 `deleteIndex(int i)` —— 刪除第 i 個

- 越界（`i < 0` 或 `i >= size`）→ 印 `InvalidOperation`
- 否則 `lst.remove(i)`，**不輸出任何東西**

```java
public void deleteIndex(int i){
    if (i < 0 || i >= lst.size()) {
        System.out.println("InvalidOperation");
        return;
    }
    lst.remove(i);
}
```

### 4-2 `deleteCount` / `deleteHas` / `deleteName` —— 依條件刪除多個

這三個方法邏輯相同，只有判斷條件不同（`count == c`、`name.contains(s)`、`name.equals(s)`），**都不輸出任何東西**（就算沒有刪到東西也不用印 `NotFound`）。

> ⚠️ **常見錯誤**：在 for-each 迴圈中直接呼叫 `lst.remove(k)`，會拋出 `ConcurrentModificationException`。

正確做法有兩種，擇一即可：

**做法 A：使用 `Iterator`（課堂上 LinkedList 的經典寫法）**

```java
public void deleteHas(String pattern){
    Iterator<Keyword> it = lst.iterator();
    while (it.hasNext()) {
        if (it.next().name.contains(pattern)) {
            it.remove();    // 透過 iterator 刪除才安全
        }
    }
}
```

**做法 B：使用 `removeIf`（Java 8 以上）**

```java
public void deleteCount(int c){
    lst.removeIf(k -> k.count == c);
}

public void deleteName(String name){
    lst.removeIf(k -> k.name.equals(name));
}
```

### 4-3 `deleteFirstN(int n)` —— 刪除前 n 個

- `n > lst.size()` → 印 `InvalidOperation`
- 否則刪除 n 次第一個元素

```java
public void deleteFirstN(int n){
    if (n < 0 || n > lst.size()) {
        System.out.println("InvalidOperation");
        return;
    }
    for (int i = 0; i < n; i++) lst.removeFirst();
}
```

### 4-4 `deleteAll()` —— 刪除全部

```java
public void deleteAll(){
    lst.clear();
}
```

---

## 5. Step 4：編譯與執行

把 `input.txt` 放在**專案根目錄**（Eclipse 是專案資料夾，不是 `src`），否則程式會找不到檔案；或者執行時輸入完整路徑。

用命令列執行：

```bash
javac -encoding UTF-8 *.java
java Main
```

程式顯示 `Please input the test file name.` 後輸入 `input.txt`。

---

## 6. Step 5：用範例逐步對答案

下表是範例 `input.txt` 每一步執行後的結果，可以用來 debug：

| 指令 | 執行後清單 / 輸出 |
| --- | --- |
| 前 9 行 `add` | `MIS(2,1.2) UCSB(2,2.2) Food(3,0.1) Data(3,0.3) NCCU(3,0.8) Fang(3,1.5) Structure(4,2.1) Badminton(4,2.3) Yu(5,1.2)` |
| `outputScore` | 輸出 `38.50` |
| `deleteCount 3` | 剩下 `MIS UCSB Structure Badminton Yu` |
| `outputCount 2` | 輸出 `[MIS,2,1.2] [UCSB,2,2.2]` |
| `outputName Yu` | 輸出 `[Yu,5,1.2]` |
| `deleteName Yu` | 剩下 `MIS UCSB Structure Badminton` |
| `outputHas a` | 輸出 `[Badminton,4,2.3]`（只有它含小寫 a） |
| `deleteHas a` | 剩下 `MIS UCSB Structure` |
| `outputIndex 2` | 輸出 `[Structure,4,2.1]` |
| `deleteIndex 4` | 只有 3 個 → 輸出 `InvalidOperation` |
| `deleteFirstN 1` | 剩下 `UCSB Structure` |
| `outputFirstN 3` | 只有 2 個 → 輸出 `InvalidOperation` |
| `deleteAll` | 清單清空 |

完整的預期輸出：

```
Please input the test file name.
input.txt
38.50
[MIS,2,1.2] [UCSB,2,2.2]
[Yu,5,1.2]
[Badminton,4,2.3]
[Structure,4,2.1]
InvalidOperation
InvalidOperation
```

---

## 7. 常見錯誤 Checklist

- [ ] `add` 用了 `>=` 而不是 `>`，導致相同 count、weight 的元素順序顛倒
- [ ] 比較字串用了 `==` 而不是 `equals`
- [ ] 在 for-each 中直接 `lst.remove(...)` → `ConcurrentModificationException`
- [ ] 用 `for (int i = 0; i < lst.size(); i++)` 邊走邊刪，刪掉一個後 index 沒有調整，導致漏刪相鄰元素
- [ ] `outputIndex` / `deleteIndex` 邊界寫成 `i > size`（應該是 `i >= size`）
- [ ] `outputFirstN` / `deleteFirstN` 邊界寫成 `n >= size`（`n == size` 是合法的，應該是 `n > size` 才不合法）
- [ ] 「找不到」該印 `NotFound`、「不合法操作」該印 `InvalidOperation`，兩者搞混
- [ ] 刪除類的 `deleteCount` / `deleteHas` / `deleteName` 多印了東西（它們不輸出）
- [ ] `input.txt` 放錯位置，出現 `FileNotFoundException`

---

## 8. 繳交

- 上傳 Moodle
- **First Submission**：10/04 (Sun.) 12:00
- **Deadline**：10/12 (Mon.) 00:00
