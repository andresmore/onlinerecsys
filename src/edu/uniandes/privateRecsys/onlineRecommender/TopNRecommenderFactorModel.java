package edu.uniandes.privateRecsys.onlineRecommender;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.mahout.cf.taste.common.TasteException;
import org.jfree.util.Log;

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
		//PriorityBlockingQueue<Prediction> predictions= new PriorityBlockingQueue<>(size+1);
		
		
		Set<Long> availableForUser=new HashSet<>(availableItems);
		availableForUser.removeAll(ratedItems);
		List<Prediction> list=availableForUser.parallelStream().map( itemId -> {
			try {
				return predictor.calculatePrediction(new UserTrainEvent(userID, itemId, "",
								1, ""), 0);
			} catch (TasteException e) {
				Log.error("taste error",e);
				e.printStackTrace();
			}
			return Prediction.createNoAblePrediction(userID, itemId);
		}).collect(Collectors.toList());
		

		PriorityQueue<Prediction> predictions= new PriorityQueue<>(size+1);
		for (Prediction prediction : list) {
			predictions.add(prediction);
			if(predictions.size()>size) {
				predictions.poll();
			}
			
		}
		
		
		Prediction[] ret= predictions.toArray(new Prediction[predictions.size()]);
		Arrays.sort(ret);
		for(int i=ret.length-1;i>=0;i--) {
			ret[i]=predictions.poll();
		}
		
		return ret;
	}

}
