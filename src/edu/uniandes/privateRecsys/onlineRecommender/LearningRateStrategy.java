package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Class that represents a learning rate strategy in the form \gamma_{t}=\gamma_{0}(1 + alpha \gamma_{0} t)^{-C}
 * @author Andres M
 *
 */
public class LearningRateStrategy {

	/**
	 * Constant for the learning rate	
	 */
	public final static int C=1;
	/**
	 * alpha parameter
	 */
	private double alpha;
	
	/**
	 * \gamma_{0}
	 */
	private double initialGamma;
	/**
	 * Has a constant learning rate strategy
	 */
	private boolean hasConstantRate=false;
	/**
	 * Constant learning rate, if any
	 */
	private double learningRate;
	
	/**
	 * Currrent timeslot
	 */
	private AtomicInteger currentT= new AtomicInteger(0);
	
	/**
	 * Last seen time
	 */
	private long lastTime=0;
	
	
	private final static Logger LOG = Logger.getLogger(LearningRateStrategy.class
		      .getName());
	
	


	private LearningRateStrategy(double alpha, double initialGamma)  {

		this.initialGamma=initialGamma;
		this.alpha=alpha;
		hasConstantRate=false;
	}

	
	
	private LearningRateStrategy(double constantLearningRate) {

		this.hasConstantRate=true;
		this.learningRate=constantLearningRate;
		
	}
	
	public double getGammaForTime(long time){
    	
    	if(!this.hasConstantRate){
    		double timeSlot=getTimeslotForTime(time);
    		return getGammaFromK(timeSlot);
    	}	
    	else
    		return this.learningRate;
    }



	public double getGammaFromK(double k) {
		if(!this.hasConstantRate){
			return initialGamma*Math.pow(1+alpha*initialGamma*k, -C);
		}
		else
    		return this.learningRate;
	}
    
	
	
	
	private synchronized double getTimeslotForTime(long time) {
		
		if(lastTime==0)
			lastTime=time;
		else if(lastTime<time){
			lastTime=time;
			return this.currentT.incrementAndGet();
		}
		
		return currentT.get();
	}



	public static LearningRateStrategy createDecreasingRate( double alpha,double initialGamma){

		return new LearningRateStrategy(alpha,initialGamma);
	}

	public static LearningRateStrategy createWithConstantRate(double constantLearningRate) {
		
		return new LearningRateStrategy(constantLearningRate);
	}



	public double getAlpha() {
		return alpha;
	}



	public double getInitialGamma() {
		return initialGamma;
	}



	public boolean isHasConstantRate() {
		return hasConstantRate;
	}



	public double getLearningRate() {
		return learningRate;
	}



	public AtomicInteger getCurrentT() {
		return currentT;
	}



	public long getLastTime() {
		return lastTime;
	}
	
	@Override
	public String toString() {
		if(this.hasConstantRate)
			return "fixed"+this.learningRate;
		else
			return "alpha:"+this.alpha+" initGamma:"+initialGamma;
	}

}
