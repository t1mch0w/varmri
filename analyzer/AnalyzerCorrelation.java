import java.io.*;
import java.util.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.charset.*;

class AnalyzerCorrelation {
	static float CPUFREQ = 2.2f;
	static List<String> eventName = Arrays.asList("RUNNABLE", "WAITING", "HARDIRQ", "SOFTIRQ", "EXITTOUSER", "MEMMIGRATION", "INST", "CYCLE", "KINST", "KCYCLE", "1_FREQ");

	int filterType = -1;
	//int filterType = 2;
	float pTarget = 0.99f;
	float pTargetLowerBound = 0.99f - 0.005f;
	float pTargetUpperBound = 0.99f + 0.005f;

	//Used for memory benchmark
	//float pTarget = 0.25;
	//float pTargetLowerBound = 0.245;
	//float pTargetUpperBound = 0.255;

	float propRSquared = 0.99f;
	float windowLengthInHours = 1;
	float targetWindow = 0;

	String testName = "";

	HashMap<String, String> regValueToId;
	HashMap<String, Float> mapInflectionPoints;
	HashMap<String, Float> impactValueResults;
	HashMap<String, Float> removedPercentResults;
	HashMap<String, Float> propRelationResults;
	HashMap<String, Float> rangeAnalysisResults;
	HashMap<String, Float> jaccardResults;
	HashMap<String, Float> curveFittingThresholds;

	HashMap<String, ArrayList<String>> priorityList;
	HashMap<String, Float> finalImpactValueResults;
	HashMap<String, String> finalFilteredReasons;

	TreeMap<Float, Float> switchInfo;
	HashMap<Float, ArrayList<String>> msrInfo;
	HashMap<String, FloatListPair> latencyPairs;
	HashMap<String, FloatListPair> msrPairs;

	FloatList dList;

	// Debug
	long startTime;
	long endTime;
	
	public AnalyzerCorrelation(TreeMap<Float, Float> switchInfo, HashMap<Float, ArrayList<String>> msrInfo, HashMap<String, FloatListPair> latencyPairs, HashMap<String, FloatListPair> msrPairs) {
		regValueToId = new HashMap<String, String>();
		mapInflectionPoints = new HashMap<String, Float>();
		impactValueResults = new HashMap<String, Float>();
		removedPercentResults = new HashMap<String, Float>();
		propRelationResults = new HashMap<String, Float>();
		rangeAnalysisResults = new HashMap<String, Float>();
		jaccardResults = new HashMap<String, Float>();
		curveFittingThresholds = new HashMap<>();
		priorityList = new HashMap<>();
		finalImpactValueResults = new HashMap<>();
		finalFilteredReasons =  new HashMap<>();
		startTime = 0;
		endTime = 0;

		dList = new FloatList();

		this.switchInfo = switchInfo;
		this.msrInfo  = msrInfo;
		this.latencyPairs = latencyPairs;
		this.msrPairs = msrPairs;
	}

	public void readPriorityList() throws FileNotFoundException, IOException {
		File priorityFile = new File("./priorityList");
		FileReader fr = new FileReader(priorityFile);
		BufferedReader br = new BufferedReader(fr);
		String line;

		while((line = br.readLine()) != null) {
			if (line.contains("#")) continue;
			String splitArray[]= line.toUpperCase().split(" ");
			ArrayList<String> tmpList = new ArrayList<>();
			for (int i = 1; i < splitArray.length; i++) {
					tmpList.add(splitArray[i]);
			}
			priorityList.put(splitArray[0], tmpList);
		}
	}

	public void readRegValueToId(HashMap<String, FloatListPair> latencyPairs, HashMap<String, FloatListPair> msrPairs) throws FileNotFoundException, IOException {
		File switchFile = new File("./msrDesc");
		FileReader fr = new FileReader(switchFile);
		BufferedReader br = new BufferedReader(fr);
		String line;

		while((line = br.readLine()) != null) {
			String splitArray[]= line.toUpperCase().split(" ");
			regValueToId.put(splitArray[1], splitArray[0]);
		}
		
		for (String eName : eventName) {
			regValueToId.put(eName, eName);
		}

		for (String eName : regValueToId.keySet()) {
			latencyPairs.put(eName, new FloatListPair());
		}

		String msrKey = null;
		for (String firstName : regValueToId.keySet()) {
			for (String secondName : regValueToId.keySet()) { 
				if (!firstName.equals(secondName)) {
					if (firstName.compareTo(secondName) < 0) {
						msrKey = firstName + "-" + secondName;
					}
					else {
						msrKey = secondName + "-" + firstName;
					}
					msrPairs.put(msrKey, new FloatListPair());
				}
			}
		}
	}

	public void readSwitchInfo(String switchFilePath, String msrFilePath, TreeMap<Float, Float> switchInfo, HashMap<Float, ArrayList<String>> msrInfo) throws FileNotFoundException, IOException {
		File switchFile = new File(switchFilePath);
		FileReader fr = new FileReader(switchFile);
		BufferedReader br = new BufferedReader(fr);
		String line = null;

		ArrayList<Float> stimeList = new ArrayList<>();
		ArrayList<Float> etimeList = new ArrayList<>();

		while((line = br.readLine()) != null) {
			String splitArray[]= line.toUpperCase().split(" ");
			int splitLength = splitArray.length;
			float stime = Long.parseLong(splitArray[splitLength - 2]) / CPUFREQ;
			float etime = Long.parseLong(splitArray[splitLength - 1]) / CPUFREQ;
			stimeList.add(stime);
			etimeList.add(etime);
		}

		for (int i = 1; i < stimeList.size() - 1; i++) {
			switchInfo.put(etimeList.get(i - 1), stimeList.get(i));	
		}
		switchInfo.put(etimeList.get(etimeList.size() - 1), Float.MAX_VALUE);

		File msrFile = new File(msrFilePath);
		fr = new FileReader(msrFile);
		br = new BufferedReader(fr);
		ArrayList<String> msrList = new ArrayList<String>();

		int msrIdx = 0;
		while((line = br.readLine()) != null) {
			if (line.contains("Level")) {
				String splitArray[]= line.toUpperCase().split(" ");
				int length = splitArray.length;
				String level = splitArray[length - 8];
				String msr = splitArray[length - 6];
				msrList.add(msr);
				if (msrList.size() == 4) {
					msrList.add(level);
					msrInfo.put(etimeList.get(msrIdx), msrList);
					msrList = new ArrayList<String>();
					msrIdx++;
				}
			}
		}
	}

	public void readVarResults(String traceFilePath, TreeMap<Float, Float> switchInfo, HashMap<Float, ArrayList<String>> msrInfo, HashMap<String, FloatListPair> latencyPairs, HashMap<String, FloatListPair> msrPairs) throws FileNotFoundException, IOException {
		int count = 0;
		String msrKey = null;
		String firstName = null;
		String secondName = null;
		float switchStartTime = switchInfo.firstKey();
		//DataInputStream is = new DataInputStream(new FileInputStream(traceFilePath));
		DataInputStream is = new DataInputStream(new BufferedInputStream(new FileInputStream(traceFilePath)));
		while (is.available() > 0) {
			int skip = 0;
			VarResult varResult = new VarResult(is);
            for (int i = 0; i < 14; i++) {
                if (i == 12) continue;
                if (varResult.results[i] < 0 || varResult.results[i] > 1e10) {
                    skip = 1;
                    break;
                }
            }
			if (skip == 1 || varResult.latency == 0 || varResult.freq > 4 || varResult.results[7] / varResult.results[6] < 0.001) continue;

			if (filterType != -1 && varResult.type != filterType) {
				continue;
			}

			float switchIdx = -1;
			Float switchTime = switchInfo.floorKey(varResult.results[12]);
			if (switchTime == null) continue;
			if (varResult.results[12] + varResult.latency <= switchInfo.get(switchTime)) {
				switchIdx = switchTime;
			}
			
			// The request is in the middle of a MSR switch 
			if (switchIdx == -1) continue;
			
			// Only focus on the first requests 
			if (switchIdx - switchStartTime < 3600 * 1e9 * windowLengthInHours * targetWindow) continue;
			if (switchIdx - switchStartTime >= 3600 * 1e9 * (windowLengthInHours) * (targetWindow + 1)) {
				break;
			}
			if (startTime == 0) startTime = System.nanoTime();

			// Prepare latencyPairs for impact values
			// Basic kernel events and fixed PMUs
			for (int i = 0; i < 8; i++) {
				int eventIdx = i;
				if (eventIdx >= 6 && eventIdx < 8) {
					if (msrInfo.get(switchIdx).get(4).equals("0")) {
							eventIdx += 2;
					}
				}
				//latencyPairs.putIfAbsent(eventName.get(eventIdx), new FloatListPair());
				FloatListPair tmpList = latencyPairs.get(eventName.get(eventIdx));
				tmpList.addData(varResult.latency, varResult.results[i]);
			}

			// Add latency and MSR
			for (int i = 8; i < 12; i++) {
				int eventIdx = i - 8;
				//latencyPairs.putIfAbsent(msrInfo.get(switchIdx).get(eventIdx), new FloatListPair());
				if (msrInfo.get(switchIdx).get(eventIdx).equals("0x000000")) continue;
				FloatListPair tmpList = latencyPairs.get(msrInfo.get(switchIdx).get(eventIdx));
				if (tmpList == null) System.out.println(msrInfo.get(switchIdx).get(eventIdx));
				tmpList.addData(varResult.latency, varResult.results[i]);
			}

			// Add latency and Freq
			FloatListPair freqList = latencyPairs.get("1_FREQ");
			freqList.addData(varResult.latency, 1.0f / varResult.freq);

			// Prepare msrPairs for proportional relationship and jaccard similarity
			for (int i = 8; i < 11; i++) {
				for (int j = i + 1; j < 12; j++) {
					firstName = msrInfo.get(switchIdx).get(i - 8);
					secondName = msrInfo.get(switchIdx).get(j - 8);
					if (firstName.equals("0x000000") || secondName.equals("0x000000")) continue;
					if (firstName.equals(secondName)) continue;
					else if (firstName.compareTo(secondName) < 0) {
						msrKey = firstName + "-" + secondName;
					}
					else {
						msrKey = secondName + "-" + firstName;
					}
					//msrPairs.putIfAbsent(msrKey, new FloatListPair());
					FloatListPair tmpList = msrPairs.get(msrKey);
					tmpList.addData(varResult.results[i], varResult.results[j]);
				}
			}

			// Prepare pairs between kernel length/CYCLE/INST and MSR
			for (int i = 0; i < 8; i++) {
				for (int j = 8; j < 12; j++) {
					int eventIdx = i;
					if (i >= 6 && msrInfo.get(switchIdx).get(4).equals("0")) {
							eventIdx += 2;
					}
					secondName = msrInfo.get(switchIdx).get(j - 8);
					if (secondName.equals("0x000000")) continue;
					firstName = eventName.get(eventIdx);
					msrKey = secondName + "-" + firstName;
					//msrPairs.putIfAbsent(msrKey, new FloatListPair());
					FloatListPair tmpList = msrPairs.get(msrKey);
					tmpList.addData(varResult.results[i], varResult.results[j]);
				}
			}

			//System.out.println(varResult.latency);			
			dList.add(varResult.latency);			

			count++;
		}
		endTime = System.nanoTime();
		System.out.printf("Parsing takes %f seconds\n", (endTime - startTime) / 1e9);
		//System.out.printf("#request = %d\n", count);
	}

	public void generateLatencyPair(HashMap<String, FloatListPair> latencyPairs) throws IOException {
		for (String key : latencyPairs.keySet()) {
			FileWriter fileWriter = new FileWriter(key + "_latency_pairs.txt");
			FloatListPair flp = latencyPairs.get(key);
			for (int i = 0; i < flp.first.length; i++) {
				fileWriter.write(String.format("%f %f\n", flp.first[i], flp.second[i]));
			}
			fileWriter.close();
		}
	}

	//The first step
	public HashMap<String, ImpactValue> impactValueAnalysis(HashMap<String, FloatListPair> latencyPairs) {
		//long stime = System.nanoTime();
		HashMap<String, ImpactValue> ivMap = new HashMap<>();
		for (String key : latencyPairs.keySet()) {
			if (latencyPairs.get(key).size() == 0) continue;
			ImpactValue iv = new ImpactValue(latencyPairs.get(key), pTarget, pTargetLowerBound, pTargetUpperBound);
			iv.start();
			ivMap.put(key, iv);
		}
		//long etime = System.nanoTime();
		//System.out.printf("Impact value takes %f seconds.\n", (etime - stime) / 1e9);
		return ivMap;
	}

	public void generateOutputImpactValue() throws IOException {
		FileWriter fileWriter = new FileWriter(testName + "_impact_value.txt");
		for (String key : impactValueResults.keySet()) {
			fileWriter.write(String.format("[Impact Value] %s %d %f %f\n", regValueToId.get(key), latencyPairs.get(key).size(), removedPercentResults.get(key), impactValueResults.get(key)));
		}
		fileWriter.close();
	}

	//The second step
	public HashMap<String, PropRelation> propRelationshipAnalysis(HashMap<String, FloatListPair> msrPairs) {
		//long stime = System.nanoTime();
		HashMap<String, PropRelation> prMap = new HashMap<>();
		for (String key : msrPairs.keySet()) {		
			if (msrPairs.get(key).size() == 0) continue;
			PropRelation pr = new PropRelation(msrPairs.get(key));
			pr.start();
			prMap.put(key, pr);
		}

		//long etime = System.nanoTime();
		//System.out.printf("PropRelation takes %f seconds.\n", (etime - stime) / 1e9);
		return prMap;
	}

	public void generateOutputPropRelation() throws IOException {
		FileWriter fileWriter = new FileWriter(testName + "_prop_relation.txt");
		for (String key : propRelationResults.keySet()) {
			String[] keySet = key.split("-");
			fileWriter.write(String.format("[Prop Relation] %s %s %d %f\n", regValueToId.get(keySet[0]), regValueToId.get(keySet[1]), msrPairs.get(key).size(), propRelationResults.get(key)));
		}
		fileWriter.close();
	}

	public void generateOutputRangeResults() throws IOException {
		FileWriter fileWriter = new FileWriter(testName + "_range_result.txt");
		for (String key : rangeAnalysisResults.keySet()) {
			fileWriter.write(String.format("[Range Result] %s %f\n", regValueToId.get(key), rangeAnalysisResults.get(key)));
		}
		fileWriter.close();
	}

	public void generateLatencyResults() throws IOException {
		FileWriter fileWriter = new FileWriter(testName + "_latency.txt");
		float[] percentList = {0.999f, 0.99f, 0.5f};
		for (float percent : percentList) {
			fileWriter.write(String.format("P%.2f %f\n", percent*100, dList.getResult(percent)));
		}
		fileWriter.close();
	}

	//The third step
	public HashMap<String, JaccardAnalysis> jaccardAnalysis(HashMap<String, FloatListPair> msrPairs) {
		//long stime = System.nanoTime();
		HashMap<String, JaccardAnalysis> jaMap = new HashMap<>();

		for (String key : msrPairs.keySet()) {
			if (msrPairs.get(key).size() == 0) continue;
			String[] keySet = key.split("-");
			float thre0 = curveFittingThresholds.get(keySet[0]);
			float thre1 = curveFittingThresholds.get(keySet[1]);
			JaccardAnalysis ja = new JaccardAnalysis(msrPairs.get(key), thre0, thre1);
			ja.start();
			jaMap.put(key, ja);
		}
		//long etime = System.nanoTime();
		//System.out.printf("Jaccard analysis takes %f seconds.\n", (etime - stime) / 1e9);

		return jaMap;
	}

	//Range Analysis
	public HashMap<String, RangeAnalysis> rangeAnalysis(HashMap<String, FloatListPair> latencyPairs) {
		HashMap<String, RangeAnalysis> rMap = new HashMap<>();
		for (String key : latencyPairs.keySet()) {
			RangeAnalysis r = new RangeAnalysis(latencyPairs.get(key), pTargetLowerBound, pTargetUpperBound);
			r.start();
			rMap.put(key, r);
		}
		return rMap;
	}

	public void generateOutputJaccardResults() throws IOException {
		FileWriter fileWriter = new FileWriter(testName + "_jaccard_similarity.txt");
		for (String key : jaccardResults.keySet()) {
			String[] keySet = key.split("-");
			fileWriter.write(String.format("[Jaccard Result] %s %s %d %f\n", regValueToId.get(keySet[0]), regValueToId.get(keySet[1]), msrPairs.get(key).size(), jaccardResults.get(key)));
		}
		fileWriter.close();
	}

	//Join function
	public void joinAndStartJaccard(HashMap<String, ImpactValue> ivMap, HashMap<String, PropRelation> prMap, HashMap<String, RangeAnalysis> rMap) {
		try {
			for (String key : ivMap.keySet()){
					ImpactValue iv = ivMap.get(key);
					iv.join();
					impactValueResults.put(key, iv.getResult());
					removedPercentResults.put(key, iv.getRemovedPercent());
					curveFittingThresholds.put(key, iv.getThreshold());
			}

			for (String key : prMap.keySet()) {
					PropRelation pr = prMap.get(key);
					pr.join();
					propRelationResults.put(key, pr.getResult());
			}

			for (String key : rMap.keySet()) {
					RangeAnalysis r = rMap.get(key);
					r.join();
					rangeAnalysisResults.put(key, r.getResult());
			}

			//Third step: get jaccard similarity
			HashMap<String, JaccardAnalysis> jaMap = jaccardAnalysis(msrPairs);

			for (String key : jaMap.keySet()) {
					JaccardAnalysis ja = jaMap.get(key);
					ja.join();
					jaccardResults.put(key, ja.getResult());
			}

		} catch (Exception e) {
			System.out.println("Exception caught in analysis().");
			e.printStackTrace();
		}
	}

	public void generateOutputFinals() throws IOException {
		// #Type (0:CYCLE, 1:CACHE, 2:INST, and MSR register value)
		String msrKey = null;
		String[] instArray = {"INST", "KINST"};
		String[] cycleArray = {"CYCLE", "KCYCLE"};
		ArrayList<String> instList = new ArrayList<>(Arrays.asList(instArray));
		ArrayList<String> cycleList = new ArrayList<>(Arrays.asList(cycleArray));
		
		// Remove from the second step (proportional relationship)
		for (String firstKey : priorityList.keySet()) {
			ArrayList<String> secondKeys = new ArrayList<>();
			for (String secondKey : priorityList.get(firstKey)) {
				if (secondKey.equals("0")) {
						secondKeys.addAll(cycleList);
				}
				else if (secondKey.equals("2")) {
						secondKeys.addAll(instList);
				}
				else {
						secondKeys.add(secondKey);
				}
			}

			for (String secondKey : secondKeys) {
				if (firstKey.compareTo(secondKey) < 0) {
					msrKey = firstKey + "-" + secondKey;
				}
				else {
					msrKey = secondKey + "-" + firstKey;
				}
				if (propRelationResults.containsKey(msrKey) && propRelationResults.get(msrKey) >= propRSquared) {
					finalImpactValueResults.put(firstKey, 0.0f);
					finalFilteredReasons.put(firstKey, String.format("Proportional to %s(%s)", regValueToId.get(secondKey), secondKey));
					break;
				}
			}
		}

		// Check jaccard similarity
		for (String firstKey : priorityList.keySet()) {
			String maxKey = null;
			float maxValue = 0.0f;
			float maxJaccard = 0.0f;
			ArrayList<String> checkedList = new ArrayList<>();
			// Already removed by the proportional relationship
			if (finalImpactValueResults.containsKey(firstKey)) continue;

			// Add basic INST and CYCLE to checkedList
			String msrLevel = priorityList.get(firstKey).get(0);
			if (msrLevel.equals("0")) {
				checkedList.addAll(cycleList);
			}
			else if (msrLevel.equals("2")) {
				checkedList.addAll(instList);
			}
			// Not only checked with level, but specified MSR
			if (priorityList.get(firstKey).size() > 1) {
				ArrayList<String> firstValue = priorityList.get(firstKey);
				for (int i = 1; i < firstValue.size(); i++) {
					checkedList.add(firstValue.get(i));
				}
			}

			// Add higher level to checkedList
			for (String key : priorityList.keySet()) {
				if (msrLevel.compareTo(priorityList.get(key).get(0)) < 0) {
					checkedList.add(key);
				}
			}

			float firstValue = impactValueResults.get(firstKey);
			for (String secondKey : checkedList) {
				if (firstKey.compareTo(secondKey) < 0) {
					msrKey = firstKey + "-" + secondKey;
				}
				else {
					msrKey = secondKey + "-" + firstKey;
				}
				// No jaccard similarity, continue
				if (!jaccardResults.containsKey(msrKey)) continue;

				float tmpValue = jaccardResults.get(msrKey) * impactValueResults.get(secondKey);
				if (tmpValue > maxValue) {
					maxKey = secondKey;
					maxValue = tmpValue;
					maxJaccard = jaccardResults.get(msrKey);
				}
			}

			if (maxValue > 0) {
				finalImpactValueResults.put(firstKey, firstValue - maxValue);
				finalFilteredReasons.put(firstKey, String.format("Remove %f due to jaccard similarity %f to %s(%s)", maxValue, maxJaccard, regValueToId.get(maxKey), maxKey));
			}
		}

		// Final output
		FileWriter fileWriter = new FileWriter(testName + "_final_result.txt");
		for (String key : impactValueResults.keySet()) {
			float finalRes = impactValueResults.get(key);
			String filteredReason =	"N/A";
			if (finalImpactValueResults.containsKey(key)) {
				finalRes = finalImpactValueResults.get(key);
				filteredReason = finalFilteredReasons.get(key);
			}
			fileWriter.write(String.format("%s(%s),%d,%f,%f,%f,%s\n", regValueToId.get(key), key, latencyPairs.get(key).size(), removedPercentResults.get(key), impactValueResults.get(key),finalRes,filteredReason));
		}
		fileWriter.close();
	}

	public void getTestName(String traceFilePath) {
		String splitArray[]= traceFilePath.split("/");
		for (String tmpStr : splitArray) {
			if (tmpStr.contains("test")) {
				testName = tmpStr;
				break;
			}
		}
	}

	public static void main(String args[]) throws IOException {
		String traceFilePath = args[0];
		String switchFilePath = args[1];
		String msrFilePath = args[2];
		long stime = 0;
		long etime = 0;

		TreeMap<Float, Float> switchInfo = new TreeMap<>();
		HashMap<Float, ArrayList<String>> msrInfo = new HashMap<Float, ArrayList<String>>();
		HashMap<String, FloatListPair> latencyPairs = new HashMap<String, FloatListPair>();
		HashMap<String, FloatListPair> msrPairs = new HashMap<String, FloatListPair>();
	
		AnalyzerCorrelation analyzer = new AnalyzerCorrelation(switchInfo, msrInfo,latencyPairs, msrPairs);
		if (args.length > 3) {
			analyzer.pTarget = Float.parseFloat(args[3]);
			float threPercent = (float)(1.0 * 5 / Math.pow(10, Float.toString(analyzer.pTarget).length() - 2 + 1));
			analyzer.pTargetLowerBound = analyzer.pTarget - threPercent;
			analyzer.pTargetUpperBound = analyzer.pTarget + threPercent;
			System.out.println("analyzer.pTargetLowerBound = " + analyzer.pTargetLowerBound);
			System.out.println("analyzer.pTargetUpperBound = " + analyzer.pTargetUpperBound);
		}
		
		// Request type
		if (args.length > 4) {
			analyzer.filterType = Integer.parseInt(args[4]);
		}

		// Window length in hours
		if (args.length > 5) {
			analyzer.windowLengthInHours = Float.parseFloat(args[5]);
		}

		// Target window
		if (args.length > 6) {
			analyzer.targetWindow = Float.parseFloat(args[6]);
		}

		analyzer.getTestName(traceFilePath);
		analyzer.readPriorityList();
		analyzer.readRegValueToId(latencyPairs, msrPairs);
		analyzer.readSwitchInfo(switchFilePath, msrFilePath, switchInfo, msrInfo);
		analyzer.readVarResults(traceFilePath, switchInfo, msrInfo, latencyPairs, msrPairs);

		analyzer.generateLatencyPair(latencyPairs);

		//analyzer.startTime = System.nanoTime();
		////First step: find out inflection points using regression approach
		//HashMap<String, ImpactValue> ivMap = analyzer.impactValueAnalysis(latencyPairs);

		////Second step: get proportional relationship
		//HashMap<String, PropRelation> prMap = analyzer.propRelationshipAnalysis(msrPairs);


		//HashMap<String, RangeAnalysis> rMap = analyzer.rangeAnalysis(latencyPairs);

		////Wait for all tasks finished and start the third step
		//analyzer.joinAndStartJaccard(ivMap, prMap, rMap);

		//analyzer.generateOutputImpactValue();
		//analyzer.generateOutputPropRelation();
		//analyzer.generateOutputRangeResults();
		//analyzer.generateOutputJaccardResults();

		//analyzer.generateLatencyResults();

		//analyzer.generateOutputFinals();
		analyzer.endTime = System.nanoTime();
		System.out.printf("Analysis takes %f seconds\n", (analyzer.endTime - analyzer.startTime) / 1e9);
	}
}
