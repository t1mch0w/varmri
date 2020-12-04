import java.io.*;
import java.util.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.charset.*;

class Analyzer {
	static double CPUFREQ = 2.2;
	static List<String> eventName = Arrays.asList("RUNNABLE", "WAITING", "HARDIRQ", "SOFTIRQ", "EXITTOUSER", "MEMMIGRATION", "INST", "CYCLE", "KINST", "KCYCLE");

	int filterType = -1;
	//int filterType = 2;
	double pTarget = 0.99;
	double pTargetLowerBound = 0.99 - 0.005;
	double pTargetUpperBound = 0.99 + 0.005;
	HashMap<String, String> regValueToId;
	HashMap<String, Double> mapInflectionPoints;
	HashMap<String, Double> impactValueResults;
	HashMap<String, Double> propRelationResults;
	HashMap<String, Double> jaccardResults;

	ArrayList<DoublePair> switchInfo;
	ArrayList<ArrayList<String>> msrInfo;
	HashMap<String, DoubleListPair> latencyPairs;
	HashMap<String, DoubleListPair> msrPairs;
	
	public Analyzer(ArrayList<DoublePair> switchInfo, ArrayList<ArrayList<String>> msrInfo, HashMap<String, DoubleListPair> latencyPairs, HashMap<String, DoubleListPair> msrPairs) {
		regValueToId = new HashMap<String, String>();
		mapInflectionPoints = new HashMap<String, Double>();
		impactValueResults = new HashMap<String, Double>();
		propRelationResults = new HashMap<String, Double>();
		jaccardResults = new HashMap<String, Double>();
		this.switchInfo = switchInfo;
		this.msrInfo  = msrInfo;
		this.latencyPairs = latencyPairs;
		this.msrPairs = msrPairs;
	}

	public void readRegValueToId(HashMap<String, DoubleListPair> latencyPairs, HashMap<String, DoubleListPair> msrPairs) throws FileNotFoundException, IOException {
		File switchFile = new File("./msrDesc");
		FileReader fr = new FileReader(switchFile);
		BufferedReader br = new BufferedReader(fr);
		String line;

		while((line = br.readLine()) != null) {
			String splitArray[]= line.split(" ");
			regValueToId.put(splitArray[1], splitArray[0]);
		}
		
		for (String eName : eventName) {
			regValueToId.put(eName, eName);
		}

		for (String eName : regValueToId.keySet()) {
			latencyPairs.put(eName, new DoubleListPair());
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
					msrPairs.put(msrKey, new DoubleListPair());
				}
			}
		}
	}

	public void readSwitchInfo(String switchFilePath, String msrFilePath, ArrayList<DoublePair> switchInfo, ArrayList<ArrayList<String>> msrInfo) throws FileNotFoundException, IOException {
		File switchFile = new File(switchFilePath);
		FileReader fr = new FileReader(switchFile);
		BufferedReader br = new BufferedReader(fr);
		String line;

		while((line = br.readLine()) != null) {
			String splitArray[]= line.split(" ");
			int splitLength = splitArray.length;
			double stime = Long.parseLong(splitArray[splitLength - 2]) / CPUFREQ;
			double etime = Long.parseLong(splitArray[splitLength - 1]) / CPUFREQ;
			DoublePair pair = new DoublePair(stime, etime);
			switchInfo.add(pair);
		}

		File msrFile = new File(msrFilePath);
		fr = new FileReader(msrFile);
		br = new BufferedReader(fr);
		ArrayList<String> msrList = new ArrayList<String>();

		while((line = br.readLine()) != null) {
			if (line.contains("Level")) {
				String splitArray[]= line.split(" ");
				int length = splitArray.length;
				String level = splitArray[length - 8];
				String msr = splitArray[length - 6];
				msrList.add(msr);
				if (msrList.size() == 4) {
					msrList.add(level);
					msrInfo.add(msrList);
					msrList = new ArrayList<String>();
				}
			}
		}
	}

	public void readVarResults(String traceFilePath, ArrayList<DoublePair> switchInfo, ArrayList<ArrayList<String>> msrInfo, HashMap<String, DoubleListPair> latencyPairs, HashMap<String, DoubleListPair> msrPairs) throws FileNotFoundException, IOException {
		int count = 0;
		String msrKey = null;
		String firstName = null;
		String secondName = null;
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

			int switchIdx = -1;
			for (int i = 0; i < switchInfo.size(); i++) {
				DoublePair currTimeInfo = switchInfo.get(i);
				DoublePair nextTimeInfo = i < switchInfo.size() - 1 ? switchInfo.get(i + 1) : new DoublePair(Double.MAX_VALUE, Double.MAX_VALUE);
				if (varResult.results[12] >= currTimeInfo.second && varResult.results[12] + varResult.latency <= nextTimeInfo.first) {
					switchIdx = i;
					break;
				}
			}
			
			// The request is in the middle of a MSR switch 
			if (switchIdx == -1) continue;
			
			// The first several requests 
			//if (switchIdx >= 3600 * 2) break;

			// Prepare latencyPairs for impact values
			// Basic kernel events and fixed PMUs
			for (int i = 0; i < 8; i++) {
				int eventIdx = i;
				if (eventIdx >= 6 && eventIdx < 8) {
					if (msrInfo.get(switchIdx).get(4).equals("0")) {
							eventIdx += 2;
					}
				}
				//latencyPairs.putIfAbsent(eventName.get(eventIdx), new DoubleListPair());
				DoubleListPair tmpList = latencyPairs.get(eventName.get(eventIdx));
				tmpList.addData(varResult.latency, varResult.results[i]);
			}

			// Add latency and MSR
			for (int i = 8; i < 12; i++) {
				int eventIdx = i - 8;
				//latencyPairs.putIfAbsent(msrInfo.get(switchIdx).get(eventIdx), new DoubleListPair());
				if (msrInfo.get(switchIdx).get(eventIdx).equals("0x000000")) continue;
				DoubleListPair tmpList = latencyPairs.get(msrInfo.get(switchIdx).get(eventIdx));
				if (tmpList == null) System.out.println(msrInfo.get(switchIdx).get(eventIdx));
				tmpList.addData(varResult.latency, varResult.results[i]);
			}

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
					//msrPairs.putIfAbsent(msrKey, new DoubleListPair());
					DoubleListPair tmpList = msrPairs.get(msrKey);
					tmpList.addData(varResult.results[i], varResult.results[j]);
				}
			}

			// Prepare pairs between CYCLE/INST and MSR
			for (int i = 6; i < 8; i++) {
				for (int j = 8; j < 12; j++) {
					int eventIdx = i;
					if (msrInfo.get(switchIdx).get(4).equals("0")) {
							eventIdx += 2;
					}
					secondName = msrInfo.get(switchIdx).get(j - 8);
					if (secondName.equals("0x000000")) continue;
					firstName = eventName.get(eventIdx);
					msrKey = secondName + "-" + firstName;
					//msrPairs.putIfAbsent(msrKey, new DoubleListPair());
					DoubleListPair tmpList = msrPairs.get(msrKey);
					tmpList.addData(varResult.results[i], varResult.results[j]);
				}
			}
			count++;
		}
		System.out.printf("#request = %d\n", count);
	}

	//The first step
	public HashMap<String, ImpactValue> impactValueAnalysis(HashMap<String, DoubleListPair> latencyPairs) {
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
		FileWriter fileWriter = new FileWriter("impact_value.txt");
		for (String key : impactValueResults.keySet()) {
			fileWriter.write(String.format("[Impact Value] %s %d %f\n", regValueToId.get(key), latencyPairs.get(key).size(), impactValueResults.get(key)));
		}
		fileWriter.close();
	}

	//The second step
	public HashMap<String, PropRelation> propRelationshipAnalysis(HashMap<String, DoubleListPair> msrPairs) {
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
		FileWriter fileWriter = new FileWriter("prop_relation.txt");
		for (String key : propRelationResults.keySet()) {
			String[] keySet = key.split("-");
			fileWriter.write(String.format("[Prop Relation] %s %s %d %f\n", regValueToId.get(keySet[0]), regValueToId.get(keySet[1]), msrPairs.get(key).size(), propRelationResults.get(key)));
		}
		fileWriter.close();
	}

	//The third step
	public HashMap<String, JaccardAnalysis> jaccardAnalysis(HashMap<String, DoubleListPair> msrPairs) {
		//long stime = System.nanoTime();
		HashMap<String, JaccardAnalysis> jaMap = new HashMap<>();

		for (String key : msrPairs.keySet()) {
			if (msrPairs.get(key).size() == 0) continue;
			JaccardAnalysis ja = new JaccardAnalysis(msrPairs.get(key), pTarget);
			ja.start();
			jaMap.put(key, ja);
		}
		//long etime = System.nanoTime();
		//System.out.printf("Jaccard analysis takes %f seconds.\n", (etime - stime) / 1e9);

		return jaMap;
	}

	public void generateOutputJaccardResults() throws IOException {
		FileWriter fileWriter = new FileWriter("jaccard_similarity.txt");
		for (String key : jaccardResults.keySet()) {
			String[] keySet = key.split("-");
			fileWriter.write(String.format("[Jaccard Result] %s %s %d %f\n", regValueToId.get(keySet[0]), regValueToId.get(keySet[1]), msrPairs.get(key).size(), jaccardResults.get(key)));
		}
		fileWriter.close();
	}

	//Join function
	public void join(HashMap<String, ImpactValue> ivMap, HashMap<String, PropRelation> prMap, HashMap<String, JaccardAnalysis> jaMap) {
		try {
			for (String key : ivMap.keySet()){
					ImpactValue iv = ivMap.get(key);
					iv.join();
					impactValueResults.put(key, iv.getResult());
			}

			for (String key : prMap.keySet()) {
					PropRelation pr = prMap.get(key);
					pr.join();
					propRelationResults.put(key, pr.getResult());
			}

			for (String key : jaMap.keySet()) {
					JaccardAnalysis ja = jaMap.get(key);
					ja.join();
					jaccardResults.put(key, ja.getResult());
			}

		} catch (Exception e) {
			System.out.println("Exception caught in analysis().");
		}
	}

	public static void main(String args[]) throws IOException {
		String traceFilePath = args[0];
		String switchFilePath = args[1];
		String msrFilePath = args[2];
		long stime = 0;
		long etime = 0;

		ArrayList<DoublePair> switchInfo = new ArrayList<DoublePair>();
		ArrayList<ArrayList<String>> msrInfo = new ArrayList<ArrayList<String>>();
		HashMap<String, DoubleListPair> latencyPairs = new HashMap<String, DoubleListPair>();
		HashMap<String, DoubleListPair> msrPairs = new HashMap<String, DoubleListPair>();
	
		Analyzer analyzer = new Analyzer(switchInfo, msrInfo,latencyPairs, msrPairs);
		if (args.length > 3) {
			analyzer.pTarget = Double.parseDouble(args[3]);
			double threPercent = 1.0 * 5 / Math.pow(10, Double.toString(analyzer.pTarget).length() - 2 + 1);
			analyzer.pTargetLowerBound = analyzer.pTarget - threPercent;
			analyzer.pTargetUpperBound = analyzer.pTarget + threPercent;
		}

		stime = System.nanoTime();
		analyzer.readRegValueToId(latencyPairs, msrPairs);
		analyzer.readSwitchInfo(switchFilePath, msrFilePath, switchInfo, msrInfo);
		analyzer.readVarResults(traceFilePath, switchInfo, msrInfo, latencyPairs, msrPairs);
		etime = System.nanoTime();
		System.out.printf("Parsing data takes %f seconds.\n", (etime - stime) / 1e9);

		//First step: find out inflection points using regression approach
		HashMap<String, ImpactValue> ivMap = analyzer.impactValueAnalysis(latencyPairs);

		//Second step: get proportional relationship
		HashMap<String, PropRelation> prMap = analyzer.propRelationshipAnalysis(msrPairs);

		//Third step: get jaccard similarity
		HashMap<String, JaccardAnalysis> jaMap = analyzer.jaccardAnalysis(msrPairs);	

		//Wait for all tasks finished
		analyzer.join(ivMap, prMap, jaMap);

		analyzer.generateOutputImpactValue();
		analyzer.generateOutputPropRelation();
		analyzer.generateOutputJaccardResults();
	}
}
