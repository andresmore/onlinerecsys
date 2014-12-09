package edu.uniandes.privateRecsys.onlineRecommender.vo;

public class ReportErrorEvent implements FileEvent {

	@Override
	public String getEventType() {
		
		return FileEvent.CALCULATE_ERROR;
	}

	@Override
	public long getTimestamp() {
		
		return 0;
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
