package edu.uniandes.privateRecsys.onlineRecommender;

public class BaseModelPredictorWithHistory extends BaseModelPredictor {
	
	
	@Override
	public boolean hasUserHistory() {
		
		return true;
	}

}
