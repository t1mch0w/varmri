import org.apache.commons.math3.stat.regression.SimpleRegression;

class CurveFit {
	SimpleRegression regression;
	public CurveFit() {
		regression = new SimpleRegression(false);
	}

	public void fit(double[][] data) {
		regression.addData(data);
	}

	public double getRSquare() {
		return regression.getRSquare();
	}
}
