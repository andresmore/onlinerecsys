package edu.uniandes.privateRecsys.onlineRecommender.vo;

public class CreateUpdateEvent implements FileEvent {

	private String type;
	private Long itemId;
	private long timestamp;

	public CreateUpdateEvent(String type, Long itemId, long timestamp) {
		this.type=type;
		this.itemId=itemId;
		this.timestamp=timestamp;
	}

	@Override
	public String getEventType() {
		// TODO Auto-generated method stub
		return type;
	}

	@Override
	public long getTimestamp() {
		// TODO Auto-generated method stub
		return this.timestamp;
	}
	
	public long getItemId(){
		return itemId;
	}
	
	
	@Override
	public String toString(){
		return this.getEventType()+" "+this.getTimestamp();
	}

	@Override
	public UserTrainEvent convertToTrainEvent() {
		
		return null;
	}

}
