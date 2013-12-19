package edu.uniandes.privateRecsys.onlineRecommender.metadata;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import com.clearspring.analytics.stream.frequency.CountMinSketch;

/**
 * Implementation of the sliding window sketch data structure as described in:<br>
 * The eternal sunshine of the sketch data structure <br>
 * Xenofontas Dimitropoulos, Marc Stoecklin, Paul Hurley, Andreas Kind <br> 
 * http://www.sciencedirect.com/science/article/pii/S1389128608002673
 * @author Andres M
 *
 */
public class SlidingWindowCountMinSketch {
	
	
	private Queue<ResetableCountMinSketch> queue;

	private int segmentLength;
	
	public SlidingWindowCountMinSketch(int depth, int width, int seed, int m, int l, long [] hashA){
		
		
		queue= new LinkedList<ResetableCountMinSketch>();
		for (int i = 0; i < m; i++) {
			ResetableCountMinSketch sketch= new ResetableCountMinSketch(depth, width, hashA);
			queue.add(sketch);
			
			
		}
	
		this.segmentLength=(int) Math.floor(l/m);
		
	}
	
	public int estimateCount(String datum){
		
		int count=0;
		for (ResetableCountMinSketch sketch : queue) {
			count+=sketch.estimateCount(datum);
		}
		return count;
	}
	
	public int estimateCount(long datum){
		
		int count=0;
		for (ResetableCountMinSketch sketch : queue) {
			count+=sketch.estimateCount(datum);
		}
		return count;
	}
	
	public void add(String datum){
		
		queue.peek().add(datum, 1);
	}
	public void updateCounter(int counter){
		
		if(counter%segmentLength==0){
			ResetableCountMinSketch skt=queue.poll();
			queue.add(skt);
			queue.peek().resetCounts();
		}
	}
	
	public void add( long datum){
		
		
		queue.peek().add(datum, 1);
	}
	

}
