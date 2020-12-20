import java.util.*;

class JaccardAnalysis extends Thread {
	DoubleListPair pairs;
	double result;
	double thre0;
	double thre1;

	public JaccardAnalysis(DoubleListPair pairs, double thre0, double thre1) {
		this.pairs = pairs;
		this.thre0 = thre0;
		this.thre1 = thre1;
	}

	public double getResult() {
		return result;
	}

	public void run() {
		try {
			ArrayList<Integer> remove0 = getRemovedList(pairs.getFirst(), thre0);
			ArrayList<Integer> remove1 = getRemovedList(pairs.getSecond(), thre1);
	
			/* debug
			System.out.println("=========");
			Collections.sort(remove0);
			for (int a : remove0) {
				System.out.printf("%d ",a );				
			}
			System.out.println();
			System.out.println("=========");
			Collections.sort(remove1);
			for (int a : remove1) {
				System.out.printf("%d ",a);
			}
			System.out.println();
			*/

			//Swap to make remove1 smaller
			if (remove0.size() < remove1.size()) {
				ArrayList<Integer> tmp = remove1;
				remove1 = remove0;
				remove0 = tmp;
			}
			HashSet<Integer> intersection = new HashSet<>(remove0);
			HashSet<Integer> union = new HashSet<>(remove0);
			union.addAll(remove1);
			intersection.retainAll(remove1);
	
			if (union.size() == 0) {
				result = 0;
			}
			else {
				result = 1.0 * intersection.size() / union.size();
			}
	
		} catch (Exception e) {
			System.out.println("Exception caught in JaccardAnalysis().");
		}
	}

	public ArrayList<Integer> getRemovedList(double[] eventValueList, double threshold) {
		ArrayList<Integer> removedList = new ArrayList<>();
		for (int i = 0; i < eventValueList.length; i++) {
			if (eventValueList[i] > threshold) {
				removedList.add(i);	
			}
		}
		return removedList;
	}
}
