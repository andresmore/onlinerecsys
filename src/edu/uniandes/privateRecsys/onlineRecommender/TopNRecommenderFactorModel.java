package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class TopNRecommenderFactorModel implements TopNRecommender {

	
	
	private UserModelTrainerPredictor predictor;
	
	public TopNRecommenderFactorModel(UserModelTrainerPredictor predictor) {
		
		this.predictor = predictor;
	}

	/* (non-Javadoc)
	 * @see edu.uniandes.privateRecsys.onlineRecommender.TopNRecommender#getTopRecommendationForUsers(java.lang.Long, int, int)
	 */
	@Override
	public Prediction[] getTopRecommendationForUsers(Set<Long> ids,Long userID, int size, int minTrains) throws TasteException {
		PriorityQueue<Prediction> predictions= new PriorityQueue<>(11);
		
		
		for (Long itemId : ids) {
			UserTrainEvent event= new UserTrainEvent(userID, itemId, "", 1, "");
			Prediction p=predictor.calculatePrediction(event, 0);
			predictions.add(p);
			if(predictions.size()>size){
				predictions.poll();
			}
		}
		
		Prediction[] ret= predictions.toArray(new Prediction[predictions.size()]);
		Arrays.sort(ret, Collections.reverseOrder());
		
		return ret;
	}

}
