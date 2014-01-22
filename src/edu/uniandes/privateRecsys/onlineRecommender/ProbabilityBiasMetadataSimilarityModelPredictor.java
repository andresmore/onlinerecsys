package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashMap;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;


/**
 * Model that blends probability hypothesis, average beta estimators and metadata similarity for online training and prediction
 * @author Andres M
 *
 */
public  class ProbabilityBiasMetadataSimilarityModelPredictor implements UserModelTrainerPredictor {
	
	
	
	private static final int NUM_PREDICTORS = 3;
	private FactorUserItemRepresentation modelRepresentation;
	private BaseModelPredictor baseModel;
	private SimpleAveragePredictor averageModel;
	private MetadataSimilarityPredictor metadataModel;
	private double lossNormalization;
	
	public ProbabilityBiasMetadataSimilarityModelPredictor(BaseModelPredictor base,SimpleAveragePredictor average, MetadataSimilarityPredictor metadata ){
		this.baseModel= base;
		this.averageModel= average;
		this.metadataModel= metadata;
	}
	public ProbabilityBiasMetadataSimilarityModelPredictor(FactorUserItemRepresentation representation){
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
		Prediction prediction3=metadataModel.calculatePrediction(event, minTrains);
		
		UserProfile profile=modelRepresentation.getPrivateUserProfile(userId);
		Vector hyperparameters=profile.getHyperParameters();
		int numTrains=profile.getNumTrains();
		double weightHyperParam1 = calculateWeightFromCumulativeLosses(hyperparameters.get(0),numTrains);
		double weightHyperParam2 = calculateWeightFromCumulativeLosses(hyperparameters.get(1),numTrains);
		double weightHyperParam3 = calculateWeightFromCumulativeLosses(hyperparameters.get(2),numTrains);
		
		double sumWeights=weightHyperParam1+weightHyperParam2;
		
		if(!prediction3.isNoPrediction())
			sumWeights+=weightHyperParam3;
		
		double finalPrediction=(weightHyperParam1*prediction1.getPredictionValue()+weightHyperParam2*prediction2.getPredictionValue());
		if(prediction3.isNoPrediction())
			finalPrediction+=weightHyperParam3*prediction3.getPredictionValue();
		
		
		finalPrediction/=sumWeights;
		
		return Prediction.createPrediction(userId, itemId, finalPrediction);
		
	}
	/**
	 * Exponential weighted average forecaster
	 * @param accumulatedRegret
	 * @param numTrains 
	 * @return
	 */
	private double calculateWeightFromCumulativeLosses(double accumulatedLosses, double numTrains) {
		
		double n=Math.sqrt(8*Math.log(NUM_PREDICTORS)/numTrains);
		//double n=0.5;
		return Math.exp(-1*n*accumulatedLosses);
	}
	@Override
	public void setModelRepresentation(FactorUserItemRepresentation model) {
		this.modelRepresentation=model;
		this.baseModel.setModelRepresentation(model);
		this.averageModel.setModelRepresentation(model);
		this.metadataModel.setModelRepresentation(model);
		
		this.lossNormalization=1;
		try{
			String[] scale=this.modelRepresentation.getRatingScale().getScale();
			this.lossNormalization=Math.pow(Double.parseDouble(scale[scale.length-1])-Double.parseDouble(scale[0]),2);
		}catch(Exception ex){
			
		}
		
		
	}
	@Override
	public HashMap<String, Vector> calculateProbabilityUpdate(UserTrainEvent event,
			String rating, Vector itemVector, UserProfile oldUserPrivate,
			String[] ratingScale) {
		
		
		HashMap<String, Vector> update = baseModel.calculateProbabilityUpdate(event, rating, itemVector, oldUserPrivate, ratingScale);
		
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
	public Vector calculatehyperParamsUpdate(UserTrainEvent event,Vector itemVector,
			HashMap<String, Vector> trainedProfiles,
			HashMap<String, BetaDistribution> biasVector, Vector hyperparameters, int numTrains) {

		double rating= Double.parseDouble(event.getRating());
		double prediction1=baseModel.calculatePrediction(itemVector,trainedProfiles);
		double prediction2=averageModel.calculatePrediction(biasVector);
		double prediction3=-1;
		try {
			Prediction prediction3Pr=metadataModel.calculatePrediction(event,0);
			if(!prediction3Pr.isNoPrediction()){
				prediction3=prediction3Pr.getPredictionValue();
			}
		} catch (TasteException e) {
			
			e.printStackTrace();
		}
		
		
		double weightHyperParam1 = calculateWeightFromCumulativeLosses(hyperparameters.get(0),numTrains);
		double weightHyperParam2 = calculateWeightFromCumulativeLosses(hyperparameters.get(1),numTrains);
		double weightHyperParam3 = calculateWeightFromCumulativeLosses(hyperparameters.get(2),numTrains);
		
		double sumWeights=weightHyperParam1+weightHyperParam2;
		
		if(prediction3!=-1)
			sumWeights+=weightHyperParam3;
		
		double finalPrediction=(weightHyperParam1*prediction1+weightHyperParam2*prediction2);
		if(prediction3!=-1)
			finalPrediction+=weightHyperParam3*prediction3;
		
		
		finalPrediction/=sumWeights;
		
		
		double predictorLoss=Math.pow(rating-finalPrediction,2)/this.lossNormalization;
		
		Vector losses=hyperparameters.clone();
		
		double predictor1Loss = Math.pow(rating-prediction1,2)/this.lossNormalization;
		double predictor2Loss = Math.pow(rating-prediction2,2)/this.lossNormalization;
		double predictor3Loss = prediction3==-1?1:Math.pow(rating-prediction3,2)/this.lossNormalization;
	
		losses.set(0,predictor1Loss);
		losses.set(1, predictor2Loss);
		losses.set(2, predictor3Loss);
		
		losses.set(3,predictorLoss);
		
		
		hyperparameters=hyperparameters.plus(losses);
		return hyperparameters;
	}
	@Override
	public int getHyperParametersSize() {
		
		return 4;
	}
	
	@Override
	public String toString(){
		return "ProbabilityBiasMetadataSimilarityModelPredictor";
	}
	
	
	@Override
	public UserMetadataInfo calculateMetadataUpdate(UserTrainEvent event,
			UserMetadataInfo trainedMetadataProfiles,int numTrains) {
		
		return null;
	}
	@Override
	public boolean hasHyperParameters() {
		return true;
	}
	@Override
	public boolean hasProbabilityPrediction() {
		return true;
	}
	@Override
	public boolean hasMetadataPredictor() {
		return false;
	}
	@Override
	public boolean hasBiasPredictor() {
		return true;
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
	public void updateItemProbabilityVector(
			UserTrainEvent gamma, UserProfile oldUserProfile,
			long itemId, String rating) {
		baseModel.updateItemProbabilityVector(gamma, oldUserProfile, itemId, rating);
		
	}
	@Override
	public void setLearningRateStrategy(LearningRateStrategy strategy) {
	
		
	}

}
