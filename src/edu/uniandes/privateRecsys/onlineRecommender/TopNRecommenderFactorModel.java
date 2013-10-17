package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.Arrays;
import java.util.Collections;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;

public class TopNRecommenderFactorModel implements TopNRecommender {

	
	private FactorUserItemRepresentation userItemRep;
	
	public TopNRecommenderFactorModel(FactorUserItemRepresentation userItemRep) {
		
		this.userItemRep = userItemRep;
	}

	/* (non-Javadoc)
	 * @see edu.uniandes.privateRecsys.onlineRecommender.TopNRecommender#getTopRecommendationForUsers(java.lang.Long, int, int)
	 */
	@Override
	public Prediction[] getTopRecommendationForUsers(Set<Long> ids,Long userID, int size, int minTrains) throws TasteException {
		PriorityQueue<Prediction> predictions= new PriorityQueue<>(11);
		
		
		for (Long itemId : ids) {
			Prediction p=userItemRep.calculatePrediction(itemId, userID, 10);
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
