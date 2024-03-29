import java.util.*;

class JaccardAnalysis extends Thread {
	FloatListPair pairs;
	float result;
	float thre0;
	float thre1;

	public JaccardAnalysis(FloatListPair pairs, float thre0, float thre1) {
		this.pairs = pairs;
		this.thre0 = thre0;
		this.thre1 = thre1;
	}

	public float getResult() {
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
			HashSet<Integer> union = new HashSet<>(remove0);

			union.addAll(remove1);
			if (union.size() == 0 || remove0.size() == 0 || remove1.size() == 0) {
				result = 0;
				return;
			}

			BitSet bitSet = new BitSet(Collections.max(remove0));
			BitSet bitSet1 = new BitSet(Collections.max(remove1));
			for (int i = 0; i < remove0.size(); i++) {
					bitSet.set(remove0.get(i));
			}
			for (int i = 0; i < remove1.size(); i++) {
					bitSet1.set(remove1.get(i));
			}
			bitSet.and(bitSet1);
			result = 1.0f * bitSet.cardinality() / union.size();
	
		} catch (Exception e) {
			System.out.println("Exception caught in JaccardAnalysis().");
            //e.printStackTrace(); 
		}
	}

	public ArrayList<Integer> getRemovedList(float[] eventValueList, float threshold) {
		ArrayList<Integer> removedList = new ArrayList<>();
		for (int i = 0; i < eventValueList.length; i++) {
			if (eventValueList[i] > threshold) {
				removedList.add(i);	
			}
		}
		return removedList;
	}
}
