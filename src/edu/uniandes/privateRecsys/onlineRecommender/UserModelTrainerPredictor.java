package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashMap;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.metadata.SlidingWindowCountMinSketch;
import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public interface UserModelTrainerPredictor {

	public void setModelRepresentation(FactorUserItemRepresentation model);
	public void setLearningRateStrategy(LearningRateStrategy strategy);
	
	public  Prediction calculatePrediction(UserTrainEvent event,
			int minTrains) throws TasteException;

	public  HashMap<String, Vector> calculateProbabilityUpdate(
			UserTrainEvent event, String rating, Vector itemVector,
			UserProfile oldUserPrivate, String[] ratingScale) throws TasteException;

	public  HashMap<String, BetaDistribution> calculatePriorsUpdate(
			UserTrainEvent event, HashMap<String, BetaDistribution> biasVector,
			String[] ratingScale);

	public  Vector calculatehyperParamsUpdate(UserTrainEvent event,Vector itemVector,
			HashMap<String, Vector> trainedProfiles,
			HashMap<String, BetaDistribution> biasVector,Vector oldHyperparameters, int numTrains);

	public UserMetadataInfo calculateMetadataUpdate(UserTrainEvent event, UserMetadataInfo trainedMetadataProfiles,int numTrains);
	

	public int getHyperParametersSize();

	public boolean hasHyperParameters();

	public boolean hasProbabilityPrediction();

	public boolean hasMetadataPredictor();

	public boolean hasBiasPredictor();

	public boolean hasUserHistory();

	public boolean saveItemMetadata();

	public void updateItemProbabilityVector(
			UserTrainEvent gamma, UserProfile oldUserProfile,
			long itemId, String rating);
	public SlidingWindowCountMinSketch buildMetadataSketch();
	

	



	
	
	
	

}