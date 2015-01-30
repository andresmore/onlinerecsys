package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashMap;
import java.util.Set;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.metadata.SlidingWindowCountMinSketch;
import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;


/**
 * Model that blends a single-update probability hypothesis and metadata training for online training and prediction on an exponential weighted predictor
 * @author Andres M
 *
 */
public  class ProbabilityMetadataModelPredictor implements UserModelTrainerPredictor {
	
	
	
	private static final int NUM_PREDICTORS = 2;
	private FactorUserItemRepresentation modelRepresentation;
	private BaseModelPredictor baseModel;
	private MetadataPredictor metadataModel;
	private double lossNormalization;
	
	public ProbabilityMetadataModelPredictor(BaseModelPredictor baseModel, MetadataPredictor metadataModel){
		this.baseModel= baseModel;
		this.metadataModel= metadataModel;
	}
	public ProbabilityMetadataModelPredictor(FactorUserItemRepresentation representation){
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
		Prediction prediction2=metadataModel.calculatePrediction(event, minTrains);
		if(prediction1.isNoPrediction() || prediction2.isNoPrediction())
			return prediction1;
		
		UserProfile profile=modelRepresentation.getPrivateUserProfile(userId);
		Vector hyperparameters=profile.getHyperParameters();
		int numTrains=profile.getNumTrains();
		double weightHyperParam1 = calculateWeightFromCumulativeLosses(hyperparameters.get(0),numTrains);
		double weightHyperParam2 = calculateWeightFromCumulativeLosses(hyperparameters.get(1),numTrains);
		
		
		double sumWeights=weightHyperParam1+weightHyperParam2;
		
		
		double finalPrediction=(weightHyperParam1*prediction1.getPredictionValue()+weightHyperParam2*prediction2.getPredictionValue());
		
		
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
		
		
		
		
		return null;
	}
	@Override
	public Vector calculatehyperParamsUpdate(UserTrainEvent event,Vector itemVector,
			HashMap<String, Vector> trainedProfiles,
			HashMap<String, BetaDistribution> biasVector, Vector hyperparameters, int numTrains) {

		double rating= Double.parseDouble(event.getRating());
		double prediction1=baseModel.calculatePrediction(itemVector,trainedProfiles);
		
		double prediction2=-1;
		try {
			Prediction prediction2Pr=metadataModel.calculatePrediction(event,0);
			if(!prediction2Pr.isNoPrediction()){
				prediction2=prediction2Pr.getPredictionValue();
			}else{
				System.out.println("rare.....");
			}
			
		} catch (TasteException e) {
			
			e.printStackTrace();
		}
		
		
		double weightHyperParam1 = calculateWeightFromCumulativeLosses(hyperparameters.get(0),numTrains);
		double weightHyperParam2 = calculateWeightFromCumulativeLosses(hyperparameters.get(1),numTrains);
		
		
		double sumWeights=weightHyperParam1+weightHyperParam2;
		
		
		double finalPrediction=(weightHyperParam1*prediction1+weightHyperParam2*prediction2);
		
		
		finalPrediction/=sumWeights;
		
		
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
		return "ProbabilityMetadataModelPredictor ("+baseModel.toString()+","+metadataModel.toString()+")" ;
	}
	
	
	@Override
	public UserMetadataInfo calculateMetadataUpdate(UserTrainEvent event,
			 UserMetadataInfo trainedMetadataProfiles,int numTrains) {
		
		return metadataModel.calculateMetadataUpdate(event, trainedMetadataProfiles, numTrains);
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
		return true;
	}
	@Override
	public boolean hasBiasPredictor() {
		return false;
	}
	@Override
	public boolean hasUserHistory() {
		
		return false;
	}
	@Override
	public boolean saveItemMetadata() {
		
		return false;
	}
	@Override
	public void updateItemProbabilityVector(
			UserTrainEvent gamma, UserProfile oldUserProfile,
			long itemId, String rating) {
		
		baseModel.updateItemProbabilityVector(gamma, oldUserProfile, itemId, rating);
	}
	
	@Override
	
	public void setLearningRateStrategy(LearningRateStrategy strategy) {
		baseModel.setLearningRateStrategy(strategy);
		metadataModel.setLearningRateStrategy(strategy);
		
	}
	
	public void setBaseModelLearningRateStrategy(LearningRateStrategy strategy) {
		baseModel.setLearningRateStrategy(strategy);
		
	}
	
	public void setMetadataModelModelLearningRateStrategy(LearningRateStrategy strategy) {
		metadataModel.setLearningRateStrategy(strategy);
		
	}
	
	public Vector calculateRegretBaseExpert() throws TasteException{
		
		Vector vec= new DenseVector(this.getHyperParametersSize());
		Set<Long> userIds=this.modelRepresentation.getUsersId();
		for (Long userId : userIds) {
			UserProfile user=this.modelRepresentation.getPrivateUserProfile(userId);
			vec=vec.plus(user.getHyperParameters());
		}
		
		return vec;
		
	}
	@Override
	public SlidingWindowCountMinSketch buildMetadataSketch() {
		// TODO Auto-generated method stub
		return metadataModel.buildMetadataSketch();
	}

}
