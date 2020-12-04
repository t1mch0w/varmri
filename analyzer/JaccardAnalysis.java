import org.apache.commons.math3.stat.regression.SimpleRegression;
import java.util.*;

class JaccardAnalysis extends Thread {
	DoubleListPair pairs;
	double pTarget;
	double result;

	public JaccardAnalysis(DoubleListPair pairs, double pTarget) {
		this.pairs = pairs;
		this.pTarget = pTarget;
	}

	public double getResult() {
		return result;
	}

	public void run() {
		try {
			pairs.sort();
			CurveFit cf0 = new CurveFit(pairs, pTarget, 0);
			CurveFit cf1 = new CurveFit(pairs, pTarget, 1);
			cf0.start();
			cf1.start();
			cf0.join();
			cf1.join();
			double thre0 = cf0.getResult();
			double thre1 = cf1.getResult();
			ArrayList<Integer> remove0 = getRemovedList(pairs.getFirst(), thre0);
			ArrayList<Integer> remove1 = getRemovedList(pairs.getSecond(), thre1);
	
			/* debug
			//System.out.println("=========");
			//Collections.sort(remove0);
			//for (int a : remove0) {
			//	System.out.printf("%d ",a );				
			//}
			//System.out.println();
			//System.out.println("=========");
			//Collections.sort(remove1);
			//for (int a : remove1) {
			//	System.out.printf("%d ",a);
			//}
			//System.out.println();
			*/

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

	public ArrayList<Integer> getRemovedList(ArrayList<Double> eventValueList, double threshold) {
		ArrayList<Integer> removedList = new ArrayList<>();
		for (int i = 0; i < eventValueList.size(); i++) {
			if (eventValueList.get(i) > threshold) {
				removedList.add(i);	
			}
		}
		return removedList;
	}
}