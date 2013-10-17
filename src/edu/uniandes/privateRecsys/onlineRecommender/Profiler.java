package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.Observable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.impl.common.FullRunningAverage;
import org.apache.mahout.cf.taste.impl.common.FullRunningAverageAndStdDev;
import org.apache.mahout.cf.taste.impl.common.RunningAverage;
import org.apache.mahout.cf.taste.impl.common.RunningAverageAndStdDev;
import org.mortbay.log.Log;

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
		StringBuffer message= new StringBuffer();
		message.append("NumEvents processed "+numEvents.get()+'\n');
		message.append("NumEvents failed "+numFails.get()+'\n');
		message.append("Time has passed (ns)"+totalTime+'\n');
		message.append("Events per time (ms) "+((double)(numEvents.get())/(double)totalTime)*1e6 +'\n');
		message.append("Total avg - stdev "+totalAverageTime.getAverage()+" - "+totalAverageTime.getStandardDeviation()+'\n');
		message.append("UserUpdate avg - stdev "+userUpdatingAverageTime.getAverage()+" - "+userUpdatingAverageTime.getStandardDeviation()+'\n');
		message.append("UserAgg avg - stdev "+userAggregatingAverageTime.getAverage()+" - "+userAggregatingAverageTime.getStandardDeviation()+'\n');
		message.append("ItemUpdate avg - stdev "+itemUpdatingAverageTime.getAverage()+" - "+itemUpdatingAverageTime.getStandardDeviation()+'\n');
		LOG.info(message.toString());
	}
	public void reportFailure() {
		numFails.incrementAndGet();
		
	}
	public static void reset() {
		
		_profiler= new Profiler();
	}

	

}
