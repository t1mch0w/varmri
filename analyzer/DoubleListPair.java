import java.util.ArrayList;

class DoubleListPair {
	public ArrayList<Double> first;
	public ArrayList<Double> second;
	
	public DoubleListPair() {
		first = new ArrayList<Double>();
		second = new ArrayList<Double>();
	}

	public void addData(double firstElement, double secondElement) {
		first.add(firstElement);
		second.add(secondElement);
	}
}
