class CurveFit extends Thread {
	int numOfSegments = 1000;
	float slopeThreshold = 0.01f;

	FloatListPair pairs;
	float pTarget = 0;
	float pTargetLowerBound = 0;
	float pTargetUpperBound = 0;
	float delta = 0.02f;
	int totalDataPoints = 0;
	int numOfDataPoints = 0; 
	int targetColumn = 0;
	int lowerBound = 0;
	int upperBound = 0;
	float result;
	float removedPercent;

	public CurveFit() {
	}

	public CurveFit(FloatListPair pairs, float pTarget, int targetColumn) {
		this(1000, 0.01f, pairs, pTarget, targetColumn);
	}

	public CurveFit(int numOfSegments, float slopeThreshold, FloatListPair pairs, float pTarget, int targetColumn) {
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
	public float getResult() {
		return result;
	}

	public float getRemovedPercent() {
		return removedPercent; 
	}

	public void run() {
		float[][] allData = pairs.getFloatArray(targetColumn, 0, totalDataPoints - 1);
		CDFAnalyzer cdfAnalyzer = new CDFAnalyzer(allData);
		result = cdfAnalyzer.getTurningPointAt(lowerBound, upperBound);
		removedPercent = 1 - cdfAnalyzer.getTurningPointPercent();
	}
}
