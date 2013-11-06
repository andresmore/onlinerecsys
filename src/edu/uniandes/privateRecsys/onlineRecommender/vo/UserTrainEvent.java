package edu.uniandes.privateRecsys.onlineRecommender.vo;


public class UserTrainEvent implements FileEvent{
	
	private long userId;
	private long itemId;
	private String rating;
	private long time;
	private int timeSlot;
	
	public UserTrainEvent(long userId, long itemId, String rating, long time) {
		super();
		this.userId = userId;
		this.itemId = itemId;
		this.rating = rating;
		this.time = time;
		
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
