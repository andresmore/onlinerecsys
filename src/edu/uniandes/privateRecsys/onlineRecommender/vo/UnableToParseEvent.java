package edu.uniandes.privateRecsys.onlineRecommender.vo;

public class UnableToParseEvent implements FileEvent {

	@Override
	public String getEventType() {
		// TODO Auto-generated method stub
		return FileEvent.UNABLE_TO_PARSE;
	}

	@Override
	public long getTimestamp() {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public UserTrainEvent convertToTrainEvent() {
		
		return null;
	}

}
