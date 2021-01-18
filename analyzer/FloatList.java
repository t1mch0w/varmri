import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

class FloatList {
	//int lengthByDefault = 10000000;
	int lengthByDefault = 1000;
	int currLength;
	public float[] first;

	public int firstIndex;

	public float[] firstSorted;
	
	public FloatList() {
		first = new float[lengthByDefault];
		firstIndex = 0;
		currLength = lengthByDefault;
	}

	public void add(float firstElement) {
		if (firstIndex >= currLength) {
			float[] tmpFirst = first;
			first = new float[2 * currLength];
			System.arraycopy(tmpFirst, 0, first, 0, currLength); 
			currLength *= 2;
		}
		first[firstIndex++] = firstElement;
	}

	public void reverseArray(float[] arr) {
		int i = 0;
		int j = arr.length - 1;
		float tmp = 0;

		while (i < j) {
			tmp = arr[i];
			arr[i] = arr[j];
			arr[j] = tmp;
			i++;
			j--;
		}	
	}

	public float[] sortArray(float[] array) {
		float[] res = new float[firstIndex];
		System.arraycopy(array, 0, res, 0, firstIndex);
		Arrays.sort(res);
		return res;
	}

	public void sort() {
		firstSorted = sortArray(first);
	}

	public float getResult(float percent) {
		this.sort();
		int pos = (int)(percent * firstSorted.length);
		return firstSorted[pos];
	}

	public void printOut() {
		for (int i = 0; i < first.length; i++) {
			System.out.printf("%f\n", first[i]);
		}
	}

	public float[] getFirst() {
		return first;
	}

	public float[] getFirstSorted() {
		return firstSorted;
	}

	public int size() {
		return firstIndex;
	}
}
