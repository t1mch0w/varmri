
/**
 * Estimates an ordinary least squares regression model
 * with one independent variable.
 * <p>
 * <code> y = intercept + slope * x  </code></p>

 * <li> The intercept term may be suppressed by passing {@code false} to
 * the {@link #SimpleRegression(boolean)} constructor.  When the
 * {@code hasIntercept} property is false, the model is estimated without a
 * constant term and {@link #getIntercept()} returns {@code 0}.</li>
 * </ul></p>
 *
 */
public class SimpleRegression {


    /** sum of x values */
    private double sumX = 0d;

    /** sum of x square */
    private double sumXX = 0d;

    /** sum of y values */
    private double sumY = 0d;

    /** sum of y square */
    private double sumYY = 0d;

    /** sum of products */
    private double sumXY = 0d;

    /** number of observations */
    private long n = 0;

    /** include an intercept or not */
    private final boolean hasIntercept;
    // ---------------------Public methods--------------------------------------

    /**
     * Create an empty SimpleRegression instance
     */
    public SimpleRegression() {
        this(true);
    }
    /**
    * Create a SimpleRegression instance, specifying whether or not to estimate
    * an intercept.
    *
    * @param includeIntercept whether or not to include an intercept term in
    * the regression model
    */
    public SimpleRegression(boolean includeIntercept) {
        hasIntercept = includeIntercept;
    }

    /**
     * Adds the observation (x,y) to the regression data set.
     * @param x independent variable value
     * @param y dependent variable value
     */
    public void addData(final double x,final double y) {
            sumXX += x * x ;
            sumYY += y * y ;
            sumXY += x * y ;
            sumX += x;
            sumY += y;
            n++;
    }

    /**
     * Adds the observations represented by the elements in data.
     *
     * (data[0][0],data[0][1]) will be the first observation, then
     * (data[1][0],data[1][1]), etc.
     * To replace all data, use clear() before adding the new
     * data.
     *
     * @param data array of observations to be added
     * greater than or equal to 2
     */
    public void addData(final double[][] data) {
        for (double[] datum : data) {
            assert datum.length == 2 : "Err: each observation should have two values";
            addData(datum[0], datum[1]);
        }
    }

    /**
     * Appends data from another regression calculation to this one.
     *
     * @param reg model to append data from
     */
    public void append(SimpleRegression reg) {
        sumXX += reg.sumXX;
        sumYY += reg.sumYY;
        sumXY += reg.sumXY;
        sumX  += reg.sumX;
        sumY  += reg.sumY;
        n  += reg.n;
    }

    /**
     * Removes data from another regression calculation to this one.
     *
     * @param reg model to append data from
     */
    public void remove(SimpleRegression reg) {
        sumXX -= reg.sumXX;
        sumYY -= reg.sumYY;
        sumXY -= reg.sumXY;
        sumX  -= reg.sumX;
        sumY  -= reg.sumY;
        n  -= reg.n;
    }



    /**
     * Returns the <a href="http://www.xycoon.com/coefficient1.htm">
     * coefficient of determination</a>,
     * usually denoted r-square.
     * <p>
     * <strong>Preconditions</strong>: <ul>
     * <li>At least two observations (with at least two different x values)
     * must have been added before invoking this method. If this method is
     * invoked before a model can be estimated, <code>Double,NaN</code> is
     * returned.
     * </li></ul></p>
     *
     * @return r-square
     */
    public double getRSquare() {
        double a = getSlope();
        double b = getIntercept();
        double num = -sumY*sumY-n*a*a*sumXX-2*n*a*b*sumX-n*n*b*b+2*n*a*sumXY+2*n*b*sumY;
        double den = n*sumYY-sumY*sumY;
        return num/den;
    }


    /**
     * Compute the partial R square of the data to append(from another regression calculation to this one),
     * without actually changing the data.
     *
     * @param reg model to append data from
     */
    public double getAppendPartialRSquare(SimpleRegression reg){
        append(reg);
        double a = getSlope();
        double b = getIntercept();
        remove(reg);
        double y_bar = (sumY+reg.sumY)/(n+reg.n);
        double sum_yhat_2 = a*a*reg.sumXX+2*a*b*reg.sumX+reg.n*b*b;
        double sum_y_yhat = a*reg.sumXY+b*reg.sumY;
        double num = reg.n*y_bar*y_bar-2*y_bar*reg.sumY-sum_yhat_2+2*sum_y_yhat;
        double den = reg.sumYY-2*y_bar*reg.sumY+reg.n*y_bar*y_bar;
        return num/den;
    }


    /**
     * Compute the full new R square of the data after append(from another regression calculation to this one),
     * without actually changing the data.
     *
     * @param reg model to append data from
     */
    public double getAppendFullRSquare(SimpleRegression reg){
        append(reg);
        double rSquare = getRSquare();
        remove(reg);
        return rSquare;
    }


    /**
     * Removes the observation (x,y) from the regression data set.
     * <p>
     * Mirrors the addData method.  This method permits the use of
     * SimpleRegression instances in streaming mode where the regression
     * is applied to a sliding "window" of observations, however the caller is
     * responsible for maintaining the set of observations in the window.</p>
     *
     * The method has no effect if there are no points of data (i.e. n=0)
     *
     * @param x independent variable value
     * @param y dependent variable value
     */
    public void removeData(final double x,final double y) {
        if (n > 0) {
            sumXX -= x * x;
            sumYY -= y * y;
            sumXY -= x * y;
            sumX -= x;
            sumY -= y;
            n--;
        }
    }


    /**
     * Removes observations represented by the elements in <code>data</code>.
      * <p>
     * If the array is larger than the current n, only the first n elements are
     * processed.  This method permits the use of SimpleRegression instances in
     * streaming mode where the regression is applied to a sliding "window" of
     * observations, however the caller is responsible for maintaining the set
     * of observations in the window.</p>
     * <p>
     * To remove all data, use <code>clear()</code>.</p>
     *
     * @param data array of observations to be removed
     */
    public void removeData(double[][] data) {
        for (int i = 0; i < data.length && n > 0; i++) {
            assert data[i].length == 2 :"Err: each observation should have two values";
            removeData(data[i][0], data[i][1]);
        }
    }

    /**
     * Clears all data from the model.
     */
    public void clear() {
        sumX = 0d;
        sumXX = 0d;
        sumY = 0d;
        sumYY = 0d;
        sumXY = 0d;
        n = 0;
    }

    /**
     * Returns the number of observations that have been added to the model.
     *
     * @return n number of observations that have been added.
     */
    public long getN() {
        return n;
    }

    /**
     * Returns the "predicted" <code>y</code> value associated with the
     * supplied <code>x</code> value,  based on the data that has been
     * added to the model when this method is activated.
     * <p>
     * <code> predict(x) = intercept + slope * x </code></p>
     * <p>
     * <strong>Preconditions</strong>: <ul>
     * <li>At least two observations (with at least two different x values)
     * must have been added before invoking this method. If this method is
     * invoked before a model can be estimated, <code>Double,NaN</code> is
     * returned.
     * </li></ul></p>
     *
     * @param x input <code>x</code> value
     * @return predicted <code>y</code> value
     */
    public double predict(final double x) {
        if (hasIntercept) {
            return getIntercept() + getSlope() * x;
        }else{
            return getSlope() * x;
        }
    }

    /**
     * Returns the intercept of the estimated regression line, if
     *
     * @return the intercept of the regression line if the model includes an
     * intercept; 0 otherwise
     */
    public double getIntercept() {
        if (hasIntercept){
            return (sumXX*sumY-sumX*sumXY)/(n*sumXX-sumX*sumX);
        }else{
            return 0d;
        }
    }

    /**
     * Returns true if the model includes an intercept term.
     *
     * @return true if the regression includes an intercept; false otherwise
     * @see #SimpleRegression(boolean)
     */
    public boolean hasIntercept() {
        return hasIntercept;
    }

    /**
    * Returns the slope of the estimated regression line.
    * <p>
    * The least squares estimate of the slope is computed using the
    * <a href="http://www.xycoon.com/estimation4.htm">normal equations</a>.
    * The slope is sometimes denoted b1.</p>
    * <p>
    * <strong>Preconditions</strong>: <ul>
    * <li>At least two observations (with at least two different x values)
    * must have been added before invoking this method. If this method is
    * invoked before a model can be estimated, <code>Double.NaN</code> is
    * returned.
    * </li></ul></p>
    *
    * @return the slope of the regression line
    */
    public double getSlope() {
        if (n < 2) {
            return Double.NaN; //not enough data
        }
        if (hasIntercept){
            return (n*sumXY-sumY*sumX)/(n*sumXX-sumX*sumX);
        }
        return sumXY / sumXX;
    }



}
