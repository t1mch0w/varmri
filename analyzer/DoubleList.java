import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

class DoubleList {
	//int lengthByDefault = 10000000;
	int lengthByDefault = 1000;
	int currLength;
	public double[] first;

	public int firstIndex;

	public double[] firstSorted;
	
	public DoubleList() {
		first = new double[lengthByDefault];
		firstIndex = 0;
		currLength = lengthByDefault;
	}

	public void add(double firstElement) {
		if (firstIndex >= currLength) {
			double[] tmpFirst = first;
			first = new double[2 * currLength];
			System.arraycopy(tmpFirst, 0, first, 0, currLength); 
			currLength *= 2;
		}
		first[firstIndex++] = firstElement;
	}

	public void reverseArray(double[] arr) {
		int i = 0;
		int j = arr.length - 1;
		double tmp = 0;

		while (i < j) {
			tmp = arr[i];
			arr[i] = arr[j];
			arr[j] = tmp;
			i++;
			j--;
		}	
	}

	public double[] sortArray(double[] array) {
		double[] res = new double[firstIndex];
		System.arraycopy(array, 0, res, 0, firstIndex);
		Arrays.sort(res);
		return res;
	}

	public void sort() {
		firstSorted = sortArray(first);
	}

	public double getResult(double percent) {
		this.sort();
		int pos = (int)(percent * firstSorted.length);
		return firstSorted[pos];
	}

	public void printOut() {
		for (int i = 0; i < first.length; i++) {
			System.out.printf("%f\n", first[i]);
		}
	}

	public double[] getFirst() {
		return first;
	}

	public double[] getFirstSorted() {
		return firstSorted;
	}

	public int size() {
		return firstIndex;
	}
}
