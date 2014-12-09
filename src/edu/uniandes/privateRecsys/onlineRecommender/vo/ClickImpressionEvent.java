package edu.uniandes.privateRecsys.onlineRecommender.vo;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class ClickImpressionEvent implements FileEvent {

	private String type;
	private long itemId;
	private long userId;
	private long publisherId;
	private Long timeStamp;
	private Map keywords;
	private String line;

	public ClickImpressionEvent(String type, long itemId, long userId, long publisherID,
			Long timeStamp, Map keywords, String line) {
		this.type=type;
		this.itemId=itemId;
		this.userId=userId;
		this.publisherId=publisherID;
		this.timeStamp=timeStamp;
		this.keywords=keywords;
		this.line=line;
	}

	public long getItemId() {
		return itemId;
	}

	public long getUserId() {
		return userId;
	}

	public long getPublisherId() {
		return publisherId;
	}

	public Map getKeywords() {
		return keywords;
	}

	@Override
	public String getEventType() {
		// TODO Auto-generated method stub
		return type;
	}

	@Override
	public long getTimestamp() {
		// TODO Auto-generated method stub
		return timeStamp;
	}
	
	@Override
	public String toString(){
		return this.getEventType()+" "+this.getTimestamp();
	}

	@Override
	public UserTrainEvent convertToTrainEvent() {
		// TODO Auto-generated method stub
		
		Set<String> keys=keywords.keySet();
		LinkedList<Long> concepts= new LinkedList<Long>();
		for (String string : keys) {
			try{
				concepts.add(Long.parseLong(string));
			}catch(NumberFormatException e){};
		}
		Collections.sort(concepts); 
		return new UserTrainEvent(this.userId, this.itemId, type.equals(ITEM_CLICKED_EVENT)?"2":"1", timeStamp,concepts.toString());
	}

	public String getLine() {
		// TODO Auto-generated method stub
		return this.line;
	}

}
