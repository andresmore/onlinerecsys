package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashMap;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.VectorProjector;
import edu.uniandes.privateRecsys.onlineRecommender.metadata.SlidingWindowCountMinSketch;
import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;


/**
 * Model that creates a join probability distribution from the base and average beta estimators for online training
 * @author Andres M
 *
 */
public  class LogCombinationPredictor implements UserModelTrainerPredictor {
	
	
	
	private FactorUserItemRepresentation modelRepresentation;
	private BaseModelPredictorWithItemRegularizationUpdate baseModel;
	private SimpleAveragePredictor averageModel;

	
	public LogCombinationPredictor(BaseModelPredictorWithItemRegularizationUpdate base,SimpleAveragePredictor average ){
		this.baseModel= base;
		this.averageModel= average;
	}
	public LogCombinationPredictor(FactorUserItemRepresentation representation){
		this.setModelRepresentation(representation);
	}
	
	
	
	/* (non-Javadoc)
	 * @see edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.ModelPredictor#calculatePrediction(long, long, int)
	 */
	@Override
	public  Prediction calculatePrediction(UserTrainEvent event, int minTrains) throws TasteException{
		
		long userId=event.getUserId();
		long itemId=event.getItemId();
		
		UserProfile profile= modelRepresentation.getPrivateUserProfile(userId);
		Vector itemVector=modelRepresentation.getPrivateItemProfile(itemId).getProbabilityVector();
		HashMap<String,Vector> trainedProfiles= profile.getUserProfiles();
		HashMap<String, BetaDistribution> dist=profile.getUserBias();
		
		String[] ratingScale=this.modelRepresentation.getRatingScale().getScale();
		double constantAvg = 0;
		for (int i = 0; i < ratingScale.length; i++) {
			double probAverage = dist.get(ratingScale[i]).getNumericalMean();
			constantAvg += probAverage;
		}
		double sumProd=0;
		double prediction=0;
		double sumDots=0;
		for (int i = 0; i < ratingScale.length; i++) {
			Vector userVector = trainedProfiles
					.get(ratingScale[i]);
			double probModel = userVector.dot(itemVector);
			sumDots+=probModel;
			double probAverage=dist.get(ratingScale[i]).getNumericalMean()/constantAvg;
			sumProd+=probAverage*probModel;
			prediction+=probAverage*probModel*modelRepresentation.getRatingScale().scaleAsValues()[i];
		}
		
		
		
		
		
		
		return Prediction.createPrediction(userId, itemId, prediction/sumProd);
		
	}
	
	
	
	
	@Override
	public void setModelRepresentation(FactorUserItemRepresentation model) {
		this.modelRepresentation=model;
		this.baseModel.setModelRepresentation(model);
		this.averageModel.setModelRepresentation(model);
		
		
		
		
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

		
		return null;
	}
	@Override
	public int getHyperParametersSize() {
		
		return 0;
	}
	
	@Override
	public String toString(){
		return "LogCombinationPredictor "+this.baseModel.toString();
	}
	
	
	@Override
	public UserMetadataInfo calculateMetadataUpdate(UserTrainEvent event,
			 UserMetadataInfo trainedMetadataProfiles,int numTrains) {
		
		return null;
	}
	@Override
	public boolean hasHyperParameters() {
		return false;
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
				
	}
	/**
	 * Doesn't handle metadata
	 */
	@Override
	public SlidingWindowCountMinSketch buildMetadataSketch() {
		
		return null;
	}
	public BaseModelPredictorWithItemRegularizationUpdate getBaseModel() {
		return baseModel;
	}
	public SimpleAveragePredictor getAverageModel() {
		return averageModel;
	}

}
