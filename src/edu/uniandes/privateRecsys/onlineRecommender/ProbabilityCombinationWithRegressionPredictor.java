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
 * Model that creates a join probability distribution from the base and average beta estimators for online training and prediction
 * @author Andres M
 *
 */
public  class ProbabilityCombinationWithRegressionPredictor implements UserModelTrainerPredictor {
	
	
	
	private FactorUserItemRepresentation modelRepresentation;
	private BaseModelPredictorWithItemRegularizationUpdate baseModel;
	private SimpleAveragePredictor averageModel;
	private LearningRateStrategy learningRateStrategy;

	
	public ProbabilityCombinationWithRegressionPredictor(BaseModelPredictorWithItemRegularizationUpdate base,SimpleAveragePredictor average ){
		this.baseModel= base;
		this.averageModel= average;
	}
	public ProbabilityCombinationWithRegressionPredictor(FactorUserItemRepresentation representation){
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
		HashMap<String, Double> joinDistribution=getJoinDistribution(trainedProfiles,dist,itemVector);
		String[] ratingScale=this.modelRepresentation.getRatingScale().getScale();
		
		double prediction=0;
		
		for (int i = 0; i < ratingScale.length; i++) {
			
			prediction+=joinDistribution.get(ratingScale[i])*modelRepresentation.getRatingScale().scaleAsValues()[i];
		}
	
		
		return Prediction.createPrediction(userId, itemId, prediction);
		
	}
	
	private HashMap<String, Double> getJoinDistribution(
			HashMap<String, Vector> trainedProfiles,
			HashMap<String, BetaDistribution> dist, Vector itemVector) {
		
		
		String[] ratingScale=this.modelRepresentation.getRatingScale().getScale();
		
		HashMap<String, Double> distributionAvg = getDistributionFromAverageBetaDistribution(dist,
				ratingScale);
	
		double sumProd=0;
		HashMap<String, Double> ret= new HashMap<String, Double>();
		for (int i = 0; i < ratingScale.length; i++) {
			Vector userVector = trainedProfiles
					.get(ratingScale[i]);
			double probModel = userVector.dot(itemVector);
		
			double probAverage=distributionAvg.get(ratingScale[i]);
			
			double pow = probAverage*probModel;
			ret.put(ratingScale[i], pow);
			sumProd+=pow;
		}
		for (String key : ratingScale) {
			ret.put(key, ret.get(key)/sumProd);
		}
		
		return ret;
	}
	public HashMap<String, Double> getDistributionFromAverageBetaDistribution(
			HashMap<String, BetaDistribution> dist, String[] ratingScale) {
		HashMap<String, Double> retDistrib= new HashMap<>();
		double constantAvg = 0;
		
		for (int i = 0; i < ratingScale.length; i++) {
			double probAverage = dist.get(ratingScale[i]).getNumericalMean();
			retDistrib.put(ratingScale[i],probAverage);
			constantAvg += probAverage;
		}
		for (int i = 0; i < ratingScale.length; i++) {
			retDistrib.put(ratingScale[i],retDistrib.get(ratingScale[i])/constantAvg);
		}
		return retDistrib;
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
		
		
		double gamma=this.learningRateStrategy.getGammaFromK(this.modelRepresentation.getNumberTrainsUser(event.getUserId()));	
		HashMap<String, Vector> trainedProfiles= oldUserPrivate.getUserProfiles();
		
		HashMap<String, BetaDistribution> dist=oldUserPrivate.getUserBias();
		HashMap<String, Double> joinDistribution=getJoinDistribution(trainedProfiles,dist,itemVector);
		
		for (int i = 0; i < ratingScale.length; i++) {	
			Vector privateVector=oldUserPrivate.getProfileForScale(ratingScale[i]);
			int prob=ratingScale[i].equals(rating)?1:0;
			
			double dotProb=joinDistribution.get(ratingScale[i]);
			
			double loss=prob-dotProb;
			
			double multiplicator=gamma*(loss);
			Vector privateVectorMult=itemVector.times(multiplicator);
			Vector result=privateVector.plus(privateVectorMult);
			
			double endDotProb=result.dot(itemVector);
			double stepLoss=prob-endDotProb;
			
			if(Math.abs(stepLoss)>Math.abs(loss)){
				//	System.err.println("Model increased loss");
			}
			trainedProfiles.put(ratingScale[i], result);
			
			
		}
		
		trainedProfiles=VectorProjector.projectUserProfileIntoSimplex(trainedProfiles,ratingScale, itemVector.size());
		
		return trainedProfiles;
		
		
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
		return "ProbabilityCombinationWithRegressionPredictor";
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
		this.learningRateStrategy=strategy;
		baseModel.setLearningRateStrategy(strategy);
				
	}

}
