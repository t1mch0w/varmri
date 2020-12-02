import org.apache.commons.math3.stat.regression.SimpleRegression;
import java.util.ArrayList;

class PropRelation extends Thread {
	double[][] data;
	DoubleListPair pairs;
	SimpleRegression regression;
	double rSquare = 0;

	public PropRelation(DoubleListPair pairs) {
		this.pairs = pairs;	
		this.data = new double[pairs.size()][2];
		regression = new SimpleRegression(false);
	}

	public double getResult() {
		return regression.getRSquare();
	}

	public void run() {
		ArrayList<Double> first = pairs.getFirst();
		ArrayList<Double> second = pairs.getSecond();
		for (int i = 0 ; i < pairs.size(); i++) {
			data[i][0] = first.get(i);
			data[i][1] = second.get(i);
		}
		regression.addData(data);	
	}
}
