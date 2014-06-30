package edu.uniandes.privateRecsys.onlineRecommender.vo;



public class UserTrainEvent implements FileEvent{
	
	private long userId;
	private long itemId;
	private String rating;
	private long time;
	private String metadata;
	
	
	
	public UserTrainEvent(long userId, long itemId, String rating, long time, String metadata) {		
		
		
		this.userId = userId;
		this.itemId = itemId;
		this.rating = rating;
		this.time = time;
		this.metadata=metadata;
		
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
	public String getMetadata(){
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
	
	

}
