package edu.uniandes.privateRecsys.onlineRecommender.jsonParserEvents;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

import org.apache.mahout.cf.taste.impl.common.FullRunningAverageAndStdDev;

import edu.uniandes.privateRecsys.onlineRecommender.vo.ClickImpressionEvent;
import edu.uniandes.privateRecsys.onlineRecommender.vo.CreateUpdateEvent;
import edu.uniandes.privateRecsys.onlineRecommender.vo.FileEvent;

public class PlistaEventObserverForTimeAveragePerPublisher implements Observer {
	
	private HashMap<Long, Long> itemId_timeStamp_map= new HashMap<Long, Long>();
	private HashMap<Long,FullRunningAverageAndStdDev> publisherId_average= new HashMap<Long, FullRunningAverageAndStdDev>();
	private long numSuccessfullEvents=0;
	private long numReceivedEvents=0;
	
	public void startParsingFiles(String directory) throws IOException{
		
		LinkedList<String> prefixes=new LinkedList<String>();
	
		prefixes.add("create");
		prefixes.add("update");
		prefixes.add("click");
		prefixes.add("impression");
		PlistaJsonEventCreator plistaEventCreator=new PlistaJsonEventCreator(directory, 1, 30, prefixes);
		plistaEventCreator.addObserver(this);
		plistaEventCreator.startEvents();
		
		
	}
	public HashMap<Long, FullRunningAverageAndStdDev> getPublisherId_average() {
		return publisherId_average;
	}
	public long getNumSuccessfullEvents() {
		return numSuccessfullEvents;
	}
	@Override
	public void update(Observable o, Object arg) {
		numReceivedEvents++;
		FileEvent event=(FileEvent) arg;
		if(event.getEventType().equals(FileEvent.ITEM_CREATE_EVENT)||event.getEventType().equals(FileEvent.ITEM_UPDATE_EVENT)){
			CreateUpdateEvent createUpdateEv=(CreateUpdateEvent) event;
			processCreateUpdateEvent(createUpdateEv);
		}
		else if(event.getEventType().equals(FileEvent.ITEM_CLICKED_EVENT)||event.getEventType().equals(FileEvent.ITEM_IMPRESSION_EVENT)){
			ClickImpressionEvent clickedImpressionEv=(ClickImpressionEvent) event;
			processClickImpressionEvent(clickedImpressionEv);
		}
		
	}
	private void processClickImpressionEvent(
			ClickImpressionEvent clickedImpressionEv) {
		
		
		long publisherId=clickedImpressionEv.getPublisherId();
		long itemId=clickedImpressionEv.getItemId();
		Long lastItemTimestamp=this.itemId_timeStamp_map.get(itemId);
		long timeStamp= clickedImpressionEv.getTimestamp();
		if(publisherId!=-1 && itemId!=-1 &&lastItemTimestamp!=null){
			FullRunningAverageAndStdDev avg=this.publisherId_average.get(publisherId);
			if(avg==null){
				avg= new FullRunningAverageAndStdDev();
				publisherId_average.put(publisherId, avg);
				
			}
			long timeDifference=timeStamp-lastItemTimestamp;
			if(timeDifference>0){
				avg.addDatum(timeDifference);
				numSuccessfullEvents++;
			}
			else{
				//WTF?
			}
			
		}
		
	}
	private void processCreateUpdateEvent(CreateUpdateEvent createUpdateEv) {
		
		long itemId= createUpdateEv.getItemId();
		long timeStamp=createUpdateEv.getTimestamp();
		
		if(itemId!=-1&&timeStamp!=-1){
			this.itemId_timeStamp_map.put(itemId, timeStamp);
		}
		
	}
	
	public static void main(String[] args) {
		
		PlistaEventObserverForTimeAveragePerPublisher publisher= new PlistaEventObserverForTimeAveragePerPublisher();
		try {
			if (args.length > 0) {

				publisher.startParsingFiles(args[0]);

			}
			else{
				publisher.startParsingFiles("data/plista");
			}
			HashMap<Long,FullRunningAverageAndStdDev> publisherId_average=publisher.getPublisherId_average();
			for (Long publisherId : publisherId_average.keySet()) {
				FullRunningAverageAndStdDev avg=publisherId_average.get(publisherId);
				System.out.println(""+publisherId+'\t'+avg.getCount()+'\t'+avg.getAverage()+'\t'+avg.getStandardDeviation());
			}
			System.out.println("received:"+publisher.getNumReceivedEvents()+" successfull :"+ publisher.getNumSuccessfullEvents());
			System.out.println(new Date());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public long getNumReceivedEvents() {
		return numReceivedEvents;
	}
	
	
	
	

}
