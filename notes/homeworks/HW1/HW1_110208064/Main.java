
import java.io.*;

public class Main {
	public static void main(String args[]) throws IOException {
		BufferedReader buf = new BufferedReader(new InputStreamReader(System.in)); 
		System.out.print("Please enter your weight(kg) and height(cm): "); 
		String text = buf.readLine(); 
		String[] data = text.split(" ");
		double res = getBMI(data);
		String dia = getDiagnosis(res);
		System.out.println(dia + " BMI: " + res);               
	}
	
	public static double getBMI(String[] data) {
		// 1. calculate the bmi 
		double weight = Double.parseDouble(data[0]); // 取出第 0 個元素轉為 double
		double heightCm = Double.parseDouble(data[1]); // 取出第 1 個元素轉為 double
		double heightM = heightCm / 100.0; // 公分轉公尺
		double bmi = weight / (heightM * heightM); // 計算 BMI
		// result bmi = weight(kg) / (height(m) * height(m))
		return bmi;
	}
	
	public static String getDiagnosis(double bmi) {
		if (bmi >= 30) {
			return "You are not in shape. Actually, you are not even close.";
		} else if (bmi >= 26) { // 因為進到這裡代表 bmi 已經 < 30，所以直接寫 bmi >= 26 即可
			return "To be honest, you are not in shape.";
		} else if (bmi >= 20) {
			return "You are in shape.";
		} else {
			return "You are under shape.";
		}
	}
}
