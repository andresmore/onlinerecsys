package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashSet;
import java.util.Set;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;

public class TopNPopularityRecommender implements TopNRecommender {

	private PopularityObserver popObserver;

	public TopNPopularityRecommender(PopularityObserver popObserver) {
		this.popObserver=popObserver;
	}

	
	@Override
	public Prediction[] getTopRecommendationForUsers(Set<Long> ids,Set<Long> ratedItems,
			Long userID, int size, int minTrains) throws TasteException {
		// TODO Auto-generated method stub
		return popObserver.mostPopularItemsPrediction();
	}


	

}
