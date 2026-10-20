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
		// 用 StringBuilder 而不是 String +=，項數很多時才不會變得很慢
		StringBuilder result = new StringBuilder();
		result.append(format(firstValue()));
		for(int i = 1; i < n; i++){
			result.append(" ").append(format(nextValue()));
		}

		System.out.println(result);
    }

}