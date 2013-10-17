package edu.uniandes.privateRecsys.onlineRecommender.jsonParserEvents;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.vo.FileEvent;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class PListaEventCollector implements Observer{

	private HashSet<Long> users;
	private HashMap<Long, HashSet<Long>> userPositiveExamples= new HashMap<Long, HashSet<Long>>();

	public PListaEventCollector(HashSet<Long> users,
			PlistaJsonEventCreator plistaEventCreator) {
		// TODO Auto-generated constructor stub
		
		this.users=users;
		plistaEventCreator.addObserver(this);
		
		
	}

	@Override
	public void update(Observable o, Object arg) {
		FileEvent event = (FileEvent) arg;
		
			if(event.convertToTrainEvent()!=null){
				processEvent(event.convertToTrainEvent());
			}
			
		
		
	}

	private void processEvent(UserTrainEvent event) {
		//Ignore user -1 or 0;
		long userId=event.getUserId();
		if(userId!=0 && userId!=-1&& this.users.contains(userId)){
			HashSet<Long> positiveExamples=userPositiveExamples.get(userId);
			if(positiveExamples==null)
				positiveExamples= new HashSet<Long>();
			positiveExamples.add(event.getItemId());
			
			this.userPositiveExamples.put(userId, positiveExamples);
			
		}
		
	}

	public HashMap<Long, HashSet<Long>> getPositiveElementsForUsers() {
		// TODO Auto-generated method stub
		return userPositiveExamples;
	}

	

}
