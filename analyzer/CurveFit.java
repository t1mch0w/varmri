class CurveFit extends Thread {
	int numOfSegments = 1000;
	double slopeThreshold = 0.01;

	DoubleListPair pairs;
	double pTarget = 0;
	double pTargetLowerBound = 0;
	double pTargetUpperBound = 0;
	double delta = 0.02;
	int totalDataPoints = 0;
	int numOfDataPoints = 0; 
	int targetColumn = 0;
	int lowerBound = 0;
	int upperBound = 0;
	double result;
	double removedPercent;

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
		this.targetColumn = targetColumn;
		this.totalDataPoints = pairs.size();
		this.numOfDataPoints = (int)Math.ceil(1.0 * totalDataPoints / numOfSegments);

		if (pTarget < 0.5) {
			pTargetLowerBound = 1 - (1 - pTarget - delta);
			pTargetUpperBound = 1 - (1 - pTarget - 2 * delta);
		}
		else {
			pTargetLowerBound = 1 - ((1 - pTarget) * 2 + delta);
			pTargetUpperBound = 1 - ((1 - pTarget) * 2);
		}
		lowerBound = (int)Math.floor(pTargetLowerBound * totalDataPoints);
		upperBound = (int)Math.floor(pTargetUpperBound * totalDataPoints);
	}

	// Get results
	public double getResult() {
		return result;
	}

	public double getRemovedPercent() {
		return removedPercent; 
	}

	public void run() {
		double[][] allData = pairs.getDoubleArray(targetColumn,0,totalDataPoints-1);
		CDFAnalyzer cdfAnalyzer = new CDFAnalyzer(allData);
		result = cdfAnalyzer.getTurningPointAt(lowerBound, upperBound);
		removedPercent = 1 - cdfAnalyzer.getTurningPointPercent();
	}
}
