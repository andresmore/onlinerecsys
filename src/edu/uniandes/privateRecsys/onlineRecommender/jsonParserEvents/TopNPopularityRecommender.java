package edu.uniandes.privateRecsys.onlineRecommender.jsonParserEvents;

import java.util.Set;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.PopularityObserver;
import edu.uniandes.privateRecsys.onlineRecommender.TopNRecommender;
import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;

public class TopNPopularityRecommender implements TopNRecommender {

	private PopularityObserver popObserver;

	public TopNPopularityRecommender(PopularityObserver popObserver) {
		this.popObserver=popObserver;
	}

	
	@Override
	public Prediction[] getTopRecommendationForUsers(Set<Long> ids,
			Long userID, int size, int minTrains) throws TasteException {
		// TODO Auto-generated method stub
		return popObserver.mostPopularItemsPrediction();
	}

}
