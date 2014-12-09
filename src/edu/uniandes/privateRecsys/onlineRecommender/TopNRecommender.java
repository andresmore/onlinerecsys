package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.Set;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;

public interface TopNRecommender {

	public  Prediction[] getTopRecommendationForUsers(Set<Long> ids,Long userID,
			int size, int minTrains) throws TasteException;

}