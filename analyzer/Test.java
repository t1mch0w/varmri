import java.io.*;
import java.util.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.charset.*;


class Test{
	public static void main(String args[]) throws IOException {
		FloatListPair pairs = new FloatListPair();	
		File latencyFile = new File(args[0]);
		FileReader fr = new FileReader(latencyFile);
		BufferedReader br = new BufferedReader(fr);
		String line;

		while((line = br.readLine()) != null) {
			String splitArray[]= line.split(" ");
			float first = Float.parseFloat(splitArray[0]);
			float second = Float.parseFloat(splitArray[1]);
			pairs.addData(first, second);
		}

		pairs.sort();
		int total = pairs.size();
		int numOfBins = 1000;
		int numOfPoints = (int)Math.ceil(1.0 * total / numOfBins);

		float lastSlope = 1.0;
		float currSlope = 0.0;
		for (int spos = 0 ; spos < total; spos += numOfPoints) {
			int epos = spos + numOfPoints > total ? total : spos + numOfPoints;
			//System.out.println(spos + " " + epos + " " + numOfPoints);
			float[][] tmpArray = pairs.getFloatArray(0, spos, epos);
			CurveFit cf = new CurveFit();
			cf.fit(tmpArray);
			lastSlope = currSlope;
			currSlope = cf.getSlope();
			System.out.printf("Bin %d ranges [%f:%f], R^2= %f, slope= %f, delta_slope= %f\n", spos/numOfPoints, tmpArray[0][0], tmpArray[tmpArray.length - 1][0], cf.getRSquare(), currSlope, Math.abs(currSlope/lastSlope - 1));
		}
	}
}
