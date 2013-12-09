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
 * Model that blends probability hypothesis and average beta estimators for online training and prediction
 * @author Andres M
 *
 */
public  class BlendedModelPredictor implements UserModelTrainerPredictor {
	
	
	
	private FactorUserItemRepresentation modelRepresentation;
	private BaseModelPredictor baseModel;
	private SimpleAveragePredictor averageModel;
	private double lossNormalization;
	
	public BlendedModelPredictor(){
		this.baseModel= new BaseModelPredictor();
		this.averageModel= new SimpleAveragePredictor();
	}
	public BlendedModelPredictor(FactorUserItemRepresentation representation){
		this.setModelRepresentation(representation);
	}
	
	
	
	/* (non-Javadoc)
	 * @see edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.ModelPredictor#calculatePrediction(long, long, int)
	 */
	@Override
public  Prediction calculatePrediction(UserTrainEvent event, int minTrains) throws TasteException{
		
		long userId=event.getUserId();
		long itemId=event.getItemId();
		
		
		Prediction prediction1=baseModel.calculatePrediction(event,minTrains);
		Prediction prediction2=averageModel.calculatePrediction(event, minTrains);
		
		UserProfile profile=modelRepresentation.getPrivateUserProfile(userId);
		Vector hyperparameters=profile.getHyperParameters();
		int numTrains=profile.getNumTrains();
		double weightHyperParam1 = calculateWeightFromCumulativeLosses(hyperparameters.get(0),numTrains);
		double weightHyperParam2 = calculateWeightFromCumulativeLosses(hyperparameters.get(1),numTrains);
		double sumWeights=weightHyperParam1+weightHyperParam2;
		double finalPrediction=(weightHyperParam1*prediction1.getPredictionValue()+weightHyperParam2*prediction2.getPredictionValue())/sumWeights;
		
		return Prediction.createPrediction(userId, itemId, finalPrediction);
		
	}
	/**
	 * Exponential weighted average forecaster
	 * @param accumulatedRegret
	 * @param numTrains 
	 * @return
	 */
	private double calculateWeightFromCumulativeLosses(double accumulatedLosses, double numTrains) {
		
		double n=Math.sqrt(8*Math.log(2)/numTrains);
		//double n=0.5;
		return Math.exp(-1*n*accumulatedLosses);
	}
	@Override
	public void setModelRepresentation(FactorUserItemRepresentation model) {
		this.modelRepresentation=model;
		this.baseModel.setModelRepresentation(model);
		this.averageModel.setModelRepresentation(model);
		
		this.lossNormalization=1;
		try{
			String[] scale=this.modelRepresentation.getRatingScale().getScale();
			this.lossNormalization=Math.pow(Double.parseDouble(scale[scale.length-1])-Double.parseDouble(scale[0]),2);
		}catch(Exception ex){
			
		}
		
		
	}
	@Override
	public HashMap<String, Vector> calculateProbabilityUpdate(double gamma,
			String rating, Vector itemVector, UserProfile oldUserPrivate,
			String[] ratingScale) {
		
		
		HashMap<String, Vector> update = baseModel.calculateProbabilityUpdate(gamma, rating, itemVector, oldUserPrivate, ratingScale);
		
		return update;
	}
	@Override
	public HashMap<String, BetaDistribution> calculatePriorsUpdate(
			UserTrainEvent event, HashMap<String, BetaDistribution> biasVector,
			String[] ratingScale) {
		
		
		HashMap<String, BetaDistribution> update = averageModel.calculatePriorsUpdate(event, biasVector, ratingScale);
		
		
		
		
		return update;
	}
	@Override
	public Vector calculatehyperParamsUpdate(double gamma,UserTrainEvent event,Vector itemVector,
			HashMap<String, Vector> trainedProfiles,
			HashMap<String, BetaDistribution> biasVector, Vector hyperparameters, int numTrains) {

		double rating= Double.parseDouble(event.getRating());
		double prediction1=baseModel.calculatePrediction(itemVector,trainedProfiles);
		double prediction2=averageModel.calculatePrediction(biasVector);
		
		double weightHyperParam1 = calculateWeightFromCumulativeLosses(hyperparameters.get(0),numTrains);
		double weightHyperParam2 = calculateWeightFromCumulativeLosses(hyperparameters.get(1),numTrains);
		double sumWeights=weightHyperParam1+weightHyperParam2;
		double finalPrediction=(weightHyperParam1*prediction1+weightHyperParam2*prediction2)/sumWeights;
		double predictorLoss=Math.pow(rating-finalPrediction,2)/this.lossNormalization;
		
		Vector losses=hyperparameters.clone();
		
		double predictor1Loss = Math.pow(rating-prediction1,2)/this.lossNormalization;
		double predictor2Loss = Math.pow(rating-prediction2,2)/this.lossNormalization;
		losses.set(0,predictor1Loss);
		losses.set(1, predictor2Loss);
		losses.set(2,predictorLoss);
		
		
		hyperparameters=hyperparameters.plus(losses);
		return hyperparameters;
	}
	@Override
	public int getHyperParametersSize() {
		
		return 3;
	}
	
	@Override
	public String toString(){
		return "BlendedModelPredictor";
	}
	
	
	@Override
	public UserMetadataInfo calculateMetadataUpdate(UserTrainEvent event,
			double gamma, UserMetadataInfo trainedMetadataProfiles) {
		
		return null;
	}

}
