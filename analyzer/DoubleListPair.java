import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

class DoubleListPair {
	int lengthByDefault = 10000000;
	int currLength;
	public double[] first;
	public double[] second;

	public int firstIndex;
	public int secondIndex;

	public double[] firstSorted;
	public double[] secondSorted;
	
	public DoubleListPair() {
		first = new double[lengthByDefault];
		second = new double[lengthByDefault];
		firstIndex = 0;
		secondIndex = 0;
		currLength = lengthByDefault;
	}

	public void addData(double firstElement, double secondElement) {
		if (firstIndex >= currLength) {
			double[] tmpFirst = first;
			double[] tmpSecond = second;
			first = new double[2 * currLength];
			second = new double[2 * currLength];
			System.arraycopy(tmpFirst, 0, first, 0, currLength); 
			System.arraycopy(tmpSecond, 0, second, 0, currLength); 
			currLength *= 2;
		}
		first[firstIndex++] = firstElement;
		second[secondIndex++] = secondElement;
	}

	public void sort() {
		firstSorted = first.clone();
		secondSorted = second.clone();
		Arrays.sort(firstSorted);
		Arrays.sort(secondSorted);
	}

	public double[][] getDoubleArray(int column, int spos, int epos) {
		double[] targetList = column == 0 ? firstSorted : secondSorted;
		int total = epos - spos;
		double[][] doubleArray = new double[total][2];
		for (int i = 0; i < total; i++) {
			doubleArray[i][0] = targetList[i + spos];
			doubleArray[i][1] = i + spos;
		}
		return doubleArray;
	}
	
	public void printOut() {
		for (int i = 0; i < first.length; i++) {
			System.out.printf("%f %f\n", first[i], second[i]);
		}
	}

	public double[] getFirst() {
		return first;
	}

	public double[] getSecond() {
		return second;
	}

	public double[] getFirstSorted() {
		return firstSorted;
	}

	public double[] getSecondSorted() {
		return secondSorted;
	}

	public int size() {
		return firstIndex;
	}
}
