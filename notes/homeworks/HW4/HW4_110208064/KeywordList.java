
import java.util.*;
public class KeywordList {

	private LinkedList<Keyword> lst;

	public KeywordList(){
		this.lst = new LinkedList<Keyword>();

	}

	public void add(Keyword keyword){
		// 1. add keyword to proper index base on its count . DECENDING SORT BY COUNT AND WEIGHT
		// Smaller count placed in the front. If equal, smaller weight is placed in the front.
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

	public void outputIndex(int i){
		// 2. output keyword at index i
		if (i < 0 || i >= lst.size()) {
			System.out.println("InvalidOperation");
			return;
		}
		System.out.println(lst.get(i));
	}

	public void outputCount(int c){
		// 3. output all keywords whose keyword is equal to c
		LinkedList<Keyword> res = new LinkedList<>();
		for (Keyword k : lst) {
			if (k.count == c) res.add(k);
		}
		if (res.isEmpty()) System.out.println("NotFound");
		else printKeywordList(res);
	}

	public void outputHas(String pattern){
		// 4. output all keywords whose name contains input pattern
		LinkedList<Keyword> res = new LinkedList<>();
		for (Keyword k : lst) {
			if (k.name.contains(pattern)) res.add(k);
		}
		if (res.isEmpty()) System.out.println("NotFound");
		else printKeywordList(res);
	}

	public void outputName(String pattern){
		// 5. output all keywords whose name is equal to input name
		LinkedList<Keyword> res = new LinkedList<>();
		for (Keyword k : lst) {
			if (k.name.equals(pattern)) res.add(k);
		}
		if (res.isEmpty()) System.out.println("NotFound");
		else printKeywordList(res);
	}

	public void outputFirstN(int n){
		// 6. output first n keywords
		if (n < 0 || n > lst.size()) {
			System.out.println("InvalidOperation");
			return;
		}
		LinkedList<Keyword> res = new LinkedList<>();
		for (int i = 0; i < n; i++) res.add(lst.get(i));
		printKeywordList(res);
	}

	public void outputScore(){
		double score = 0;
		// 7. output the total score of all keywords
		for (Keyword k : lst) {
			score += k.count * k.weight;
		}
		System.out.println(String.format("%.2f", score));
	}

	public void deleteIndex(int i){
		// 8. remove keyword at index i
		if (i < 0 || i >= lst.size()) {
			System.out.println("InvalidOperation");
			return;
		}
		lst.remove(i);
	}

	public void deleteCount(int c){
		// 9. remove nodes that the count is equal to c
		Iterator<Keyword> it = lst.iterator();
		while (it.hasNext()) {
			if (it.next().count == c) it.remove();
		}
	}

	public void deleteHas(String pattern){
		// 10. remove nodes that the name contains input name
		Iterator<Keyword> it = lst.iterator();
		while (it.hasNext()) {
			if (it.next().name.contains(pattern)) it.remove();
		}
	}

	public void deleteName(String name){
		// 11. remove nodes that the name is equal to input name
		Iterator<Keyword> it = lst.iterator();
		while (it.hasNext()) {
			if (it.next().name.equals(name)) it.remove();
		}
	}

	public void deleteFirstN(int n){
		// 12. remove first n nodes
		if (n < 0 || n > lst.size()) {
			System.out.println("InvalidOperation");
			return;
		}
		for (int i = 0; i < n; i++) lst.removeFirst();
	}

	public void deleteAll(){
		// 13. remove all nodes
		lst.clear();
	}

	private void printKeywordList(LinkedList<Keyword> kLst){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < kLst.size(); i++){
			Keyword k = kLst.get(i);
			if(i > 0) sb.append(" ");
			sb.append(k.toString());
		}
		System.out.println(sb.toString());
	}
}
