package edu.uniandes.privateRecsys.onlineRecommender.vo;

public interface FileEvent {
	
	public static final String FILE_EVENT="file_event";
	public static final String STOP_EVENT="stop_event";
	public static final String CALCULATE_ERROR="stop_event";
	public static final String ITEM_CREATE_EVENT="create";
	public static final String ITEM_UPDATE_EVENT="update";
	public static final String ITEM_IMPRESSION_EVENT="impression";
	public static final String ITEM_CLICKED_EVENT="click";
	
	public static final String UNABLE_TO_PARSE = "unable";
	
	public String getEventType();
	public long getTimestamp();
	
	public UserTrainEvent convertToTrainEvent();
}
