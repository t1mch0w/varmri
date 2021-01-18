import java.io.*;
import java.util.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.charset.*;
import org.apache.commons.math3.stat.StatUtils;

class LatencyTool {
	int filterType = -1;
	float hourWindow = 0.0f;
	int totalWindow = 0;
	static float CPUFREQ = 2.2f;
	ArrayList<ArrayList<Float>> latLists = null;
	String subDir = null;
	int CDFIdx = -1;

	static List<String> eventName = Arrays.asList("RUNNABLE", "WAITING", "HARDIRQ", "SOFTIRQ", "EXITTOUSER", "MEMMIGRATION", "INST", "CYCLE", "KINST", "KCYCLE");

	public LatencyTool(float hourWindow) {
		this.hourWindow = hourWindow;
		latLists = new ArrayList<>();
		totalWindow = (int)(15 / hourWindow);
		for (int i = 0; i < totalWindow + 1; i++) {
			latLists.add(new ArrayList<Float>());
		}
	}

	public String getLatencyInfo(ArrayList<Float> latList, int windowIdx) {
		ArrayList<Float> result = new ArrayList<Float>();
		ArrayList<Float> target = new ArrayList<Float>(Arrays.asList(0.9999f, 0.999f, 0.99f, 0.9f, 0.8f, 0.7f, 0.6f, 0.5f));
		Collections.sort(latList);
		float topTotal = 0.0f;
		int topCount = 100;
		int total = latList.size();
	
		result.add((float)total);

		if (total < 100) {
			result.add(0.0f);
		}
		else {
			for (int i = latList.size() - 1; i >= latList.size() - topCount + 1; i--) {
				topTotal += latList.get(i);
				//System.out.println(latList.get(i));
			}
			result.add(topTotal / topCount);
		}

		for (Float p : target) {
			//result.add(getLatencyByPercent(latList, p));
			if (total == 0) {
				result.add(0.0f);
			}
			else {
				int pos = (int)(p * total);
				result.add(latList.get(pos));
			}
		}

		double[] arr = new double[latList.size()]; 
		int idx = 0;
		for (Float d : latList) {
			arr[idx++] = d;
		}

		float mean = (float)(StatUtils.mean(arr));
		float var = (float)(StatUtils.variance(arr));
		float cov = (float)(Math.sqrt(StatUtils.variance(arr)) / StatUtils.mean(arr));
		result.add(mean);
		result.add(var);
		result.add(cov);

		String windowStr;
		if (windowIdx < latLists.size() - 1) {
			windowStr = String.format(" Window%d", windowIdx);
		}
		else {
			windowStr = " Total";
		}

		StringBuilder res = new StringBuilder(subDir);
		res.append(windowStr);
		for (Float d : result) {
			res.append(" ");
			res.append(d);
		}
		res.append("\n");
		return res.toString();
	}

	public float getLatencyByPercent(ArrayList<Float> latList, float percent) {
		float threPercent = (float)(1.0 * 5 / Math.pow(10, Float.toString(percent).length() - 2 + 1));
		int latListSize = latList.size();
		int spos = (int)(latListSize * (percent - threPercent));
		int epos = (int)(latListSize * (percent + threPercent));
		float latency = 0.0f;
		for (int i = spos; i < epos; i++) {
			latency += latList.get(i);
		}
		latency /= (epos - spos);	
		return latency;
	}

	public void readSwitchInfo(String switchFilePath, String msrFilePath, TreeMap<Float, Float> switchInfo, HashMap<Float, ArrayList<String>> msrInfo) throws FileNotFoundException, IOException {
		File switchFile = new File(switchFilePath);
		FileReader fr = new FileReader(switchFile);
		BufferedReader br = new BufferedReader(fr);
		String line = null;

		ArrayList<Float> stimeList = new ArrayList<>();
		ArrayList<Float> etimeList = new ArrayList<>();

		while((line = br.readLine()) != null) {
			String splitArray[]= line.split(" ");
			int splitLength = splitArray.length;
			float stime = (float)(Long.parseLong(splitArray[splitLength - 2]) / CPUFREQ);
			float etime = (float)(Long.parseLong(splitArray[splitLength - 1]) / CPUFREQ);
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
				String splitArray[]= line.split(" ");
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
		VarResult varResult = null;
		float switchStartTime = switchInfo.firstKey();

		DataInputStream is = new DataInputStream(new BufferedInputStream(new FileInputStream(traceFilePath)));
		while (is.available() > 0) {
			int skip = 0;
			varResult = new VarResult(is);
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

			float switchIdx = -1.0f;
			Float switchTime = switchInfo.floorKey(varResult.results[12]);
			if (switchTime == null) continue;
			if (varResult.results[12] + varResult.latency <= switchInfo.get(switchTime)) {
				switchIdx = switchTime;
			}
			
			// The request is in the middle of a MSR switch 
			if (switchIdx == -1) continue;
			
			// Only focus on the first requests 
			//if (switchIdx - switchStartTime >= 3600 * 1e9 * hourWindow) break;
			// Decide window index and collect latency
			int windowIdx = (int)((switchIdx - switchStartTime) / (3600 * 1e9 * hourWindow));
			//latLists.get(windowIdx).add(varResult.latency);
			//latLists.get(totalWindow).add(varResult.latency);

			//softirq
			//latLists.get(windowIdx).add(varResult.results[3]);
			//latLists.get(totalWindow).add(varResult.results[3]);
			
			//waiting
			latLists.get(windowIdx).add(varResult.results[1]);
			latLists.get(totalWindow).add(varResult.results[1]);
		}
	}

	public void generateLatencyFile() {
		ArrayList<String> results = new ArrayList<>();
		for (int i = 0; i < latLists.size(); i++) {
			ArrayList<Float> latList = latLists.get(i);
			String result = getLatencyInfo(latList, i);
			results.add(result);
			System.out.print(result);
		}
	}

	public void generateCDFFile() {
		ArrayList<Float> targetList = latLists.get(CDFIdx);
		Collections.sort(targetList);
		int total = targetList.size();
		for (int i = 0; i < total; i++) {
			System.out.printf("%f %f\n", 1.0 * i / total, targetList.get(i));
		}
	}

	public static void main(String args[]) throws IOException {
		String traceFilePath = args[0];
		String switchFilePath = args[1];
		String msrFilePath = args[2];
		LatencyTool analyzer = new LatencyTool(Float.parseFloat(args[3]));
		analyzer.subDir = traceFilePath.substring(0, traceFilePath.length() - 12);

		if (args.length > 4) {
			analyzer.filterType = Integer.parseInt(args[4]);
		}

		if (args.length > 5) {
			analyzer.CDFIdx = Integer.parseInt(args[5]);
		}

		TreeMap<Float, Float> switchInfo = new TreeMap<>();
		HashMap<Float, ArrayList<String>> msrInfo = new HashMap<Float, ArrayList<String>>();
		HashMap<String, FloatListPair> latencyPairs = new HashMap<String, FloatListPair>();
		HashMap<String, FloatListPair> msrPairs = new HashMap<String, FloatListPair>();

		analyzer.readSwitchInfo(switchFilePath, msrFilePath, switchInfo, msrInfo);
		analyzer.readVarResults(traceFilePath, switchInfo, msrInfo, latencyPairs, msrPairs);
		if (analyzer.CDFIdx == -1) {
			analyzer.generateLatencyFile();
		}
		else {
			analyzer.generateCDFFile();
		}
	}
}
