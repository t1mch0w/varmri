import java.io.*;
import java.util.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.charset.*;
import org.apache.commons.math3.stat.StatUtils;

class LatencyTool {
	int filterType = -1;
	double hourWindow = 0.0;
	int totalWindow = 0;
	static double CPUFREQ = 2.2;
	ArrayList<ArrayList<Double>> latLists = null;
	String subDir = null;

	static List<String> eventName = Arrays.asList("RUNNABLE", "WAITING", "HARDIRQ", "SOFTIRQ", "EXITTOUSER", "MEMMIGRATION", "INST", "CYCLE", "KINST", "KCYCLE");

	public LatencyTool(double hourWindow) {
		this.hourWindow = hourWindow;
		latLists = new ArrayList<>();
		totalWindow = (int)(15 / hourWindow);
		for (int i = 0; i < totalWindow + 1; i++) {
			latLists.add(new ArrayList<Double>());
		}
	}

	public String getLatencyInfo(ArrayList<Double> latList, int windowIdx) {
		ArrayList<Double> result = new ArrayList<Double>();
		ArrayList<Double> target = new ArrayList<Double>(Arrays.asList(0.9999, 0.999, 0.99, 0.5));
		Collections.sort(latList);
		double topTotal = 0;
		int topCount = 100;
	
		result.add((double)latList.size());

		if (latList.size() < 100) {
			result.add(0.0);
		}
		else {
			for (int i = latList.size() - 1; i >= latList.size() - topCount + 1; i--) {
				topTotal += latList.get(i);
				//System.out.println(latList.get(i));
			}
			result.add(topTotal / topCount);
		}

		for (Double p : target) {
			result.add(getLatencyByPercent(latList, p));
		}

        double[] arr = new double[latList.size()]; 
		int idx = 0;
		for (Double d : latList) {
			arr[idx++] = d;
		}

		Double mean = StatUtils.mean(arr);
		Double var = StatUtils.variance(arr);
		Double cov = Math.sqrt(StatUtils.variance(arr)) / StatUtils.mean(arr);
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
		for (Double d : result) {
			res.append(" ");
			res.append(d);
		}
		res.append("\n");
		return res.toString();
	}

	public double getLatencyByPercent(ArrayList<Double> latList, double percent) {
		double threPercent = 1.0 * 5 / Math.pow(10, Double.toString(percent).length() - 2 + 1);
		int latListSize = latList.size();
		int spos = (int)(latListSize * (percent - threPercent));
		int epos = (int)(latListSize * (percent + threPercent));
		double latency = 0;
		for (int i = spos; i < epos; i++) {
			latency += latList.get(i);
		}
		latency /= (epos - spos);	
		return latency;
	}

	public void readSwitchInfo(String switchFilePath, String msrFilePath, TreeMap<Double, Double> switchInfo, HashMap<Double, ArrayList<String>> msrInfo) throws FileNotFoundException, IOException {
		File switchFile = new File(switchFilePath);
		FileReader fr = new FileReader(switchFile);
		BufferedReader br = new BufferedReader(fr);
		String line = null;

		ArrayList<Double> stimeList = new ArrayList<>();
		ArrayList<Double> etimeList = new ArrayList<>();

		while((line = br.readLine()) != null) {
			String splitArray[]= line.split(" ");
			int splitLength = splitArray.length;
			double stime = Long.parseLong(splitArray[splitLength - 2]) / CPUFREQ;
			double etime = Long.parseLong(splitArray[splitLength - 1]) / CPUFREQ;
			stimeList.add(stime);
			etimeList.add(etime);
		}

		for (int i = 1; i < stimeList.size() - 1; i++) {
			switchInfo.put(etimeList.get(i - 1), stimeList.get(i));	
		}
		switchInfo.put(etimeList.get(etimeList.size() - 1), Double.MAX_VALUE);

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

	public void readVarResults(String traceFilePath, TreeMap<Double, Double> switchInfo, HashMap<Double, ArrayList<String>> msrInfo, HashMap<String, DoubleListPair> latencyPairs, HashMap<String, DoubleListPair> msrPairs) throws FileNotFoundException, IOException {
		int count = 0;
		VarResult varResult = null;
		double switchStartTime = switchInfo.firstKey();

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

			double switchIdx = -1;
			Double switchTime = switchInfo.floorKey(varResult.results[12]);
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
			latLists.get(windowIdx).add(varResult.latency);
			latLists.get(totalWindow).add(varResult.latency);
		}
	}

	public void generateLatencyFile() {
		ArrayList<String> results = new ArrayList<>();
		for (int i = 0; i < latLists.size(); i++) {
			ArrayList<Double> latList = latLists.get(i);
			String result = getLatencyInfo(latList, i);
			results.add(result);
			System.out.print(result);
		}
	}

	public static void main(String args[]) throws IOException {
		String traceFilePath = args[0];
		String switchFilePath = args[1];
		String msrFilePath = args[2];
		LatencyTool analyzer = new LatencyTool(Double.parseDouble(args[3]));
		analyzer.subDir = traceFilePath.substring(0, traceFilePath.length() - 12);
		if (args.length > 4) {
			analyzer.filterType = Integer.parseInt(args[4]);
		}

		TreeMap<Double, Double> switchInfo = new TreeMap<>();
		HashMap<Double, ArrayList<String>> msrInfo = new HashMap<Double, ArrayList<String>>();
		HashMap<String, DoubleListPair> latencyPairs = new HashMap<String, DoubleListPair>();
		HashMap<String, DoubleListPair> msrPairs = new HashMap<String, DoubleListPair>();

		analyzer.readSwitchInfo(switchFilePath, msrFilePath, switchInfo, msrInfo);
		analyzer.readVarResults(traceFilePath, switchInfo, msrInfo, latencyPairs, msrPairs);
		analyzer.generateLatencyFile();
	}
}
