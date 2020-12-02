import java.util.*;

class ImpactValue extends Thread {
	DoubleListPair doublePair;
	double pTarget;
	double threshold;
	double pTargetLowerBound;
	double pTargetUpperBound;
	double result;
	

	public ImpactValue(DoubleListPair doublePair, double pTarget, double pTargetLowerBound, double pTargetUpperBound) {
		this.doublePair = doublePair;
		this.pTarget = pTarget;
		this.pTargetLowerBound = pTargetLowerBound;
		this.pTargetUpperBound = pTargetUpperBound;
	}

	public void run() {
		try {
			doublePair.sort();
			CurveFit cf = new CurveFit(doublePair, pTarget, 1);
			cf.start();
			cf.join();
			threshold = cf.getResult();
			//mapInflectionPoints.put(key, cfMap.get(key).getResult());
		} catch (Exception e) {
			System.out.println("Exception caught in curveFit().");
		}

		ArrayList<Integer> removedList = getRemovedList(doublePair.getSecond(), threshold);
		//System.out.printf("Impact value starts to remove %d events of %d events (%f%%).\n", removedList.size(), doublePair.getSecond().size(), (double) removedList.size() / doublePair.getSecond().size());
		double latencyRemoved = getLatency(getLatencyList(doublePair.getFirst(), removedList), pTargetLowerBound, pTargetUpperBound);
		double latencyNormal = getLatency(doublePair.getFirstSorted(), pTargetLowerBound, pTargetUpperBound);
		//System.out.printf("latencyNormal = %f, latencyRemoved = %f\n", latencyNormal, latencyRemoved);
		result = latencyNormal / latencyRemoved - 1; 
	}

	public double getResult() {
		return result;
	}

	public ArrayList<Integer> getRemovedList(ArrayList<Double> eventValueList, double threshold) {
		ArrayList<Integer> removedList = new ArrayList<>();
		for (int i = 0; i < eventValueList.size(); i++) {
			if (eventValueList.get(i) > threshold) {
				removedList.add(i);	
			}
		}
		return removedList;
	}

	public ArrayList<Double> getLatencyList(ArrayList<Double> latencyList, ArrayList<Integer> removedList) {
		ArrayList<Double> remainedLatencyList = new ArrayList<>(latencyList);
		Collections.sort(removedList, Collections.reverseOrder());
		for (int removePos : removedList) {
			remainedLatencyList.remove(removePos);
		}
		Collections.sort(remainedLatencyList);
		return remainedLatencyList;
	}

	public double getLatency(ArrayList<Double> latencyList, double pTargetLowerBound, double pTargetUpperBound) {
		int total = latencyList.size();
		int spos = (int)(pTargetLowerBound * total);
		int epos = (int)(pTargetUpperBound * total);
		int count = 0;
		double avgLatency = 0;
		for (int i = spos; i < epos; i++) {
			avgLatency += latencyList.get(i);
			count++;
		}
		//System.out.printf("spos = %d, epos= %d\n", spos, epos);
		return avgLatency/count;
	}


}
