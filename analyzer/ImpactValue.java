import java.util.*;

class ImpactValue extends Thread {
	DoubleListPair doublePair;
	double pTarget;
	double threshold;
	double pTargetLowerBound;
	double pTargetUpperBound;
	double result;
	double removedPercent;

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
			removedPercent = cf.getRemovedPercent();
			//System.out.printf("threshold = %f, removedPercent = %f\n", threshold, removedPercent);
			//mapInflectionPoints.put(key, cfMap.get(key).getResult());
		} catch (Exception e) {
			System.out.println("Exception caught in curveFit().");
		}

		ArrayList<Integer> removedList = getRemovedList(doublePair.getSecond(), doublePair.size(), threshold);
		//System.out.printf("Impact value starts to remove %d events of %d events (%f%%).\n", removedList.size(), doublePair.getSecond().size(), (double) removedList.size() / doublePair.getSecond().size());
		double latencyRemoved = getLatency(getLatencyList(doublePair.getFirst(), doublePair.size(), removedList), pTarget, removedList.size());
		double latencyNormal = getLatency(doublePair.getFirstSorted(), pTarget, 0);
		//double latencyRemoved = getLatency(getLatencyList(doublePair.getFirst(), removedList), pTargetLowerBound, pTargetUpperBound, removedList.size());
		//double latencyNormal = getLatency(doublePair.getFirstSorted(), pTargetLowerBound, pTargetUpperBound);
		//System.out.printf("latencyNormal = %f, latencyRemoved = %f\n", latencyNormal, latencyRemoved);
		result = (latencyNormal -  latencyRemoved ) / latencyNormal; 
	}

	public double getThreshold() {
		return threshold;
	}

	public double getResult() {
		return result;
	}

	public double getRemovedPercent() {
		return removedPercent;
	}

	public ArrayList<Integer> getRemovedList(double[] eventValueList, int size, double threshold) {
		ArrayList<Integer> removedList = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			if (eventValueList[i] > threshold) {
				removedList.add(i);	
			}
		}
		return removedList;
	}

	public double[] getLatencyList(double[] latencyList, int size, ArrayList<Integer> removedList) {
		double[] remainedLatencyList = new double[size];
		System.arraycopy(latencyList, 0, remainedLatencyList, 0, size);
		for (int removePos : removedList) {
			remainedLatencyList[removePos] = 0;
		}
		Arrays.sort(remainedLatencyList);
		return remainedLatencyList;
	}

	public double getLatency(double[] latencyList, double pTarget, int numRemoved) {
		int total = latencyList.length - numRemoved;
		int pos = (int)(pTarget * total) + numRemoved;
		return latencyList[pos];
	}

	public double getLatency(ArrayList<Double> latencyList, double pTargetLowerBound, double pTargetUpperBound, int numRemoved) {
		int total = latencyList.size() - numRemoved;
		int spos = (int)(pTargetLowerBound * total) + numRemoved;
		int epos = (int)(pTargetUpperBound * total) + numRemoved;
		int count = 0;
		double avgLatency = 0;
		for (int i = spos; i < epos; i++) {
			avgLatency += latencyList.get(i);
			count++;
		}
		//System.out.printf("spos = %d, epos= %d\n", spos, epos);
		return avgLatency/count;
	}

	public double getLatency(ArrayList<Double> latencyList, double pTargetLowerBound, double pTargetUpperBound) {
		return getLatency(latencyList, pTargetLowerBound, pTargetUpperBound, 0);
	}
}
