package edu.uniandes.privateRecsys.onlineRecommender;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters.RSDataset;
import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.vo.FileEvent;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class PrivateRecommenderParallelTrainer implements Observer {

	private FactorUserItemRepresentation userItemRep;
	private IUserProfileUpdater userUpdater;
	private IUserItemAggregator userAggregator;
	private IItemProfileUpdater itemProfileUpdater;
	
	private ThreadPoolExecutor executor;
	
	private final static Logger LOG = Logger
			.getLogger(PrivateRecommenderParallelTrainer.class.getName());
	private LinkedList<Long> threadIds= new LinkedList<Long>();
	private String[] states;
	private RSDataset dataset;
	private LearningRateStrategy lambda;
	private static int NUM_TRIES_EVALUATION=10;
	
	private AtomicLong numSubmitedTasks=new AtomicLong(0);

	
	private LinkedList<Double> partialEvaluations=new LinkedList<>();
	private UserModelTrainerPredictor predictor;

	public PrivateRecommenderParallelTrainer(FactorUserItemRepresentation userItemRep,
			UserModelTrainerPredictor predictor,
			IUserProfileUpdater userUpdater,
			IUserItemAggregator userAggregator,
			IItemProfileUpdater itemProfileUpdater, RSDataset dataset, LearningRateStrategy lambda) {
		this.userItemRep=userItemRep;
		this.predictor = predictor;
		this.userUpdater = userUpdater;
		this.userAggregator = userAggregator;
		this.itemProfileUpdater = itemProfileUpdater;
		this.lambda=lambda;
	
		this.dataset=dataset;
		LOG.info("Parallel trainer asking for number of available processors");
		int numProcessors = Runtime.getRuntime().availableProcessors();
		//int numProcessors=1;
		LOG.info("Launching pool executor with " + numProcessors + " executors");
		executor = new ThreadPoolExecutor(numProcessors, numProcessors, 0L,
				TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
		executor.prestartAllCoreThreads();
		int initPoolSize=executor.getPoolSize();
		states = new String[initPoolSize];
		LOG.info("Launched pool executor with " + numProcessors + " executors");
		Profiler.reset();
		
		
	

	}

	@Override
	public void update(Observable o, Object arg) {
		FileEvent event = (FileEvent) arg;
		try {
			if(event.convertToTrainEvent()!=null){
				processEvent(event.convertToTrainEvent());
			}
			else if(event.getEventType().equals(FileEvent.CALCULATE_ERROR)){
				calculateModelError();
			}
		} catch (TasteException e) {
			
			e.printStackTrace();
		} catch (InterruptedException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		} catch (PrivateRecsysException e) {
			
			e.printStackTrace();
		}

	}

	private void calculateModelError() throws InterruptedException, IOException, PrivateRecsysException, TasteException {
		int countTries=0;
		while(executor.getQueue().size()!=0&& countTries<PrivateRecommenderParallelTrainer.NUM_TRIES_EVALUATION){
			System.out.println("Waiting finish train model for partial evaluation, queue is"+executor.getQueue().size());
			Thread.sleep(5000);
			countTries++;
		}
		if(countTries<PrivateRecommenderParallelTrainer.NUM_TRIES_EVALUATION){
			double rmse=ModelEvaluator.evaluateModel(new File(dataset.getTestCV()), dataset.getScale(), this.predictor,10);
			partialEvaluations.add(rmse);
		}else{
			partialEvaluations.add(-1.0);
		}
		
	}

	public void processEvent(UserTrainEvent event) throws TasteException,
			InterruptedException {

		while (executor.getQueue().size() > 500000) {
			StringBuilder build = new StringBuilder("[");
			for (int i = 0; i < states.length; i++) {
				build.append(states[i]);
				if (i < states.length - 1)
					build.append(",");
				else
					build.append("]");
			}
			//System.out.println(build.toString());
			Thread.sleep(2000);
			// System.out.println("Waited, queue size is now "+executor.getQueue().size());

		}
		RatingScale rs = userItemRep.getRatingScale();
		if (rs.hasScale(event.getRating())) {
			PrivateRecommenderStrategyRunner runner = new PrivateRecommenderStrategyRunner(
					this.userItemRep, this.userUpdater, this.userAggregator,
					this.itemProfileUpdater, event, this, this.lambda);
			final Future sub = executor.submit(runner);
			numSubmitedTasks.incrementAndGet();
		} else {
			throw new TasteException("Rating " + event.getRating()
					+ " not in rating scale");
		}
		// list.add(sub);
		
		// runner.run();

	}

	public void shutdownThread() {
		executor.shutdown();
	

	}

	public long numExecutedTasks() {
		return executor.getCompletedTaskCount();
	}

	public boolean finishedTraining() {
		return executor.isTerminated();
	}

	public boolean forceShutdown() throws InterruptedException {
		
		LOG.info("Awaiting termination of Parallel training:");
		executor.awaitTermination(15, TimeUnit.MINUTES);
		LOG.info("Finished termination of Parallel training: "+executor.isTerminated());
		if(!executor.isTerminated())
			executor.shutdownNow();
		
		Profiler.getInstance().printStats();
		
		return executor.isTerminated();

	}

	
	synchronized public void updateState(long threadId, UserTrainEvent event, String action)  {
		int pos=-1;
		
		for (int i = 0; i < threadIds.size(); i++) {
			if(threadId==threadIds.get(i)){
				pos=i;
				i=threadIds.size();
			}	
		}
		if(pos==-1){
			pos=threadIds.size();
			threadIds.add(threadId);
		}			
		
		if(pos>=0&&pos<states.length){
			states[pos] = "["+action+"]"+event.getUserId() + "-" + event.getItemId() + "-"
				+ event.getTime();
		}else{
			throw new RuntimeException("Invalid state report whwn training");
		}
		
	}
	
	public LinkedList<Double> getPartialEvaluations(){
		return this.partialEvaluations;
	}

	public long getNumSubmitedTasks() {
		return numSubmitedTasks.get();
	}
	
	

}
