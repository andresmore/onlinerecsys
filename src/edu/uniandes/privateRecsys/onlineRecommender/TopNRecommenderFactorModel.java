package edu.uniandes.privateRecsys.onlineRecommender;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class TopNRecommenderFactorModel implements TopNRecommender,Serializable {

	
	
	private UserModelTrainerPredictor predictor;
	
	public TopNRecommenderFactorModel(UserModelTrainerPredictor predictor) {
		
		this.predictor = predictor;
	}

	/* (non-Javadoc)
	 * @see edu.uniandes.privateRecsys.onlineRecommender.TopNRecommender#getTopRecommendationForUsers(java.lang.Long, int, int)
	 */
	@Override
	public Prediction[] getTopRecommendationForUsers( Set<Long> availableItems,Set<Long> ratedItems,Long userID, int size, int minTrains) throws TasteException {
		PriorityQueue<Prediction> predictions= new PriorityQueue<>(size+1);
		
		
		for (Long itemId : availableItems) {
			if (!ratedItems.contains(itemId)) {
				UserTrainEvent event = new UserTrainEvent(userID, itemId, "",
						1, "");
				Prediction p = predictor.calculatePrediction(event, 0);
				predictions.add(p);
				if (predictions.size() > size) {
					predictions.poll();
				}
			}
		}
		
		Prediction[] ret= predictions.toArray(new Prediction[predictions.size()]);
		for(int i=ret.length-1;i>=0;i--) {
			ret[i]=predictions.poll();
		}
		
		return ret;
	}

}
