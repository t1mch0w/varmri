import java.io.*;
import java.util.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.charset.*;

class VarResult {
	static int NUM_VAR = 14;
	static float TIME_FREQ = 2.2f;
	int threadId;
	long reqId;
	long event;
	int type;
	short bitmap;

	float runningLength;
	float results[] = new float[NUM_VAR];
	float latency;
	float freq;

	public VarResult() {
	}

	public VarResult(DataInputStream is) throws IOException, EOFException{
		byte[] buffer = new byte[12];
		is.read(buffer);
		ByteBuffer buf = ByteBuffer.wrap(buffer);
		buf.order(ByteOrder.LITTLE_ENDIAN);

		this.threadId = buf.getInt();
		this.reqId = buf.getInt();
		this.event = (int)buf.get();
		this.type = (int)buf.get();
		this.bitmap = buf.getShort();

		int msr_total = 0;
		for (int i = 0; i < NUM_VAR; i++) {
			if (((this.bitmap >> i) & 1) == 1) {
				msr_total++;
			}
		}

		if (msr_total > 0) {
				byte[] msr_buffer = new byte[8 * msr_total];
				is.read(msr_buffer);
				ByteBuffer buf_msr = ByteBuffer.wrap(msr_buffer);
				buf_msr.order(ByteOrder.LITTLE_ENDIAN);

				for (int i = 0; i < NUM_VAR; i++) {
						results[i] = 0;
						if (((this.bitmap >> i) & 1) == 1) {
								results[i] = buf_msr.getLong();
								if ((i >= 0 && i <= 4) || i >= 12) {
										results[i] /= TIME_FREQ;
								}
						}
				}
		}

		//this.latency = (results[13] - results[12]);
		//this.runningLength = latency - results[0] - results[1] - results[2] - results[3] - results[4] - results[5];
		this.latency = results[13] + results[0] + results[1];
		//this.runningLength = latency - results[0] - results[1] - results[2] - results[3] - results[4];
		this.runningLength = results[13] - results[2] - results[3] - results[4] - results[5];
		//this.freq = 1.0 * results[7] / this.runningLength;
		this.freq = (float)(1.0 * results[7] / this.runningLength);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%.3f", latency));
		sb.append(" ");
		sb.append(threadId);
		sb.append(" ");
		sb.append(reqId);
		sb.append(" ");
		//sb.append(event);
		//sb.append(" ");
		sb.append(type);
		sb.append(" ");
		sb.append(String.format("%.3f", runningLength));
		sb.append(" ");
		for (int i = 0; i < NUM_VAR - 2; i++) {
			sb.append(String.format("%.3f", results[i]));
			sb.append(" ");
		}
		sb.append(String.format("%.3f", 1/freq));
		//sb.append(String.format("%.3f ", results[10]));
		//sb.append(String.format("%.3f ", results[11]));
		//sb.append(String.format("%.3f ", results[12] * 2.2));
		//sb.append(String.format("%.3f ", results[13] * 2.2));
		return sb.toString();
	}

	public List<Float> toList() {
		List<Float> res = new ArrayList<>();
		res.add(latency);
		res.add(runningLength);
		for (int i = 0; i < NUM_VAR; i++) {
			res.add(results[i]);
		}
		res.add(freq);
		return res;
	}
}
