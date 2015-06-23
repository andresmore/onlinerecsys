package edu.uniandes.privateRecsys.onlineRecommender;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;

import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.vo.FileEvent;

public class FilterElement {

	private Long userId;
	private String trainSet;
	HashSet<Long> itemIds=new HashSet<Long>();
	
	public FilterElement(Long userId, String file){
		this.userId=userId;
		this.trainSet=file;
	}
	
	public  HashSet<Long> getElementsFromFile() {
		
		try {
			FileEventCreator fileEv= new FileEventCreator(new File(trainSet),-1,-1);
			fileEv.addObserver(new Observer() {
				
				@Override
				public void update(Observable o, Object arg) {
					FileEvent ev=(FileEvent)arg;
					if(ev.convertToTrainEvent().getUserId()==userId){
						itemIds.add(ev.convertToTrainEvent().getItemId());
					}
					
				}
			});
			fileEv.startEvents();
		} catch (IOException | PrivateRecsysException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return itemIds;
	}
	
	

}
