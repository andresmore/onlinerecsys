package edu.uniandes.privateRecsys.onlineRecommender;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.vo.FileEvent;
import edu.uniandes.privateRecsys.onlineRecommender.vo.IRPrecisionError;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class TopNPredictorParallelCalculator {


	private Set<Long> users;
	private FactorUserItemRepresentation userItemRep;
	private AtomicLong numSubmitedTasks=new AtomicLong(0);
	private AtomicLong numExecutedTasks=new AtomicLong(0);
	private double[] precisions ;
	private double[] precisionsAt5;
	private double[] precisionsAt10 ;
	private double[] aucs;
	private int[] posFirstHit ;
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
	private HashMap<Long, Set<Long>> preloadTestFile() throws TasteException {
		final HashMap<Long, Set<Long>> testPreferences= new HashMap<Long, Set<Long>>();
		
		
		try {
			FileEventCreator fileEv= new FileEventCreator(new File(testFile),-1,-1);
			fileEv.addObserver(new Observer() {
				
				@Override
				public void update(Observable o, Object arg) {
					FileEvent ev=(FileEvent)arg;
					UserTrainEvent event=ev.convertToTrainEvent();
					testPreferences.putIfAbsent(event.getUserId(), new HashSet<Long>());
					testPreferences.get(event.getUserId()).add(event.getItemId());
					
					
				}
			});
			fileEv.startEvents();
		} catch (IOException | PrivateRecsysException e) {
			
			e.printStackTrace();
			throw new TasteException(e);
		}
		return testPreferences;
	}

	public IRPrecisionError calculateIRMetrics(int minNumTrains, int listSize, boolean preloadTest) throws TasteException {
		HashMap<Long, Set<Long>> preloadedTest=null;
		
		if(preloadTest) {
			LOG.info("Preloading test file");
			preloadedTest=preloadTestFile();
			LOG.info("Test file loaded");
		}
		precisions = new double[users.size()];
		precisionsAt5= new double[users.size()];;
		precisionsAt10 = new double[users.size()];;
		aucs= new double[users.size()];
		posFirstHit = new int[users.size()];;
		
		int idx=0;
		for (Long userID : users) {
			
			TopNPredictorParallelRunner run=new TopNPredictorParallelRunner(this,userID,userItemRep,this.topNrecommender, this.testFile, minNumTrains, listSize, preloadedTest,idx);
			while(executor.getQueue().size()>400000){
				System.out.println("Waiting on queue, size is "+executor.getQueue().size());
				try {
					Thread.sleep(50);
					System.out.println("Waited, queue size is now "+executor.getQueue().size());
				} catch (InterruptedException e) {
					LOG.log(Level.SEVERE, "Interrupted exception", e);
				}
				
			}
			executor.submit(run);
			numSubmitedTasks.incrementAndGet();
			idx++;

			
	
		}
		
		try {
			executor.shutdown();
			LOG.info("Awaiting termination of TopNIR eval");
			executor.awaitTermination(15, TimeUnit.MINUTES);
			LOG.info("Finished termination of TopNIR eval terminated: "+executor.isTerminated());
			LOG.info("Excecuted "+this.numSubmitedTasks+" evs");
			if(!executor.isTerminated())
				executor.shutdownNow();
		} catch (InterruptedException e) {
			LOG.log(Level.SEVERE, "TopNIR eval not concluded", e);
			throw new TasteException(
					"Evaluation failed -  numEvaluated not as  numSubmitedTasks: "
							+ this.numSubmitedTasks+"!="+this.numExecutedTasks);
		}
		
		

	return new IRPrecisionError(precisions, precisionsAt5, precisionsAt10,
			aucs,posFirstHit);
	
	}

	public void addNewPrecision(double averagePR, int idx) {
		precisions[idx]=averagePR;
		
	}

	public void addNewPrecisionAt5(double pAt5, int idx) {
		precisionsAt5[idx]=(pAt5);
	}

	public void addNewPrecisionAt10(double pAt10,int idx) {
		precisionsAt10[idx]=(pAt10);
		
	}

	public void addNewAUC(double trapezoidSum, int idx) {
		aucs[idx]=(trapezoidSum);
	}

	public void incrementNumExecutedTasks() {
		long num=this.numExecutedTasks.incrementAndGet();
		
		if(num%10000==0) {
			LOG.info("Excecuted "+num+" evs");
		}
	}

	public void addFirstHit(int posFirstHit, int idx) {
		this.posFirstHit[idx]=(posFirstHit);
		
	}

}
