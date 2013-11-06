package edu.uniandes.privateRecsys.onlineRecommender.jsonParserEvents;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

import edu.uniandes.privateRecsys.onlineRecommender.vo.ClickImpressionEvent;
import edu.uniandes.privateRecsys.onlineRecommender.vo.FileEvent;

public class PlistaEventObserverForUsers implements Observer {
	
	private static final int MIN_TRAINS = 10;
	private HashMap<Long, HashSet<Long>> userId_countInTrain= new HashMap<>();
	private HashMap<Long, HashSet<Long>> userId_countInTest= new HashMap<>();
	
	private HashSet<Long> confirmedTrainUsers= new HashSet<Long>();
	private HashSet<Long> confirmedTestUsers= new HashSet<Long>();
	private boolean inTrainingPhase=true;
	
	private long numSuccessfullEvents=0;
	private long numReceivedEvents=0;
	private int numRepetitions;
	
	public void startParsingFiles(String directory) throws IOException{
		
		LinkedList<String> prefixes=new LinkedList<String>();
	
	
		prefixes.add("click");
		prefixes.add("impression");
		PlistaJsonEventCreator plistaEventCreator=new PlistaJsonEventCreator(directory, 1, 25, prefixes);
		//PlistaJsonEventCreator plistaEventCreator=new PlistaJsonEventCreator(directory, 1, 30, prefixes);
		plistaEventCreator.addObserver(this);
		plistaEventCreator.startEvents();
		inTrainingPhase=false;
		this.userId_countInTrain.clear();
		System.gc();
		
		PlistaJsonEventCreator plistaEventCreator2=new PlistaJsonEventCreator(directory, 26, 30, prefixes);
		plistaEventCreator2.addObserver(this);
		plistaEventCreator2.startEvents();
		
		
		
	
		
		PrintWriter pr= new PrintWriter(new File(directory,"itemsCount2.csv"));
		for (Long key : this.confirmedTestUsers) {
			
				pr.println(key);
				numSuccessfullEvents++;
			
			
		}
		pr.close();
		
	}
	
	public long getNumSuccessfullEvents() {
		return numSuccessfullEvents;
	}
	@Override
	public void update(Observable o, Object arg) {
		numReceivedEvents++;
		FileEvent event=(FileEvent) arg;
		if(event.getEventType().equals(FileEvent.ITEM_CLICKED_EVENT)||event.getEventType().equals(FileEvent.ITEM_IMPRESSION_EVENT)){
			ClickImpressionEvent clickedImpressionEv=(ClickImpressionEvent) event;
			processClickImpressionEvent(clickedImpressionEv);
		}
		
	}
	//1682081761
	private void processClickImpressionEvent(
			ClickImpressionEvent clickedImpressionEv) {
		
		
		long userId=clickedImpressionEv.getUserId();
		if (userId != -1 && userId != 0) {
			HashSet<Long> count = null;
			
			
				
			if(inTrainingPhase&& !this.confirmedTrainUsers.contains(userId)){
				
				count = userId_countInTrain.get(userId);
				if(count==null)
					count= new HashSet<Long>();	
				count.add(clickedImpressionEv.getItemId());
				
				if(count.size()==MIN_TRAINS){
					this.confirmedTrainUsers.add(userId);
					userId_countInTrain.remove(userId);
					
				}else{
					userId_countInTrain.put(userId,count);
				}
			}
			else if(!inTrainingPhase&& !this.confirmedTestUsers.contains(userId)&&this.confirmedTrainUsers.contains(userId)){
				count = userId_countInTest.get(userId);
				if(count==null)
					count= new HashSet<Long>();	
				count.add(clickedImpressionEv.getItemId());
				
				if(count.size()==MIN_TRAINS){
					this.confirmedTestUsers.add(userId);
					userId_countInTest.remove(userId);
					
				}
				else{
					userId_countInTest.put(userId,count);
				}
			}
			
			
			

		}
		
	}
	
	public static void main(String[] args) {
		
		PlistaEventObserverForUsers userObserver= new PlistaEventObserverForUsers();
		try {
			if (args.length > 0) {

				userObserver.startParsingFiles(args[0]);

			}
			else{
				userObserver.startParsingFiles("data/plista");
			}
			
			System.out.println("received:"+userObserver.getNumReceivedEvents()+" successfull :"+ userObserver.getNumSuccessfullEvents()+" numRepetitions: "+userObserver.getNumRepetitions());
			System.out.println(new Date());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private long getNumRepetitions() {
		// TODO Auto-generated method stub
		return this.numRepetitions;
	}

	public long getNumReceivedEvents() {
		return numReceivedEvents;
	}
	
	
	
	

}
