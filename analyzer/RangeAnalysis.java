class RangeAnalysis extends Thread {
	DoubleListPair eventPairs;
	double pTargetLowerBound;
	double pTargetUpperBound;
	double result = 0.0;

	public RangeAnalysis(DoubleListPair eventPairs, double pTargetLowerBound, double pTargetUpperBound) {
		this.eventPairs = eventPairs;
		this.pTargetLowerBound = pTargetLowerBound;
		this.pTargetUpperBound = pTargetUpperBound;
	}

	public void run() {
		int total = 0;
		int numOfEvent = eventPairs.size();
		int startIdx = (int)(pTargetLowerBound * numOfEvent);
		int endIdx = (int)(pTargetUpperBound * numOfEvent);

		double[] latency = eventPairs.getFirst();
		double[] event = eventPairs.getSecond();
		double[] latencySorted = eventPairs.getFirstSorted();

		double startValue = latencySorted[startIdx];
		double endValue = latencySorted[endIdx];
		for (int i = 0; i < numOfEvent; i++) {
			if (latency[i] >= startValue && latency[i] <= endValue) {
				result += event[i];
				total++;
			}
		}
		result /= total;
	}

	public double getResult() {
		return result;
	}
}
