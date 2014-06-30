package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.mahout.cf.taste.impl.common.FullRunningAverage;
import org.apache.mahout.cf.taste.impl.common.RunningAverage;

import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class RMSE_Evaluator implements Observer {

	
	
	private static final int LIMIT_SIZE_QUEUE = 500000;
	private RunningAverage rmseAverage= new FullRunningAverage();
	private RunningAverage maeAverage= new FullRunningAverage();
	private RunningAverage averagePrediction= new FullRunningAverage();
	private ThreadPoolExecutor executor ;
	
	
	private AtomicLong numEvals=new AtomicLong(0);
	
	private AtomicLong  randEvals=new AtomicLong(0);

	private int minTrains;

	private AtomicLong  hybridEvals=new AtomicLong(0);

	private AtomicLong numSubmitedTasks=new AtomicLong(0);

	private UserModelTrainerPredictor predictor;
	private RatingScale scale;
	

	private final static Logger LOG = Logger.getLogger(RMSE_Evaluator.class
		      .getName());
	public long getNumHybridEvals() {
		return hybridEvals.get();
	}

	public RMSE_Evaluator(UserModelTrainerPredictor predictor,RatingScale scale, int minTrains) {
		this.predictor=predictor;
		this.scale=scale;
		this.minTrains=minTrains;
		LOG.info("RMSE evaluator asking for processors available");
		int numProcessors=Runtime.getRuntime().availableProcessors();
		LOG.info("Launching pool executor with "+numProcessors+" executors");
		executor = new ThreadPoolExecutor(numProcessors, numProcessors, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
		executor.prestartAllCoreThreads();
		
	}

	@Override
	public void update(Observable o, Object arg) {

		UserTrainEvent event = (UserTrainEvent) arg;
		PredictionEvaluationRunnable run= new PredictionEvaluationRunnable(event,this.predictor,this.minTrains,this);
		if(executor.getQueue().size()>LIMIT_SIZE_QUEUE){
			while (executor.getQueue().size() > LIMIT_SIZE_QUEUE/2) {
				try {
					//System.out.println("wating for RMSE eval "+executor.getQueue().size());
					Thread.sleep(500);
					//System.out.println("waited for RMSE eval "+executor.getQueue().size());
				} catch (InterruptedException e) {
					LOG.log(Level.SEVERE, "Interrupted exception", e);
				}
			}
			
		}
		executor.submit(run);
		numSubmitedTasks.incrementAndGet();
		

	}
	public long getNumEvals() {
		return numEvals.get();
	}

	public long getRandEvals() {
		return randEvals.get();
	}

	public void resetRMSE(){
		rmseAverage= new FullRunningAverage();
		maeAverage= new FullRunningAverage();
		numEvals=new AtomicLong();
		randEvals=new AtomicLong();
		this.hybridEvals=new AtomicLong();
		numSubmitedTasks=new AtomicLong();
	}

	public double getRMSE() {
		
		return Math.sqrt(rmseAverage.getAverage());
	}
	
	public double getMAE(){
		return maeAverage.getAverage();
	}

	public void shutdownThread() {
		executor.shutdown();
		
	}
	
	public boolean finishedRMSECalculation() {
		return executor.isTerminated();
		
	}

	public void notifyPrediction(UserTrainEvent event, Prediction prediction) {
		
		
		double predictionValue=0;
		if(Double.isNaN(prediction.getPredictionValue())){
		  System.out.println("Error!!!");
		}
		if (prediction.isNoPrediction()) {
			
			/*int randIndex = Math.abs(RandomUtils
					.nextInt(this.scale.getScale().length));
			predictionValue = Double.parseDouble(this.scale.getScale()[randIndex]);
			*/
			randEvals.incrementAndGet();

		}
		else {
			predictionValue=prediction.getPredictionValue();
			if(prediction.isHybrid())
				this.hybridEvals.incrementAndGet();
			
			
			double[] scaleAsValues = this.scale.scaleAsValues();
			if (predictionValue < scaleAsValues[0])
				predictionValue = scaleAsValues[0];
			if (predictionValue > scaleAsValues[scaleAsValues.length-1])
				predictionValue = scaleAsValues[scaleAsValues.length-1];

			averagePrediction.addDatum(predictionValue);
			float diff = (float) (Double.parseDouble(event.getRating()) - predictionValue);
			rmseAverage.addDatum(diff * diff);
			maeAverage.addDatum(Math.abs(diff));
			numEvals.incrementAndGet();
		}
		
		
		
		
	}

	public long numExecutedTasks() {
		return executor.getCompletedTaskCount();
	}

	public boolean finishedTraining() {
		return executor.isTerminated();
	}

	public boolean forceShutdown() throws InterruptedException {
		LOG.info("Awaiting termination of RMSE eval");
		executor.awaitTermination(15, TimeUnit.MINUTES);
		LOG.info("Finished termination of RMSE eval terminated: "+executor.isTerminated());
		if(!executor.isTerminated())
			executor.shutdownNow();
		
		return executor.isTerminated();

	}
	
	public long getNumSubmitedTasks() {
		return numSubmitedTasks.get();
	}

}
