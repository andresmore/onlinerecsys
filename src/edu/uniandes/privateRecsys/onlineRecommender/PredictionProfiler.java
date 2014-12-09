package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.Observable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import org.apache.mahout.cf.taste.impl.common.FullRunningAverageAndStdDev;
import org.apache.mahout.cf.taste.impl.common.RunningAverageAndStdDev;

public class PredictionProfiler extends Observable{

	private static PredictionProfiler _profiler=new PredictionProfiler();
	private AtomicLong numEvents=new AtomicLong(0);
	private AtomicLong numFails=new AtomicLong(0);
	private Long initialProfilerTime=null;
	private RunningAverageAndStdDev totalAverageTime= new FullRunningAverageAndStdDev();
	
	
	private final static Logger LOG = Logger.getLogger(PredictionProfiler.class
		      .getName());
	
	

	public static PredictionProfiler getInstance(){
		return _profiler;
	}
	private PredictionProfiler(){
		
	}
	
	public void reportTimes(long initialTime, long endTime) {
		
		
		
		totalAverageTime.addDatum(endTime-initialTime);
		synchronized (this) {
			if(initialProfilerTime==null){
				initialProfilerTime=initialTime;
			}
		}
			
			
			//System.out.println(numEvents.intValue());
			if(numEvents.incrementAndGet()%2000000==0){
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
		message.append(new String("NumEvents processed ")).append(numEvents.get()).append('\n');
		message.append(new String("NumEvents failed ")).append(numFails.get()).append('\n');
		message.append(new String("Time has passed (ns)")).append(totalTime).append('\n');
		message.append(new String("Events per time (ms) ")).append(((double)(numEvents.get())/(double)totalTime)*1e6).append('\n');
		message.append(new String("Total avg - stdev ")).append(totalAverageTime.getAverage()).append(new String(" - ")).append(totalAverageTime.getStandardDeviation()).append('\n');
	
		LOG.info(message.toString());
	}
	public void reportFailure() {
		numFails.incrementAndGet();
		
	}
	public static void reset() {
		
		_profiler= new PredictionProfiler();
	}

	

}
