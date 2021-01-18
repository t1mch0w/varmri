class RangeAnalysis extends Thread {
	FloatListPair eventPairs;
	float pTargetLowerBound;
	float pTargetUpperBound;
	float result = 0.0f;

	public RangeAnalysis(FloatListPair eventPairs, float pTargetLowerBound, float pTargetUpperBound) {
		this.eventPairs = eventPairs;
		this.pTargetLowerBound = pTargetLowerBound;
		this.pTargetUpperBound = pTargetUpperBound;
	}

	public void run() {
		int total = 0;
		int numOfEvent = eventPairs.size();
		int startIdx = (int)(pTargetLowerBound * numOfEvent);
		int endIdx = (int)(pTargetUpperBound * numOfEvent);

		float[] latency = eventPairs.getFirst();
		float[] event = eventPairs.getSecond();
		float[] latencySorted = eventPairs.getFirstSorted();

		float startValue = latencySorted[startIdx];
		float endValue = latencySorted[endIdx];
		for (int i = 0; i < numOfEvent; i++) {
			if (latency[i] >= startValue && latency[i] <= endValue) {
				result += event[i];
				total++;
			}
		}
		result /= total;
	}

	public float getResult() {
		return result;
	}
}
