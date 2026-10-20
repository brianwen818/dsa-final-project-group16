# HW2 教學：給會 Python（和一點 C++）的人的 Java 入門

這份教學會一步一步帶你完成 HW2。每一步都會先講要用到的 Java 觀念，並跟 Python / C++ 對照，然後告訴你要改哪裡。

每一步的參考答案都收在 **▶ 參考答案** 裡。建議先自己寫，寫不出來再打開看。

---

## 第 0 步：先搞懂 Java 的基本規則

### 0.1 怎麼編譯與執行

Java 跟 C++ 一樣要先編譯：

```bash
cd HW2_110208064        # 進到放 .java 檔的資料夾
javac *.java            # 編譯，會產生 .class 檔（像 C++ 的 g++）
java Main               # 執行，Main 是有 main() 的那個類別名稱（不用加 .java 或 .class）
```

> 如果 `javac` 印出 `Note: GeomProgression.java uses unchecked or unsafe operations.`，這只是**警告**，不是錯誤，可以忽略（第 3 步會解釋原因）。

### 0.2 語法速查表

| 概念 | Python | C++ | Java |
|---|---|---|---|
| 宣告變數 | `x = 3` | `int x = 3;` | `int x = 3;`（跟 C++ 一樣要寫型別） |
| 小數 | `x = 1.5` | `double x = 1.5;` | `double x = 1.5;` |
| 字串 | `s = "hi"` | `std::string s = "hi";` | `String s = "hi";`（S 大寫） |
| 印出 | `print(x)` | `cout << x << endl;` | `System.out.println(x);` |
| 字串串接 | `s + str(x)` | 要轉型 | `s + x`（數字會**自動**轉成字串） |
| for 迴圈 | `for i in range(1, n):` | `for (int i = 1; i < n; i++)` | 跟 C++ 一樣 |
| if | `if a and b:` | `if (a && b)` | 跟 C++ 一樣 |
| 區塊 | 縮排 | `{ }` | `{ }`，每行結尾要加 `;` |
| 整數除法 | `7 // 2` → 3 | `7 / 2` → 3 | `7 / 2` → 3（兩邊都是 int 就是整數除法） |
| 丟出例外 | `raise Exception("msg")` | `throw std::runtime_error("msg");` | `throw new Exception("msg");` |
| 接住例外 | `try: ... except Exception as e:` | `try {...} catch (std::exception& e) {...}` | `try {...} catch (Exception e) {...}` |
| 取得例外訊息 | `str(e)` | `e.what()` | `e.getMessage()` |

### 0.3 類別與繼承

```python
# Python
class Progression:
    def __init__(self, first):
        self.first = first

class GeomProgression(Progression):
    def __init__(self, first, base):
        super().__init__(first)
        self.base = base
```

```java
// Java
public class Progression {
    protected int first;               // 欄位要先宣告型別
    public Progression(int first) {    // 建構子：名字跟類別一樣，沒有回傳型別
        this.first = first;            // this 就是 Python 的 self
    }
}

public class GeomProgression extends Progression {   // extends = 繼承
    private int base;
    public GeomProgression(int first, int base) {
        super(first);                  // 呼叫父類別的建構子
        this.base = base;
    }
}
```

- `public`、`private`、`protected` 跟 C++ 的用法一樣。
- `abstract` 方法就像 C++ 的純虛擬函式 `virtual T nextValue() = 0;`：父類別只宣告不實作，由子類別實作。

### 0.4 泛型（Generics）：這次作業的重點

泛型就是 C++ 的 **template**：

```cpp
// C++
template <typename T>
class Progression { T first; };
Progression<int> p;
```

```java
// Java
public class Progression<T> { protected T first; }
Progression<Integer> p;
```

有兩個跟 C++ 不一樣的地方：

1. **`< >` 裡不能放 `int`、`double`，要放 `Integer`、`Double`。**
   Java 的泛型只能裝「物件」。`int` 對應的物件版本是 `Integer`，`double` 對應的是 `Double`。兩者之間 Java 會自動轉換（這叫 autoboxing），所以 `Integer x = 3;` 可以直接寫。
2. **`<T extends Number>`** 的意思是「T 必須是數字類型（Integer、Double……）」，概念類似 C++20 的 concept。有了這個限制，你才能對 T 呼叫 `.intValue()`、`.doubleValue()` 這些數字方法。

### 0.5 `instanceof`：檢查型別

```java
if (a instanceof Integer) { ... }   // 相當於 Python 的 isinstance(a, int)
```

---

## 第 1 步：看懂現有的程式

`HW2_110208064/` 裡有三個檔案：

```
Progression<T>              ← 抽象父類別：存 first / curr，負責印出數列
   └── GeomProgression<T>   ← 子類別：nextValue() = curr × base（等比數列）
Main                        ← 讀輸入、建立物件、呼叫印出
```

程式的流程：

1. `Main` 讀進 `type`：0 代表人口，1 代表資本。
2. 讀初始值和年數。
3. 建立 `GeomProgression`。人口的公比是 `2`（每 30 年翻倍），資本的公比是 `1.02`（年利率 2%）。
4. 呼叫 `printProgression(n)` 印出 n 項。

輸入格式是一行三個數字：`類型 初始值 年數`，例如：

```
0 3 120      → 人口：初始 3 人，120 年
1 100 2      → 資本：初始 100 元，2 年
```

你要完成程式碼裡標 `// 1.`、`// 2.`、`// 3.` 的地方，另外還要補一個沒標號的地方（第 5 步）。

---

## 第 2 步：GeomProgression.java，補上 Double 乘法（TODO 3）

先做最簡單的這個。打開 [GeomProgression.java](../HW2_110208064/GeomProgression.java)：

```java
private T multiply(T a, T b) {
    if(a instanceof Integer){
        Integer erasedValue = a.intValue() * b.intValue();
        return (T)erasedValue;
    }
    // 3. Need to fill this method with Double.
//		else if

    else{
        return null;
    }
}
```

**為什麼不直接寫 `a * b`？** 因為 `T` 是泛型，Java 不知道 T 能不能用 `*` 相乘。（C++ 的 template 可以直接寫，Java 不行。）所以要先用 `instanceof` 判斷型別，再用 `.intValue()` 或 `.doubleValue()` 把它轉成真正的數字來乘，最後用 `(T)` 轉型回 T。

`(T)` 這個轉型就是編譯時出現 unchecked 警告的原因，可以忽略。

**你的任務：** 照著 Integer 分支的寫法，把 `//		else if` 那行換成 Double 的版本。

提示：`intValue()` 改成 `doubleValue()`，`Integer` 改成 `Double`。

<details>
<summary>▶ 參考答案</summary>

```java
else if(a instanceof Double){
    Double erasedValue = a.doubleValue() * b.doubleValue();
    return (T)erasedValue;
}
```

</details>

---

## 第 3 步：Progression.java，實作 printProgression（TODO 2）

打開 [Progression.java](../HW2_110208064/Progression.java)。要印出前 n 項（包含第一項），格式是用 `, ` 隔開：

```
3, 6, 12, 24, 48
```

### 3.1 先寫出基本版

可以用的工具：

- `firstValue()`：把 `curr` 重設回第一項，並回傳第一項。
- `nextValue()`：算出下一項，並回傳它。

Python 版的想法是這樣：

```python
result = str(self.first_value())
for i in range(1, n):
    result += ", " + str(self.next_value())
print(result)
```

**你的任務：** 把它翻成 Java，寫在 `printProgression` 裡的兩個空行。

提示：Java 的字串用 `+` 串接數字時會自動轉成字串，所以 `", " + nextValue()` 可以直接寫。

<details>
<summary>▶ 參考答案（基本版）</summary>

```java
public void printProgression(int n){
    String result = "";
    result += firstValue();
    for (int i = 1; i < n; i++) {
        result += ", " + nextValue();
    }
    System.out.println(result);
}
```

</details>

### 3.2 發現問題：小數印出來很醜

基本版印資本時會得到：

```
100.0, 102.0, 104.04000000000001
```

但題目要求的是 `100, 102, 104.04`。問題有兩個：

- Double 的整數值會帶 `.0`。
- 浮點數有誤差（跟 Python 的 `0.1 + 0.2 = 0.30000000000000004` 是同一件事）。

解法是用 Java 內建的 `DecimalFormat` 格式化數字：

```java
DecimalFormat df = new DecimalFormat("#.##");
df.format(104.04000000000001);   // → "104.04"
df.format(100.0);                // → "100"
```

`"#.##"` 的意思是「最多兩位小數，多餘的 0 不印」，效果類似 Python 的 `f"{x:.2f}".rstrip("0").rstrip(".")`。

### 3.3 加上格式化

1. 在檔案**最上面**（`public abstract class` 那行之前）加上 import，就像 Python 的 import：
   ```java
   import java.text.DecimalFormat;
   ```
2. 在類別裡新增一個小工具方法 `format`：如果值是 Double 就用 DecimalFormat 格式化，否則直接轉成字串。
3. 把 `printProgression` 裡的 `firstValue()`、`nextValue()` 都用 `format(...)` 包起來。

<details>
<summary>▶ 參考答案（完整的 Progression.java）</summary>

```java
import java.text.DecimalFormat;

public abstract class Progression<T> {
	protected T first;
	protected T curr;

	public Progression(T first) {
		this.first = first;
		this.curr = first;
	}

	//reset and return first value
	public T firstValue() {
		curr = first;
		return first;
	}

	public abstract T nextValue();

	// 把數值轉成字串；Double 最多留兩位小數
	private String format(T value) {
		if (value instanceof Double) {
			DecimalFormat df = new DecimalFormat("#.##");
			return df.format(value);
		}
		return value.toString();
	}

    public void printProgression(int n){
    	// 2. Print the first n values (include first value)
		String result = "";
		result += format(firstValue());
		for (int i = 1; i < n; i++) {
			result += ", " + format(nextValue());
		}
		System.out.println(result);
    }

}
```

</details>

---

## 第 4 步：Main.java，讀使用者輸入（TODO 1）

打開 [Main.java](../HW2_110208064/Main.java)。`Scanner` 是 Java 讀輸入的工具，類似 C++ 的 `cin`：

| Python | C++ | Java |
|---|---|---|
| `x = int(input())` | `int x; cin >> x;` | `int x = sc.nextInt();` |
| `x = float(input())` | `double x; cin >> x;` | `double x = sc.nextDouble();` |

`Scanner` 跟 `cin` 一樣以空白分隔，所以輸入 `0 3 120` 時可以連續呼叫三次 `nextInt()`。第一個數字 `type` 程式已經幫你讀好了。

**你的任務：**

- 在 `case 0` 的 `// 1. get user input` 下面讀入 `people`（int）和 `years`（int）。
- 在 `case 1` 的 `// 1. get user input` 下面讀入 `initial`（**Double**）和 `years`（int）。

變數名稱 `people`、`initial` 不能改，因為下面建立物件時已經用了這兩個名字。

⚠️ **陷阱：** 在 Java（和 C++）裡，同一個 `switch` 裡的所有 `case` 共用同一個作用域。如果 case 0 和 case 1 都宣告 `int years`，會出現 `variable years is already defined` 的錯誤。

解法是用大括號把每個 case 的內容包起來，讓它們各自有自己的作用域：

```java
case 0: { //population
    ...
    break;
}
case 1: { //capital
    ...
    break;
}
```

<details>
<summary>▶ 參考答案（case 0 與 case 1）</summary>

```java
case 0: { //population
    // 1. get user input
    int people = sc.nextInt();
    int years = sc.nextInt();

    int prate = 2 ;
    GeomProgression<Integer> Population = new GeomProgression<Integer>(people, prate);
    Population.printProgression();   // ← 這行第 5 步還要改
    break;
}
case 1: { //capital
    // 1. get user input
    Double initial = sc.nextDouble();
    int years = sc.nextInt();

    Double crate = 1.02;
    GeomProgression<Double> Capital = new GeomProgression<Double>(initial, crate);
    Capital.printProgression();      // ← 這行第 5 步還要改
    break;
}
```

</details>

---

## 第 5 步：算出要印幾項（題目沒標，但一定要改）

`printProgression(int n)` 需要一個參數 n，但 Main 裡寫的是 `printProgression()`，沒有傳參數，這樣**編譯會失敗**。你要自己算出 n。

想一想：

- **人口**：每 30 年翻倍一次。120 年會翻倍 4 次，加上第一項，一共印 **5** 項（3, 6, 12, 24, 48）。
  → n = ?（提示：用整數除法。如果是 100 年，應該只翻倍 3 次。）
- **資本**：每年算一次。2 年會算 2 次，加上第一項，一共印 **3** 項（100, 102, 104.04）。
  → n = ?

<details>
<summary>▶ 參考答案</summary>

```java
Population.printProgression(years / 30 + 1);   // int / int 是整數除法，100/30 = 3
...
Capital.printProgression(years + 1);
```

</details>

現在可以先編譯並執行看看：

```bash
javac *.java
java Main
```

輸入 `0 3 120`，應該會印出 `3, 6, 12, 24, 48`。
輸入 `1 100 2`，應該會印出 `100, 102, 104.04`。

---

## 第 6 步：讓例外處理更完整

作業要求要用 try-catch 處理例外。Main 已經有一個 `try { ... } catch (Exception e) { ... }`，它的作用跟 Python 的 `try: ... except Exception as e:` 一樣。

目前的程式有兩個可以改進的地方：

### 6.1 輸入不是數字時

試試看輸入 `abc`，你會看到 `Error: null`。這是因為 `nextInt()` 丟出的 `InputMismatchException` 沒有帶訊息。

跟 Python 可以寫多個 `except` 一樣，Java 也可以寫多個 `catch`，**比較具體的例外要放在前面**：

```python
# Python 的寫法
try:
    ...
except ValueError:
    print("Error: please enter numbers only")
except Exception as e:
    print("Error:", e)
```

**你的任務：** 在 Java 裡照做。

1. 檔案最上面加上 `import java.util.InputMismatchException;`
2. 在原本的 `catch (Exception e)` **前面**加一個 `catch (InputMismatchException e)`。

### 6.2 輸入負數時

人口或年數是負的不合理。可以在讀完輸入後自己丟出例外：

```java
if (people < 0 || years < 0) {
    throw new Exception("people and years must be non-negative");
}
```

丟出的例外會被下面的 `catch (Exception e)` 接住，然後印出 `Error: people and years must be non-negative`。case 1 也照著加一個。

<details>
<summary>▶ 參考答案（完整的 Main.java）</summary>

```java
import java.util.InputMismatchException;
import java.util.Scanner;

public class Main {

	public static void main(String[] args) {
		System.out.println("Please type (1)type and (2)number of initial people or capital and (3)years: ");
		Scanner sc = new Scanner(System.in);
			try {
				while(sc.hasNext()) {
					int type = sc.nextInt();

					switch(type){
						case 0: { //population
							// 1. get user input
							int people = sc.nextInt();
							int years = sc.nextInt();
							if (people < 0 || years < 0) {
								throw new Exception("people and years must be non-negative");
							}
							int prate = 2 ;
							GeomProgression<Integer> Population = new GeomProgression<Integer>(people, prate);
							Population.printProgression(years / 30 + 1);
							break;
						}
						case 1: { //capital
							// 1. get user input
							Double initial = sc.nextDouble();
							int years = sc.nextInt();
							if (initial < 0 || years < 0) {
								throw new Exception("capital and years must be non-negative");
							}
							Double crate = 1.02;
							GeomProgression<Double> Capital = new GeomProgression<Double>(initial, crate);
							Capital.printProgression(years + 1);
							break;
						}
						default:
							throw new Exception("InvalidType please enter type for 0 or 1");
					}
				}

			}catch(InputMismatchException e) {
				System.out.println("Error: please enter numbers only");
			}catch(Exception e) {
				System.out.println("Error: "+e.getMessage());
			}

		sc.close();
	}
}
```

</details>

---

## 第 7 步：最終測試

```bash
javac *.java
java Main
```

逐一輸入下面每一行，確認輸出跟右邊一樣。注意：一旦發生錯誤，程式就會結束，所以測試錯誤情況時要重新執行 `java Main`。

| 輸入 | 預期輸出 | 在測什麼 |
|---|---|---|
| `0 3 120` | `3, 6, 12, 24, 48` | 題目範例 1 |
| `1 100 2` | `100, 102, 104.04` | 題目範例 2 |
| `0 3 100` | `3, 6, 12, 24` | 年數不是 30 的倍數 |
| `1 100 10` | `100, 102, 104.04, 106.12, 108.24, 110.41, 112.62, 114.87, 117.17, 119.51, 121.9` | 較多年 |
| `5 1 1` | `Error: InvalidType please enter type for 0 or 1` | 錯誤的類型 |
| `abc` | `Error: please enter numbers only` | 非數字輸入 |
| `0 -1 30` | `Error: people and years must be non-negative` | 負數 |

在 Windows 上想結束輸入，請按 `Ctrl + Z` 再按 Enter（或直接 `Ctrl + C`）。

---

## 常見編譯錯誤對照

| 錯誤訊息 | 原因 |
|---|---|
| `cannot find symbol ... variable people` | 還沒宣告 `people`（第 4 步） |
| `variable years is already defined` | case 沒加 `{ }`（第 4 步的陷阱） |
| `method printProgression in class Progression<T> cannot be applied to given types` | `printProgression()` 沒傳 n（第 5 步） |
| `cannot find symbol ... class DecimalFormat` | 忘了 `import java.text.DecimalFormat;` |
| `';' expected` | 某行少了分號 |
| `incompatible types: double cannot be converted to Integer` | 型別對不上，檢查 `Integer`、`Double` 有沒有用對 |

---

## 回顧：這次作業練到的觀念

- **繼承**：`GeomProgression extends Progression`。子類別只實作 `nextValue()`，印數列的邏輯全部重用父類別的 `printProgression()`。
- **泛型**：同一個 `GeomProgression<T>`，可以寫成 `<Integer>` 處理人口，也可以寫成 `<Double>` 處理資本，不必寫兩個類別。
- **例外處理**：用 `throw` 丟出錯誤，用 `try-catch` 接住；多個 `catch` 時，具體的例外要放在前面。
