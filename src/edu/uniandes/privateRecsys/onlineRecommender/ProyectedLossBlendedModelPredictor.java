package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashMap;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.VectorProjector;
import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;


/**
 * Model that blends probability hypothesis and average for online training and prediction
 * @author Andres M
 *
 */
public  class ProyectedLossBlendedModelPredictor implements UserModelTrainerPredictor {
	
	private FactorUserItemRepresentation modelRepresentation;
	private BaseModelPredictor baseModel;
	private SimpleAveragePredictor averageModel;
	
	public ProyectedLossBlendedModelPredictor(){
		this.baseModel= new BaseModelPredictor();
		this.averageModel= new SimpleAveragePredictor();
	}
	public ProyectedLossBlendedModelPredictor(FactorUserItemRepresentation representation){
		this.modelRepresentation=representation;
		this.baseModel.setModelRepresentation(representation);
		this.averageModel.setModelRepresentation(representation);
	}
	
	
	
	/* (non-Javadoc)
	 * @see edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.ModelPredictor#calculatePrediction(long, long, int)
	 */
	@Override
	public  Prediction calculatePrediction(long itemId, long userId, int minTrains) throws TasteException{
		
		
		Prediction prediction1=baseModel.calculatePrediction(itemId,userId,minTrains);
		Prediction prediction2=averageModel.calculatePrediction(itemId, userId, minTrains);
		
		UserProfile profile=modelRepresentation.getPrivateUserProfile(userId);
		Vector hyperparameters=profile.getHyperParameters();
		
		double finalPrediction=hyperparameters.get(0)*prediction1.getPredictionValue()+hyperparameters.get(1)*prediction2.getPredictionValue();
		
		return Prediction.createPrediction(userId, itemId, finalPrediction);
		
	}
	@Override
	public void setModelRepresentation(FactorUserItemRepresentation model) {
		this.modelRepresentation=model;
		this.baseModel.setModelRepresentation(model);
		this.averageModel.setModelRepresentation(model);
		
	}
	@Override
	public HashMap<String, Vector> calculateProbabilityUpdate(double gamma,
			String rating, Vector itemVector, UserProfile oldUserPrivate,
			String[] ratingScale) {
		
		return baseModel.calculateProbabilityUpdate(gamma, rating, itemVector, oldUserPrivate, ratingScale);
	}
	@Override
	public HashMap<String, BetaDistribution> calculatePriorsUpdate(
			UserTrainEvent event, HashMap<String, BetaDistribution> biasVector,
			String[] ratingScale) {
		
		
		return averageModel.calculatePriorsUpdate(event, biasVector, ratingScale);
	}
	@Override
	public Vector calculatehyperParamsUpdate(double gamma,UserTrainEvent event,Vector itemVector,
			HashMap<String, Vector> trainedProfiles,
			HashMap<String, BetaDistribution> biasVector, Vector hyperparameters, int numTrains) {

		double rating= Double.parseDouble(event.getRating());
		double prediction1=baseModel.calculatePrediction(itemVector,trainedProfiles);
		double prediction2=averageModel.calculatePrediction(biasVector);
		if(hyperparameters.getLengthSquared()==0)
			hyperparameters=VectorProjector.projectVectorIntoSimplex(hyperparameters.assign(1));
		double finalPrediction=hyperparameters.get(0)*prediction1+hyperparameters.get(1)*prediction2;
		double finalLoss=Math.pow(rating-finalPrediction,2);
		Vector regrets=hyperparameters.clone();
		regrets.set(0, Math.max(0, finalLoss-Math.pow(rating-prediction1,2)));
		regrets.set(1, Math.max(0, finalLoss-Math.pow(rating-prediction2,2)));
		regrets=VectorProjector.projectVectorIntoSimplex(regrets);
		
		//TODO: Acummulated losses
		hyperparameters=hyperparameters.plus(regrets);
		return VectorProjector.projectVectorIntoSimplex(hyperparameters);
	}
	@Override
	public int getHyperParametersSize() {
		
		return 2;
	}

}
