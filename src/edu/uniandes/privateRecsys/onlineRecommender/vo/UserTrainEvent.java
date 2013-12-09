package edu.uniandes.privateRecsys.onlineRecommender.vo;

import java.util.HashSet;


public class UserTrainEvent implements FileEvent{
	
	private long userId;
	private long itemId;
	private String rating;
	private long time;
	private int timeSlot;
	private HashSet<String> metadata;
	
	private HashSet<Character> separators;
	
	public UserTrainEvent(long userId, long itemId, String rating, long time, String metadata) {
		super();
		this.separators= new HashSet<>(4);
		separators.add('{');
		separators.add('}');
		separators.add(',');
		separators.add(':');
		
		
		this.userId = userId;
		this.itemId = itemId;
		this.rating = rating;
		this.time = time;
		this.metadata=breakConcepts(metadata);
		
	}
	public long getUserId() {
		return userId;
	}
	public long getItemId() {
		return itemId;
	}
	public String getRating() {
		return rating;
	}
	public long getTime() {
		return time;
	}
	public HashSet<String> getMetadata(){
		return metadata;
	}
	
	
	public void updateRating(String rating){
		this.rating=rating;
	}
	
	@Override
	public String getEventType() {
		
		return FileEvent.FILE_EVENT;
	}
	@Override
	public long getTimestamp() {
		
		return time;
	}

	@Override
	public String toString(){
		return this.getEventType()+" "+this.getTimestamp();
	}
	@Override
	public UserTrainEvent convertToTrainEvent() {
		
		return this;
	}
	
	private HashSet<String> breakConcepts(String metadataVector) {
		HashSet<String> concepts= new HashSet<String>();
		
		StringBuilder builder= new StringBuilder();
		
		for (int i = 0; i < metadataVector.length(); i++) {
			char at= metadataVector.charAt(i);
			if( this.separators.contains(at) ){
				if(builder.length()>0)
					concepts.add(builder.toString());
				
					builder= new StringBuilder();
				
			}
			else{
				builder.append(at);
			}
			
		}
		if(builder.length()>0)
			concepts.add(builder.toString());
		
		return concepts;
	}

}
