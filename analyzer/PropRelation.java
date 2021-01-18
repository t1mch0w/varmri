import org.apache.commons.math3.stat.regression.SimpleRegression;
import java.util.ArrayList;

class PropRelation extends Thread {
	double[][] data;
	FloatListPair pairs;
	SimpleRegression regression;
	float rSquare = 0;

	public PropRelation(FloatListPair pairs) {
		this.pairs = pairs;	
		this.data = new double[pairs.size()][2];
		regression = new SimpleRegression(false);
	}

	public float getResult() {
		return (float)(regression.getRSquare());
	}

	public void run() {
		float[] first = pairs.getFirst();
		float[] second = pairs.getSecond();
		for (int i = 0 ; i < pairs.size(); i++) {
			data[i][0] = first[i];
			data[i][1] = second[i];
		}
		regression.addData(data);	
	}
}
