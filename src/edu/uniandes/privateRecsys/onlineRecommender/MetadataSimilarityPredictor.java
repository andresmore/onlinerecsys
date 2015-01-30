package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.ItemProfile;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.metadata.SlidingWindowCountMinSketch;
import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class MetadataSimilarityPredictor implements UserModelTrainerPredictor {

	private FactorUserItemRepresentation modelRepresentation;

	@Override
	public void setModelRepresentation(FactorUserItemRepresentation model) {
		this.modelRepresentation=model;
		
	}

	@Override
	public Prediction calculatePrediction(UserTrainEvent event, int minTrains)
			throws TasteException {

		long userId=event.getUserId();
		long itemId=event.getItemId();
		UserProfile user = modelRepresentation
				.getPrivateUserProfile(userId);
		ItemProfile item = modelRepresentation
				.getPrivateItemProfile(itemId);
		
		LinkedList<Preference> userHistory=user.getUserHistory();
		LinkedList<Long> itemVector=item.getMetadataVector();
		
		if(itemVector==null || userHistory==null)
			return Prediction.createNoAblePrediction(userId, itemId);
		double preference = 0.0;
		double totalSimilarity = 0.0;
		int numRatings=0;
		for (Preference pair : userHistory) {
			ItemProfile otherItem = modelRepresentation
					.getPrivateItemProfile(pair.getItemID());
			LinkedList<Long> otherMetadataVector = otherItem.getMetadataVector();
			
			double similarity = calculateJaccardCoefficient(itemVector,
					otherMetadataVector);

			if (!Double.isNaN(similarity)&&similarity>0) {
				preference += pair.getValue() * similarity;
				totalSimilarity += similarity;
				numRatings++;
			}

		}
		if (numRatings <= 1) {
			return Prediction.createNoAblePrediction(userId, itemId);
		}

		double estimate = (double) (preference / totalSimilarity);
		
		if(Double.isNaN(estimate))
			return Prediction.createNoAblePrediction(userId, itemId);
		
		
		return Prediction.createPrediction(userId, itemId, estimate);
	}
	
	/**
	 * Calculates the Jaccard Coefficient between lists a and b, lists are ordered
	 * @param a first list
	 * @param b second list
	 * @return the size of the intersection divided by the size of the union of both lists
	 */
	private double calculateJaccardCoefficient(LinkedList<Long> a,
			LinkedList<Long> b) {
		
		
		int indexA=0;
		int indexB=0;
		Iterator<Long> iterA=a.iterator();
		Iterator<Long> iterB=b.iterator();
		double union=0;
		double intersection=0;
		
		boolean ended=false;
		
		double aElement=iterA.hasNext()?iterA.next():-1;
		double bElement=iterB.hasNext()?iterB.next():-1;
		while( !(ended) ){
			
			if(indexA==a.size()-1){
				union+=b.size()-indexB;
				ended=true;
			}
			else if(indexB==b.size()-1){
				union+=a.size()-indexA;
				ended=true;
			}
			else{
				
				if(aElement==bElement){
					intersection++;
					union++;
					indexA++;
					indexB++;
					if(iterA.hasNext())
						aElement=iterA.next();
					if(iterB.hasNext())
						aElement=iterB.next();
				}
				else if(aElement>bElement){
					indexB++;
					union++;
					
					if(iterB.hasNext())
						bElement=iterB.next();
				}
				else{
					if(iterA.hasNext())
						aElement=iterA.next();
					
					indexA++;
					union++;
				}
			}
			
		}
		
		
		
		
		
		return intersection/union;
	}

	@Override
	public HashMap<String, Vector> calculateProbabilityUpdate(UserTrainEvent event,
			String rating, Vector itemVector, UserProfile oldUserPrivate,
			String[] ratingScale) {
		
		return null;
	}

	@Override
	public HashMap<String, BetaDistribution> calculatePriorsUpdate(
			UserTrainEvent event, HashMap<String, BetaDistribution> biasVector,
			String[] ratingScale) {
		
		return null;
	}

	@Override
	public Vector calculatehyperParamsUpdate(
			UserTrainEvent event, Vector itemVector,
			HashMap<String, Vector> trainedProfiles,
			HashMap<String, BetaDistribution> biasVector,
			Vector oldHyperparameters, int numTrains) {
		
		return null;
	}

	@Override
	public UserMetadataInfo calculateMetadataUpdate(UserTrainEvent event,
			 UserMetadataInfo trainedMetadataProfiles,
			int numTrains) {
		
		return null;
	}

	@Override
	public int getHyperParametersSize() {
		
		return 0;
	}

	@Override
	public boolean hasHyperParameters() {
		
		return false;
	}

	@Override
	public boolean hasProbabilityPrediction() {
		
		return false;
	}

	@Override
	public boolean hasMetadataPredictor() {
		
		return false;
	}

	@Override
	public boolean hasBiasPredictor() {
		
		return false;
	}

	@Override
	public boolean hasUserHistory() {
		
		return true;
	}

	@Override
	public boolean saveItemMetadata() {
		return true;
	}
	
	@Override
	public String toString() {
		
		return "MetadataSimilarityPredictor";
	}

	@Override
	public void updateItemProbabilityVector(
			UserTrainEvent gamma, UserProfile oldUserProfile,
			long itemId, String rating) {
		
		
	}

	@Override
	public void setLearningRateStrategy(LearningRateStrategy strategy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public SlidingWindowCountMinSketch buildMetadataSketch() {
		
		return null;
	}

}
