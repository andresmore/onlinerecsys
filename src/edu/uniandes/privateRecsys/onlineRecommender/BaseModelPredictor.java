package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashMap;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.ItemProfile;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.VectorProjector;
import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;


/**
 * Model that uses only the probability hypothesis for online training and prediction
 * @author Andres M
 *
 */
public  class BaseModelPredictor implements UserModelTrainerPredictor {
	
	private FactorUserItemRepresentation modelRepresentation;
	
	public BaseModelPredictor(){
		
	}
	public BaseModelPredictor(FactorUserItemRepresentation representation){
		this.modelRepresentation=representation;
	}
	
	
	/* (non-Javadoc)
	 * @see edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.ModelPredictor#calculatePrediction(long, long, int)
	 */
	@Override
	public  Prediction calculatePrediction(UserTrainEvent event, int minTrains) throws TasteException{
		
		long userId=event.getUserId();
		long itemId=event.getItemId();
		UserProfile user = modelRepresentation
				.getPrivateUserProfile(userId);
		ItemProfile item = modelRepresentation
				.getPrivateItemProfile(itemId);
		
		int numTrainsItem=modelRepresentation.getNumberTrainsItem(itemId);
		int numTrainsUser=modelRepresentation.getNumberTrainsUser(userId);
		if (numTrainsUser < minTrains){
		
			return Prediction.createNoAblePrediction(userId,itemId);
		}
		else {
			
			double prediction=0;
			
			if (item != null && user != null) {
				Vector itemVector = item.getProbabilityVector();
				if(numTrainsItem<minTrains){
					//Equiprobable vector
						itemVector=itemVector.assign(1);
						itemVector=VectorProjector.projectVectorIntoSimplex(itemVector);
						
				}
				prediction=this.calculatePrediction(itemVector, user.getUserProfiles());
				
			}
			user=null;
			item=null;
			
			return Prediction.createPrediction(userId,itemId,prediction);
		}
		
		
	}
	@Override
	public void setModelRepresentation(FactorUserItemRepresentation model) {
		this.modelRepresentation=model;
		
	}
	@Override
	public HashMap<String, Vector> calculateProbabilityUpdate(double gamma,
			String rating, Vector itemVector, UserProfile oldUserPrivate,
			String[] ratingScale) {
		
			
		HashMap<String, Vector> trainedProfiles= new HashMap<>();
		
		
		for (int i = 0; i < ratingScale.length; i++) {	
			Vector privateVector=oldUserPrivate.getProfileForScale(ratingScale[i]);
			int prob=ratingScale[i].equals(rating)?1:0;
			
			double dotProb=privateVector.dot(itemVector);
			
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
		
		
		return null;
	}
	@Override
	public Vector calculatehyperParamsUpdate(double gamma,UserTrainEvent event,Vector itemVector,
			HashMap<String, Vector> trainedProfiles,
			HashMap<String, BetaDistribution> biasVector, Vector hyperparameters, int numTrains) {
		
		return null;
	}
	@Override
	public int getHyperParametersSize() {
		
		return 0;
	}
	public double calculatePrediction(Vector itemVector,
			HashMap<String, Vector> trainedProfiles) {
		String[] ratingScale=this.modelRepresentation.getRatingScale().getScale();
		double prediction=0;
		//Vector dotVector= new DenseVector(this.modelRepresentation.getRatingScale().getRatingSize());
		for (int i = 0; i < ratingScale.length; i++) {
			Vector userVector = trainedProfiles
					.get(ratingScale[i]);
			double dot = userVector.dot(itemVector);
			
			prediction += dot * Double.parseDouble(ratingScale[i]);
			//dotVector.setQuick(i, dot);
		}
		return prediction;
	}
	
	
	@Override
	public String toString(){
		return "BaseModelPredictor";
	}
	
	@Override
	public UserMetadataInfo calculateMetadataUpdate(UserTrainEvent event,
			double gamma, UserMetadataInfo trainedMetadataProfiles,int numTrains) {
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
	
	
}
