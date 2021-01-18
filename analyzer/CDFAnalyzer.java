import java.util.ArrayList;
import java.util.List;

public class CDFAnalyzer {

    // [][0]: x
    // [][1]: y
    private final float[][] allData;
    private float R2Threshold;
    private int numInitIntervals;
    private final float initR2Threshold;
    private List<Float> turningPointsX;
    private List<Float> turningPointsY;

    // These are LENGTH of interval
    private int curInterval;
    private int initInterval;
    // Two regression class
    private SimpleRegression curReg;
    private SimpleRegression tempReg;
    // starting and ending index
    int start;
    int end;
    // whether the interval have been adjusted
    // in this iteration
    boolean adjusted;
    boolean ascending;

    float turningPointValue;
    float turningPointPos;
    int numOfDataPoints;

    public CDFAnalyzer(float[][] data) {
        // default R2-Threshold: 0.95
        // default number of intervals: 1000
        this(data, 0.95f, 1000);
    }

    public CDFAnalyzer(float[][] data, float R2threshold){
        // default number of intervals: 1000
        this(data, R2threshold, 1000);
    }

    public CDFAnalyzer(float[][] data,int numIntervals){
        // default R2-Threshold: 0.95
        this(data, 0.95f, numIntervals);
    }
    

    public CDFAnalyzer(float[][] data, float R2threshold,int numIntervals) {
        allData = data;
        initR2Threshold = R2threshold;
        R2Threshold = initR2Threshold;
        turningPointsX = new ArrayList<>();
        turningPointsY = new ArrayList<>();
        numInitIntervals = numIntervals;
	turningPointValue = Float.NaN;
	turningPointPos = data.length - 1;
	numOfDataPoints = data.length - 1;
    }

    private void findWithoutAdjust(){
        curReg = new SimpleRegression();
        tempReg = new SimpleRegression();
        // initial interval LENGTH
        initInterval = allData.length / numInitIntervals;
        start = 0;
        end = initInterval - 1;

        for(int ii=1;ii<=numInitIntervals;ii++){
            for (int i = start; i <= end; i++) {
                tempReg.addData(allData[i][1], allData[i][0]);
            }
            if (curReg.getN() == 0){
                if (tempReg.getRSquare() >= R2Threshold) {
                    curReg.append(tempReg);
                }else{
                    recordTurningInterval();
                }
            }else{
                if (curReg.getAppendPartialRSquare(tempReg)>=R2Threshold){
                    curReg.append(tempReg);
                }else{
                    recordTurningInterval();
                    curReg.clear();
                }
            }
            tempReg.clear();
            start = end+1;
            end = start+initInterval-1;
            validateIndex();
        }
    }


    private void recordTurningInterval(){
        recordTurningPoints(start);
        recordTurningPoints(end);
    }


    private void recordTurningPoints(int idx){
        turningPointsX.add(allData[idx][1]);
        turningPointsY.add(allData[idx][0]);
    }

    private void validateIndex(){
        // avoid index out of bound
        end = Math.min(end, allData.length - 1);
        start = Math.min(start,end-1);
    }

    public float getTurningPointAt(int lower,int target){
        findWithoutAdjust();

        float maxInRange = -1;

        for(int i=0;i<turningPointsX.size();i++){
            float point = turningPointsX.get(i);
            float lat = turningPointsY.get(i);
            if (point > lower && point <= target){
		    turningPointValue = lat;
            maxInRange = Math.max(lat,maxInRange);
		    turningPointPos = point;
            }
            else if (Float.isNaN(turningPointValue) && point >= target){
            turningPointValue = lat;
		    turningPointPos = point;
            if (lat != maxInRange){
                // if there isn't one greater than maxInRange, will return
                // the last turning point at the end of for loop.
                break;
            }
            }
        }

		//for (int i = 0; i < allData.length; i++) {
		//	System.out.println(allData[i][0] + " " + allData[i][1]);
		//}
		//System.out.println(turningPointValue);
        return turningPointValue;
    }

    public float getTurningPointPercent() {
	    return turningPointPos / numOfDataPoints;
    }
}
