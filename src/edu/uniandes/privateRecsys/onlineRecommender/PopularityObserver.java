package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.PriorityQueue;

import edu.uniandes.privateRecsys.onlineRecommender.vo.FileEvent;
import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class PopularityObserver implements Observer {
	
	private static final int QUEUE_SIZE = 20;

	private static int NUM_HOURS=6;
	
	private HashMap<Long, int[]> countsPerHour= new HashMap<>();
	private long lastPopularityTimeCheck=-1;
	private PriorityQueue<Prediction> cachedQueue= new PriorityQueue<>();
	
	
	
	@Override
	public void update(Observable o, Object arg) {
		FileEvent event = (FileEvent) arg;
		try {
			if(event.convertToTrainEvent()!=null){
				processEvent(event.convertToTrainEvent());
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}

	private void processEvent(UserTrainEvent event) {
		long itemId=event.getItemId();
		if (itemId != 0 && itemId != -1) {
			long timestamp = event.getTimestamp();
			checkTimeShift(timestamp);
			updateItemCount(itemId);
		}
		
	}

	private void updateItemCount(long itemId) {
		int[]arr=this.countsPerHour.get(itemId);
		if(arr==null)
			arr=new int[NUM_HOURS];
		
		arr[0]=arr[0]+1;
		countsPerHour.put(itemId,arr);
		
	}

	synchronized private void checkTimeShift(long timestamp) {
		if(this.lastPopularityTimeCheck==-1)
			this.lastPopularityTimeCheck=timestamp;
		else if(timestamp-this.lastPopularityTimeCheck>3600000){
			this.lastPopularityTimeCheck=timestamp;
			shiftCountPerHourArrays();
			updateCachedCounts();
		}
		
		
	}

	private void updateCachedCounts() {
		synchronized (cachedQueue) {
			HashSet<Long> toRemove= new HashSet<Long>();
			this.cachedQueue.clear();
			for (Long key : countsPerHour.keySet()) {
				int[] arr2=countsPerHour.get(key);
				int prediction = getSumFromVector(arr2);
				if(prediction==0)
					toRemove.add(key);
				else{
					this.cachedQueue.add(Prediction.createNormalPrediction(-1, key, prediction));
					if(this.cachedQueue.size()>QUEUE_SIZE)
						this.cachedQueue.poll();
				}
			}
			for (Long key : toRemove) {
				this.countsPerHour.remove(key);
			}
			
		}
		
		
	}

	private int getSumFromVector(int[] arr2) {
		int prediction=0;
		for (int count : arr2) {
			prediction+=count;
		}
		return prediction;
	}

	private void shiftCountPerHourArrays() {
		for (Long key : countsPerHour.keySet()) {
			int[] arr2=countsPerHour.get(key);
			shiftArray(arr2);
			countsPerHour.put(key,arr2);
		}
	}

	private void shiftArray(int[] arr2) {
		for (int i = arr2.length-1; i >= 1; i--) {
			arr2[i]=arr2[i-1];
		}
		arr2[0]=0;
		
	}
	
	public Prediction[] mostPopularItemsPrediction(){
		synchronized (cachedQueue) {
			Prediction[] ret=this.cachedQueue.toArray(new Prediction[this.cachedQueue.size()]);
			Arrays.sort(ret, Collections.reverseOrder());
			return ret;
		}
		
		
		
		
	}
	
	

}
