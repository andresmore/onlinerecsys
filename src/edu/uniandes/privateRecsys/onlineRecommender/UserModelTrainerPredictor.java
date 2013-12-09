package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashMap;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public interface UserModelTrainerPredictor {

	public void setModelRepresentation(FactorUserItemRepresentation model);
	
	public  Prediction calculatePrediction(UserTrainEvent event,
			int minTrains) throws TasteException;

	public  HashMap<String, Vector> calculateProbabilityUpdate(
			double gamma, String rating, Vector itemVector,
			UserProfile oldUserPrivate, String[] ratingScale);

	public  HashMap<String, BetaDistribution> calculatePriorsUpdate(
			UserTrainEvent event, HashMap<String, BetaDistribution> biasVector,
			String[] ratingScale);

	public  Vector calculatehyperParamsUpdate(double gamma,UserTrainEvent event,Vector itemVector,
			HashMap<String, Vector> trainedProfiles,
			HashMap<String, BetaDistribution> biasVector,Vector oldHyperparameters, int numTrains);

	public UserMetadataInfo calculateMetadataUpdate(UserTrainEvent event,double gamma, UserMetadataInfo trainedMetadataProfiles);
	
	public int getHyperParametersSize();
	
	
	

}