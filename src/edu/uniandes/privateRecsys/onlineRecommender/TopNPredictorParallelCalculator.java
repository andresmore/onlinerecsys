package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.vo.IRPrecisionError;

public class TopNPredictorParallelCalculator {


	private Set<Long> users;
	private FactorUserItemRepresentation userItemRep;
	private AtomicLong numSubmitedTasks=new AtomicLong(0);
	private AtomicLong numExecutedTasks=new AtomicLong(0);
	private ConcurrentLinkedQueue <Double> precisions = new ConcurrentLinkedQueue <Double>();
	private ConcurrentLinkedQueue <Double> precisionsAt5 = new ConcurrentLinkedQueue <Double>();
	private ConcurrentLinkedQueue <Double> precisionsAt10 = new ConcurrentLinkedQueue <Double>();
	private ConcurrentLinkedQueue <Double> aucs = new ConcurrentLinkedQueue <Double>();
	private final static Logger LOG = Logger
			.getLogger(TopNPredictorParallelCalculator.class.getName());
	
	private ThreadPoolExecutor executor;
	private TopNRecommender topNrecommender;
	private String testFile;
	public TopNPredictorParallelCalculator(
			
			Set<Long> users, FactorUserItemRepresentation userItemRep, TopNRecommender topNRecommender, String testFile) {

	
		this.users=users;
		this.userItemRep=userItemRep;
		this.topNrecommender=topNRecommender;
		this.testFile=testFile;

		
		LOG.info("Parallel trainer asking for number of available processors");
		int numProcessors = Runtime.getRuntime().availableProcessors();
		LOG.info("Launching pool executor with " + numProcessors + " executors");
		executor = new ThreadPoolExecutor(numProcessors, numProcessors, 0L,
				TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
		executor.prestartAllCoreThreads();
		LOG.info("Launched pool executor with " + numProcessors + " executors");
		Profiler.reset();
		
		
	}

	public IRPrecisionError calculateIRMetrics() throws TasteException {
		
		
		for (Long userID : users) {
			
			TopNPredictorParallelRunner run=new TopNPredictorParallelRunner(this,userID,userItemRep,this.topNrecommender, this.testFile);
			while(executor.getQueue().size()>400000){
				//System.out.println("Waiting on queue, size is "+executor.getQueue().size());
				try {
					Thread.sleep(50);
					System.out.println("Waited, queue size is now "+executor.getQueue().size());
				} catch (InterruptedException e) {
					LOG.log(Level.SEVERE, "Interrupted exception", e);
				}
				
			}
			executor.submit(run);
			numSubmitedTasks.incrementAndGet();
		

			
	
		}
		
		try {
			executor.shutdown();
			LOG.info("Awaiting termination of TopNIR eval");
			executor.awaitTermination(15, TimeUnit.MINUTES);
			LOG.info("Finished termination of TopNIR eval terminated: "+executor.isTerminated());
			if(!executor.isTerminated())
				executor.shutdownNow();
		} catch (InterruptedException e) {
			LOG.log(Level.SEVERE, "TopNIR eval not concluded", e);
			throw new TasteException(
					"Evaluation failed -  numEvaluated not as  numSubmitedTasks: "
							+ this.numSubmitedTasks+"!="+this.numExecutedTasks);
		}
		
		

	return new IRPrecisionError(precisions, precisionsAt5, precisionsAt10,
			aucs);
	
	}

	public void addNewPrecision(double averagePR) {
		precisions.add(averagePR);
		
	}

	public void addNewPrecisionAt5(Double pAt5) {
		precisionsAt5.add(pAt5);
	}

	public void addNewPrecisionAt10(Double pAt10) {
		precisionsAt10.add(pAt10);
		
	}

	public void addNewAUC(double trapezoidSum) {
		aucs.add(trapezoidSum);
	}

	public void incrementNumExecutedTasks() {
		this.numExecutedTasks.incrementAndGet();
	}

}
