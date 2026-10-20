public class GeomProgression<T extends Number> extends Progression<T>{

	private T base;
	public GeomProgression(T first, T base) {
		super(first);
		this.base = base;
	}

	private T multiply(T a, T b) {
		if(a instanceof Integer){
			// 先用 long 算，才能檢查結果有沒有超出 int 範圍
			long product = (long) a.intValue() * b.intValue();
			if(product > Integer.MAX_VALUE || product < Integer.MIN_VALUE){
				throw new ArithmeticException("Integer overflow: " + a + " * " + b
						+ " exceeds the int range (max " + Integer.MAX_VALUE + ")");
			}
		    Integer erasedValue = (int) product;
		    return (T)erasedValue;
		}
		// 3. Need to fill this method with Double.
		else if(a instanceof Double){
			Double erasedValue = a.doubleValue() * b.doubleValue();
			// double 溢位不會丟例外，而是變成 Infinity，要自己檢查
			if(erasedValue.isInfinite()){
				throw new ArithmeticException("Double overflow: " + a + " * " + b
						+ " exceeds the double range (max " + Double.MAX_VALUE + ")");
			}
			return (T)erasedValue;
		}

		else{
			throw new IllegalArgumentException("Unsupported type: " + a.getClass().getSimpleName());
		}
	}

	public T nextValue() {
		curr = multiply(curr, base);
		return curr;
	}
}
