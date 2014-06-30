package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.Observable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.impl.common.FullRunningAverageAndStdDev;
import org.apache.mahout.cf.taste.impl.common.RunningAverageAndStdDev;

public class Profiler extends Observable{

	private static Profiler _profiler=new Profiler();
	private AtomicLong numEvents=new AtomicLong(0);
	private AtomicLong numFails=new AtomicLong(0);
	private Long initialProfilerTime=null;
	private RunningAverageAndStdDev totalAverageTime= new FullRunningAverageAndStdDev();
	private RunningAverageAndStdDev userUpdatingAverageTime= new FullRunningAverageAndStdDev();
	private RunningAverageAndStdDev userAggregatingAverageTime= new FullRunningAverageAndStdDev();
	private RunningAverageAndStdDev itemUpdatingAverageTime= new FullRunningAverageAndStdDev();
	
	private final static Logger LOG = Logger.getLogger(Profiler.class
		      .getName());
	
	

	public static Profiler getInstance(){
		return _profiler;
	}
	private Profiler(){
		
	}
	
	public void reportTimes(long initialTime, long userTime,
			long userAggregation, long itemUpdater) {
		
		
		userUpdatingAverageTime.addDatum(userTime-initialTime);
		userAggregatingAverageTime.addDatum(userAggregation-userTime);
		itemUpdatingAverageTime.addDatum(itemUpdater-userAggregation);
		totalAverageTime.addDatum(itemUpdater-initialTime);
		synchronized (this) {
			if(initialProfilerTime==null){
				initialProfilerTime=initialTime;
			}
		}
			
			
			//System.out.println(numEvents.intValue());
			if(numEvents.incrementAndGet()%100000==0){
				printStats();
			}
		
		
	}

	public void printStats() {
		synchronized (this) {
			if(initialProfilerTime==null){
				initialProfilerTime=System.nanoTime();
			}
		}
		long totalTime=System.nanoTime()-initialProfilerTime;
		StringBuilder message= new StringBuilder();
		message.append(new StringBuilder(new String("NumEvents processed ")).append(numEvents.get()).append('\n'));
		message.append(new StringBuilder(new String("NumEvents failed ")).append(numFails.get()).append('\n'));
		message.append(new StringBuilder(new String("Time has passed (ns)")).append(totalTime).append('\n'));
		message.append(new StringBuilder(new String("Events per time (ms) ")).append(((double)(numEvents.get())/(double)totalTime)*1e6).append('\n'));
		message.append(new StringBuilder(new String("Total avg - stdev ")).append(totalAverageTime.getAverage()).append(new String(" - ")).append(totalAverageTime.getStandardDeviation()).append('\n'));
		message.append(new StringBuilder(new String("UserUpdate avg - stdev ")).append(userUpdatingAverageTime.getAverage()).append(new String(" - ")).append(userUpdatingAverageTime.getStandardDeviation()).append('\n'));
		message.append(new StringBuilder(new String("UserAgg avg - stdev ")).append(userAggregatingAverageTime.getAverage()).append(new String(" - ")).append(userAggregatingAverageTime.getStandardDeviation()).append('\n'));
		message.append(new StringBuilder(new String("ItemUpdate avg - stdev ")).append(itemUpdatingAverageTime.getAverage()).append(new String(" - ")).append(itemUpdatingAverageTime.getStandardDeviation()).append('\n'));
		LOG.info(message.toString());
	}
	public void reportFailure() {
		numFails.incrementAndGet();
		
	}
	public static void reset() {
		
		_profiler= new Profiler();
	}

	

}
