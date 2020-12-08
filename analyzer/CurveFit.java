class CurveFit extends Thread {
	int numOfSegments = 1000;
	double slopeThreshold = 0.01;

	DoubleListPair pairs;
	double pTarget = 0;
	double pTargetLowerBound = 0;
	int totalDataPoints = 0;
	int numOfDataPoints = 0; 
	int startSegment = 0;
	int targetColumn = 0;

//	SimpleRegression currRegression;
//	SimpleRegression lastRegression;

	double result;

	public CurveFit() {
	}

	public CurveFit(DoubleListPair pairs, double pTarget, int targetColumn) {
		this(1000, 0.01, pairs, pTarget, targetColumn);
	}

	public CurveFit(int numOfSegments, double slopeThreshold, DoubleListPair pairs, double pTarget, int targetColumn) {
		this.numOfSegments = numOfSegments;
		this.slopeThreshold = slopeThreshold;
		this.pairs = pairs;
		this.pTarget = pTarget;
		pTargetLowerBound = pTarget - (1 - pTarget) * 2;
		this.targetColumn = targetColumn;
		totalDataPoints = pairs.size();
		this.numOfDataPoints = (int)Math.ceil(1.0 * totalDataPoints / numOfSegments);
		startSegment = (int)Math.floor(pTargetLowerBound * totalDataPoints / numOfDataPoints);
	}

	// Get results
	public double getResult() {
		return result;
	}

	public void run() {
//		int found = 0;
//		double[][] lastArray = null;
//		double[][] currArray;
		double[][] allData = pairs.getDoubleArray(targetColumn,0,totalDataPoints-1);
		CDFAnalyzer cdfAnalyzer = new CDFAnalyzer(allData);
		int lower = (int) ((startSegment-0.05*numOfSegments)*numOfDataPoints);
		int target = startSegment*numOfDataPoints;
		result = cdfAnalyzer.getTurningPointAt(lower,target);
//		for (int spos = startSegment; spos >= 0 && (int)(spos - numOfSegments * 0.05) >= 0 && found == 0; spos--) {
//			currRegression = new SimpleRegression();
//			currArray = pairs.getDoubleArray(targetColumn, spos * numOfDataPoints, (spos + 1) * numOfDataPoints);
//			currRegression.addData(currArray);
//			if (lastRegression != null) {
//				double lastSlope = lastRegression.getSlope();
//				double currSlope = currRegression.getSlope();
//				if (lastRegression.getRSquare() > 0.9 && currRegression.getRSquare() > 0.9) {
//					// Current regression's slope INFINITE or change is larger then threshold
//					if (currSlope == Double.NaN || Math.abs(lastSlope/currSlope - 1) > slopeThreshold) {
//						result = lastArray[lastArray.length - 1][0];
//						found = 1;
//						break;
//					}
//				}
//			}
//			lastRegression = currRegression;
//			lastArray = currArray;
//		}
//
//		// Double check whether we really found the inflection point.
//		if (found == 0) {
//			currArray = pairs.getDoubleArray(targetColumn, startSegment * numOfDataPoints, (startSegment + 1) * numOfDataPoints);
//			result = currArray[currArray.length - 1][0];
//			//System.out.println("[Error] Curve fitting fails. No inflection points found in the close scope. Threshold = " + currArray[currArray.length - 1][0] + " startSegment = " + startSegment + " numOfDataPoints = " + numOfDataPoints);
//		}
	}

//	// Used for test
//	public void fit(double[][] data) {
//		currRegression = new SimpleRegression();
//		currRegression.addData(data);
//	}
//
//	// Used for test
//	public double getRSquare() {
//		return currRegression.getRSquare();
//	}
//
//	// Used for test
//	public double getSlope() {
//		return currRegression.getSlope();
//	}
}
