package edu.uniandes.privateRecsys.onlineRecommender;

public class BaseModelPredictorWithItemRegularizationUpdateWithHistory extends BaseModelPredictorWithItemRegularizationUpdate {

	public BaseModelPredictorWithItemRegularizationUpdateWithHistory(double regularizationConstant) {
		super(regularizationConstant);
		
	}

	@Override
	public boolean hasUserHistory() {
		
		return true;
	}
}
