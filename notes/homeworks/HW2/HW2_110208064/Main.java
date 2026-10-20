import java.util.InputMismatchException;
import java.util.Scanner;

public class Main {

	// 讀一個整數：不是整數、或超出 int 範圍都丟出例外
	private static int readInt(Scanner sc, String name) {
		String token = sc.next();
		if(!token.matches("[+-]?\\d+")){
			throw new InputMismatchException(name + " must be an integer, but got \"" + token + "\"");
		}
		try {
			return Integer.parseInt(token);
		}catch(NumberFormatException e) {
			throw new ArithmeticException(name + " is too large: " + token
					+ " (int range is " + Integer.MIN_VALUE + " ~ " + Integer.MAX_VALUE + ")");
		}
	}

	// 讀一個小數：不是數字、或超出 double 範圍都丟出例外
	private static double readDouble(Scanner sc, String name) {
		String token = sc.next();
		double value;
		try {
			value = Double.parseDouble(token);
		}catch(NumberFormatException e) {
			throw new InputMismatchException(name + " must be a number, but got \"" + token + "\"");
		}
		if(Double.isNaN(value)){
			throw new InputMismatchException(name + " must be a number, but got \"" + token + "\"");
		}
		if(Double.isInfinite(value)){
			throw new ArithmeticException(name + " is too large: " + token
					+ " (max " + Double.MAX_VALUE + ")");
		}
		return value;
	}

	public static void main(String[] args) {
		System.out.println("Please type (1)type and (2)number of initial people or capital and (3)years: ");
		Scanner sc = new Scanner(System.in);
			// try 放在迴圈裡面：一筆輸入出錯只印錯誤訊息，還能繼續輸入下一筆
			while(sc.hasNext()) {
				try {
					int type = readInt(sc, "type");

					switch(type){
						case 0: {//population
							// 1. get user input
							int people = readInt(sc, "people");
							int years = readInt(sc, "years");
							if(people < 0 || years < 0){
								throw new IllegalArgumentException("people and years must be non-negative");
							}

							int prate = 2 ;
							GeomProgression<Integer> Population = new GeomProgression<Integer>(people, prate);
							Population.printProgression(years / 30 + 1);
							break;
						}
						case 1: {//capital
							// 1. get user input
							Double initial = readDouble(sc, "capital");
							int years = readInt(sc, "years");
							if(initial < 0 || years < 0){
								throw new IllegalArgumentException("capital and years must be non-negative");
							}

							Double crate = 1.02;
							GeomProgression<Double> Capital = new GeomProgression<Double>(initial, crate);
							// years 是 int 最大值時 years + 1 會溢位
							if(years == Integer.MAX_VALUE){
								throw new ArithmeticException("years + 1 exceeds the int range (max " + Integer.MAX_VALUE + ")");
							}
							Capital.printProgression(years + 1);
							break;
						}
						default:
							throw new IllegalArgumentException("InvalidType please enter type for 0 or 1");
					}
				}catch(InputMismatchException e) {
					System.out.println("Error: invalid input, " + e.getMessage());
					skipLine(sc);
				}catch(ArithmeticException e) {
					System.out.println("Error: overflow, " + e.getMessage());
					skipLine(sc);
				}catch(Exception e) {
					System.out.println("Error: " + e.getMessage());
					skipLine(sc);
				}
			}

		sc.close();
	}

	// 出錯時丟掉這一行剩下的輸入，避免殘留的數字被當成下一筆的 type
	private static void skipLine(Scanner sc) {
		if(sc.hasNextLine()){
			sc.nextLine();
		}
	}
}
