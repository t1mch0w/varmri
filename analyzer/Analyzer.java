import java.io.*;
import java.util.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.charset.*;

class Analyzer {
	static int filterType = -1;
	static double CPUFREQ = 2.2;

	static List<String> eventName = Arrays.asList("RUNNABLE", "WAITING", "HARDIRQ", "SOFTIRQ", "EXITTOUSER", "MEMMIGRATION", "INST", "CYCLE", "KINST", "KCYCLE");

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

	public void readVarResults(String traceFilePath, ArrayList<DoublePair> switchInfo, ArrayList<ArrayList<String>> msrInfo, HashMap<String, ArrayList<DoublePair>> latencyPairs, HashMap<String, ArrayList<DoublePair>> msrPairs) throws FileNotFoundException, IOException {
		DataInputStream is = new DataInputStream(new FileInputStream(traceFilePath));
		while (is.available() != 0) {
			int skip = 0;
			VarResult varResult = new VarResult(is);
			for (double tmp_res : varResult.results) {
					if (tmp_res < 0) {
							skip = 1;
							break;
					}
			}
			for (int i = 8; i < 12; i++) {
					if (varResult.results[i] > 10000000) {
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
				DoublePair switchTimeInfo = switchInfo.get(i);
				if (varResult.results[12] > switchTimeInfo.first && varResult.results[12] + varResult.latency < switchTimeInfo.second) {
					switchIdx = i;
					break;
				}
			}
			
			// The request is in the middle of a MSR switch 
			if (switchIdx == -1) continue;

			// Prepare latencyPairs for impact values
			// Basic kernel events and fixed PMUs
			for (int i = 0; i < 8; i++) {
				int eventIdx = i;
				if (eventIdx >= 6 && eventIdx < 8) {
					if (msrInfo.get(switchIdx).get(4).equals("0")) {
							eventIdx += 2;
					}
				}
				latencyPairs.putIfAbsent(eventName.get(eventIdx), new ArrayList<DoublePair>());
				ArrayList<DoublePair> tmpList = latencyPairs.get(eventName.get(eventIdx));
				tmpList.add(new DoublePair(varResult.latency, varResult.results[i]));
			}
			// Add latency and MSR
			for (int i = 8; i < 12; i++) {
				int eventIdx = i - 8;
				latencyPairs.putIfAbsent(msrInfo.get(switchIdx).get(eventIdx), new ArrayList<DoublePair>());
				ArrayList<DoublePair> tmpList = latencyPairs.get(msrInfo.get(switchIdx).get(eventIdx));
				tmpList.add(new DoublePair(varResult.latency, varResult.results[i]));
			}

			// Prepare msrPairs for proportional relationship and jaccard similarity
			for (int i = 8; i < 11; i++) {
				for (int j = i + 1; j < 12; j++) {
					String msrKey = msrInfo.get(switchIdx).get(i - 8) + "-" + msrInfo.get(switchIdx).get(j - 8);
					msrPairs.putIfAbsent(msrKey, new ArrayList<DoublePair>());
					ArrayList<DoublePair> tmpList = msrPairs.get(msrKey);
					tmpList.add(new DoublePair(varResult.results[i], varResult.results[j]));
				}
			}
		}
	}

	public static void main(String args[]) throws IOException {
		String traceFilePath = args[0];
		String switchFilePath = args[1];
		String msrFilePath = args[2];
		ArrayList<DoublePair> switchInfo = new ArrayList<DoublePair>();
		ArrayList<ArrayList<String>> msrInfo = new ArrayList<ArrayList<String>>();
		HashMap<String, ArrayList<DoublePair>> latencyPairs = new HashMap<String, ArrayList<DoublePair>>();
		HashMap<String, ArrayList<DoublePair>> msrPairs = new HashMap<String, ArrayList<DoublePair>>();

		Analyzer analyzer = new Analyzer();
		analyzer.readSwitchInfo(switchFilePath, msrFilePath, switchInfo, msrInfo);
		analyzer.readVarResults(traceFilePath, switchInfo, msrInfo, latencyPairs, msrPairs);
	}
}
