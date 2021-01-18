import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

class FloatListPair {
	//int lengthByDefault = 10000000;
	int lengthByDefault = 1000;
	int currLength;
	public float[] first;
	public float[] second;

	public int firstIndex;
	public int secondIndex;

	public float[] firstSorted;
	public float[] secondSorted;
	
	public FloatListPair() {
		first = new float[lengthByDefault];
		second = new float[lengthByDefault];
		firstIndex = 0;
		secondIndex = 0;
		currLength = lengthByDefault;
	}

	public void addData(float firstElement, float secondElement) {
		if (firstIndex >= currLength) {
			float[] tmpFirst = first;
			float[] tmpSecond = second;
			first = new float[2 * currLength];
			second = new float[2 * currLength];
			System.arraycopy(tmpFirst, 0, first, 0, currLength); 
			System.arraycopy(tmpSecond, 0, second, 0, currLength); 
			currLength *= 2;
		}
		first[firstIndex++] = firstElement;
		second[secondIndex++] = secondElement;
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
		//reverseArray(res);
		return res;
	}

	public void sort() {
		firstSorted = sortArray(first);
		secondSorted = sortArray(second);
	}

	public float[][] getFloatArray(int column, int spos, int epos) {
		float[] targetList = column == 0 ? firstSorted : secondSorted;
		int total = epos - spos;
		float[][] floatArray = new float[total][2];
		for (int i = 0; i < total; i++) {
			floatArray[i][0] = targetList[i + spos];
			floatArray[i][1] = i + spos;
		}
		return floatArray;
	}
	
	public void printOut() {
		for (int i = 0; i < first.length; i++) {
			System.out.printf("%f %f\n", first[i], second[i]);
		}
	}

	public float[] getFirst() {
		return first;
	}

	public float[] getSecond() {
		return second;
	}

	public float[] getFirstSorted() {
		return firstSorted;
	}

	public float[] getSecondSorted() {
		return secondSorted;
	}

	public int size() {
		return firstIndex;
	}
}
