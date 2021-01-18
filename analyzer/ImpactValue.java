import java.util.*;

class ImpactValue extends Thread {
	FloatListPair floatPair;
	float pTarget;
	float threshold;
	float pTargetLowerBound;
	float pTargetUpperBound;
	float result;
	float removedPercent;

	public ImpactValue(FloatListPair floatPair, float pTarget, float pTargetLowerBound, float pTargetUpperBound) {
		this.floatPair = floatPair;
		this.pTarget = pTarget;
		this.pTargetLowerBound = pTargetLowerBound;
		this.pTargetUpperBound = pTargetUpperBound;
	}

	public void run() {
		try {
			floatPair.sort();
			CurveFit cf = new CurveFit(floatPair, pTarget, 1);
			cf.start();
			cf.join();
			threshold = cf.getResult();
			removedPercent = cf.getRemovedPercent();
			//System.out.printf("threshold = %f, removedPercent = %f\n", threshold, removedPercent);
			//mapInflectionPoints.put(key, cfMap.get(key).getResult());
		} catch (Exception e) {
			System.out.println("Exception caught in curveFit().");
		}

		ArrayList<Integer> removedList = getRemovedList(floatPair.getSecond(), floatPair.size(), threshold);
		//System.out.printf("Impact value starts to remove %d events of %d events (%f%%).\n", removedList.size(), floatPair.getSecond().size(), (float) removedList.size() / floatPair.getSecond().size());
		float latencyRemoved = getLatency(getLatencyList(floatPair.getFirst(), floatPair.size(), removedList), pTarget, removedList.size());
		float latencyNormal = getLatency(floatPair.getFirstSorted(), pTarget, 0);
		//float latencyRemoved = getLatency(getLatencyList(floatPair.getFirst(), removedList), pTargetLowerBound, pTargetUpperBound, removedList.size());
		//float latencyNormal = getLatency(floatPair.getFirstSorted(), pTargetLowerBound, pTargetUpperBound);
		//System.out.printf("latencyNormal = %f, latencyRemoved = %f\n", latencyNormal, latencyRemoved);
		result = (latencyNormal -  latencyRemoved ) / latencyNormal; 
	}

	public float getThreshold() {
		return threshold;
	}

	public float getResult() {
		return result;
	}

	public float getRemovedPercent() {
		return removedPercent;
	}

	public ArrayList<Integer> getRemovedList(float[] eventValueList, int size, float threshold) {
		ArrayList<Integer> removedList = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			if (eventValueList[i] > threshold) {
				removedList.add(i);	
			}
		}
		return removedList;
	}

	public float[] getLatencyList(float[] latencyList, int size, ArrayList<Integer> removedList) {
		float[] remainedLatencyList = new float[size];
		System.arraycopy(latencyList, 0, remainedLatencyList, 0, size);
		for (int removePos : removedList) {
			remainedLatencyList[removePos] = 0;
		}
		Arrays.sort(remainedLatencyList);
		return remainedLatencyList;
	}

	public float getLatency(float[] latencyList, float pTarget, int numRemoved) {
		int total = latencyList.length - numRemoved;
		int pos = (int)(pTarget * total) + numRemoved;
		return latencyList[pos];
	}

	public float getLatency(ArrayList<Float> latencyList, float pTargetLowerBound, float pTargetUpperBound, int numRemoved) {
		int total = latencyList.size() - numRemoved;
		int spos = (int)(pTargetLowerBound * total) + numRemoved;
		int epos = (int)(pTargetUpperBound * total) + numRemoved;
		int count = 0;
		float avgLatency = 0;
		for (int i = spos; i < epos; i++) {
			avgLatency += latencyList.get(i);
			count++;
		}
		//System.out.printf("spos = %d, epos= %d\n", spos, epos);
		return avgLatency/count;
	}

	public float getLatency(ArrayList<Float> latencyList, float pTargetLowerBound, float pTargetUpperBound) {
		return getLatency(latencyList, pTargetLowerBound, pTargetUpperBound, 0);
	}
}
