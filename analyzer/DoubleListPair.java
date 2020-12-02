import java.util.ArrayList;
import java.util.Collections;

class DoubleListPair {
	public ArrayList<Double> first;
	public ArrayList<Double> second;
	//public ArrayList<Double>[] sortedFields;

	public ArrayList<Double> firstSorted;
	public ArrayList<Double> secondSorted;
	
	public DoubleListPair() {
		first = new ArrayList<Double>();
		second = new ArrayList<Double>();
		//sortedFields = new ArrayList<Double>[2];
	}

	public void addData(double firstElement, double secondElement) {
		first.add(firstElement);
		second.add(secondElement);
	}

	public void sort() {
		firstSorted = new ArrayList<>(first);
		secondSorted = new ArrayList<>(second);
		Collections.sort(firstSorted);
		Collections.sort(secondSorted);
	}

	public double[][] getDoubleArray(int column, int spos, int epos) {
		ArrayList<Double> targetList = column == 0 ? firstSorted : secondSorted;
		int total = epos - spos;
		double[][] doubleArray = new double[total][2];
		for (int i = 0; i < total; i++) {
			doubleArray[i][0] = targetList.get(i + spos);
			doubleArray[i][1] = i + spos;
		}
		return doubleArray;
	}
	
	public void printOut() {
		for (int i = 0; i < first.size(); i++) {
			System.out.printf("%f %f\n", first.get(i), second.get(i));
		}
	}

	//public ArrayList<Double> getSortedArray(int column) {
	//}

	public ArrayList<Double> getFirst() {
		return first;
	}

	public ArrayList<Double> getSecond() {
		return second;
	}

	public ArrayList<Double> getFirstSorted() {
		return firstSorted;
	}

	public int size() {
		return first.size();
	}
}
