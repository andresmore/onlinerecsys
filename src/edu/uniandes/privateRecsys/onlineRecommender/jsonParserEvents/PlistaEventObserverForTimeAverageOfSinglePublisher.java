package edu.uniandes.privateRecsys.onlineRecommender.jsonParserEvents;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

import org.apache.mahout.cf.taste.impl.common.FullRunningAverageAndStdDev;

import edu.uniandes.privateRecsys.onlineRecommender.vo.ClickImpressionEvent;
import edu.uniandes.privateRecsys.onlineRecommender.vo.CreateUpdateEvent;
import edu.uniandes.privateRecsys.onlineRecommender.vo.FileEvent;

public class PlistaEventObserverForTimeAverageOfSinglePublisher implements Observer {
	
	private HashMap<Long, Long> itemId_timeStamp_map= new HashMap<Long, Long>();
	private FullRunningAverageAndStdDev average= new FullRunningAverageAndStdDev();
	private long numSuccessfullEvents=0;
	private long numReceivedEvents=0;
	private LinkedList<PrintWriter> printWriter;
	private LinkedList<Long> publisherIds;
	
	
	
	public PlistaEventObserverForTimeAverageOfSinglePublisher(LinkedList<Long> publisherIds){
		this.publisherIds=publisherIds;
	}
	public void startParsingFiles(String directory) throws IOException{
		
		
		this.printWriter= new LinkedList<>();
		for (int i=0;i<publisherIds.size();i++) {
			printWriter.addLast(new PrintWriter(new File(directory,publisherIds.get(i)+"_averageData.data")));
		}
		
		LinkedList<String> prefixes=new LinkedList<String>();
	
		prefixes.add("create");
		prefixes.add("update");
		prefixes.add("click");
		prefixes.add("impression");
		PlistaJsonEventCreator plistaEventCreator=new PlistaJsonEventCreator(directory, 1, 30, prefixes);
		plistaEventCreator.addObserver(this);
		plistaEventCreator.startEvents();
		for (PrintWriter pr : printWriter) {
			pr.close();
		}
		
		
		
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
		if(publisherId!=-1 && itemId!=-1 &&lastItemTimestamp!=null ){
			long timeDifference=timeStamp-lastItemTimestamp;
			for (int i=0;i<publisherIds.size();i++) {
				
				if(timeDifference>0&&publisherId==publisherIds.get(i)){
					this.average.addDatum(timeDifference);
					numSuccessfullEvents++;
					printWriter.get(i).println(timeStamp+","+timeDifference);
				}
				
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
		LinkedList<Long> pubIds=new LinkedList<Long>();
		pubIds.add((long) 13554);
		pubIds.add((long) 418);
		pubIds.add((long) 596);
		pubIds.add((long) 1677);
		pubIds.add((long) 2522);
		pubIds.add((long) 694);
		pubIds.add((long) 12935);
		pubIds.add((long) 2524);
		pubIds.add((long) 2525);
		
		PlistaEventObserverForTimeAverageOfSinglePublisher publisher= new PlistaEventObserverForTimeAverageOfSinglePublisher(pubIds);
		try {
			if (args.length > 0) {

				publisher.startParsingFiles(args[0]);

			}
			else{
				publisher.startParsingFiles("data/plista");
			}
			System.out.println(publisher.getAverage());
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
	public FullRunningAverageAndStdDev getAverage() {
		return average;
	}
	
	
	
	

}
