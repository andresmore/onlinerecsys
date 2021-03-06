package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashMap;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FullRunningAverage;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.metadata.SlidingWindowCountMinSketch;
import edu.uniandes.privateRecsys.onlineRecommender.utils.PrivateRandomUtils;
import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;
/**
 * Class that trains and samples a bayes average model for the user
 * @author Andres M
 *
 */
public class BayesAveragePredictor implements UserModelTrainerPredictor {

	private FactorUserItemRepresentation model;

	@Override
	public void setModelRepresentation(FactorUserItemRepresentation model) {
		this.model=model;

	}

	@Override
public  Prediction calculatePrediction(UserTrainEvent event, int minTrains) throws TasteException{
		
		long userId=event.getUserId();
		long itemId=event.getItemId();
		UserProfile profile=model.getPrivateUserProfile(userId);
		HashMap<String, BetaDistribution> dist=profile.getUserBias();
		double avgModelEstimation=0;
		double sumSamples=0;
		for (String scale : dist.keySet()) {
			long initTime=System.nanoTime();
			//System.out.println(Thread.currentThread()+"Sampling ");
			double sample=dist.get(scale).sample();
			//System.out.println(Thread.currentThread()+"Sampled "+(System.nanoTime()-initTime));
			avgModelEstimation+=sample*Double.parseDouble(scale);
			sumSamples+=sample;
		}
		return Prediction.createPrediction(userId, itemId, avgModelEstimation/sumSamples);
	}

	@Override
	public HashMap<String, Vector> calculateProbabilityUpdate(UserTrainEvent event,
			String rating, Vector itemVector, UserProfile oldUserPrivate,
			String[] ratingScale) {
		
		return oldUserPrivate.getUserProfiles();
	}

	@Override
	public HashMap<String, BetaDistribution> calculatePriorsUpdate(
			UserTrainEvent event, HashMap<String, BetaDistribution> biasVector,
			String[] ratingScale) {
		HashMap<String, BetaDistribution> ret= new HashMap<String, BetaDistribution>();
		for (int i = 0; i < ratingScale.length; i++) {
			BetaDistribution dist=biasVector.get(ratingScale[i]);
			if(event.getRating().equals(ratingScale[i])){
				ret.put(ratingScale[i], new BetaDistribution(PrivateRandomUtils.getCurrentRandomGenerator(),dist.getAlpha()+1, dist.getBeta(),BetaDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY));
			}else{
				ret.put(ratingScale[i], new BetaDistribution(PrivateRandomUtils.getCurrentRandomGenerator(),dist.getAlpha(), dist.getBeta()+1,BetaDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY));
			}
				
		}
		return ret;
	}

	@Override
	public Vector calculatehyperParamsUpdate(UserTrainEvent event,Vector itemVector,
			HashMap<String, Vector> trainedProfiles,
			HashMap<String, BetaDistribution> biasVector,
			Vector oldHyperparameters, int numTrains) {
	
		return oldHyperparameters;
	}

	@Override
	public int getHyperParametersSize() {
		
		return 0;
	}
	
	/**
	 * Testing betapriors
	 * @param args
	 */
	public static void main(String[] args) {
		
		BetaDistribution[] arr= new BetaDistribution[5];
		for (int i = 0; i < arr.length; i++) {
			arr[i]= new  BetaDistribution(1,1);
		}
		int TRIES=100;
		//XORShiftRNG genRand= new XORShiftRNG();
		NormalDistribution normal= new NormalDistribution(5, 1);
		
		FullRunningAverage fullrun= new FullRunningAverage();
		for (int i = 0; i < TRIES; i++) {
			//int choice=genRand.nextInt(arr.length);
			int choice=(int) Math.floor(Math.max(0, Math.min(normal.sample(), arr.length-1)));
			fullrun.addDatum(choice+1);
			arr[choice]=new BetaDistribution(arr[choice].getAlpha()+1, arr[choice].getBeta());
			for (int j = 0; j < arr.length; j++) {
				if(choice!=j)
					arr[j]= new  BetaDistribution(arr[j].getAlpha(), arr[j].getBeta()+1);
			}
			String sysout="choice["+(choice+1)+"]"+"avg["+fullrun.getAverage()+"]"+'\t';
			double avgModelEstimation=0;
			double sumSamples=0;
			for (int j = 0; j < arr.length; j++) {
				double sample=arr[j].sample();
				sumSamples+=sample;
				avgModelEstimation+=(j+1)*sample;
				sysout+=sample+"	";
			}
			avgModelEstimation/=sumSamples;
			System.out.println(sysout);
			System.out.println("Model estimation "+avgModelEstimation);
			
		}
		
		String finalDist="";
		for (int i = 0; i < arr.length; i++) {
			finalDist+="("+arr[i].getAlpha()+","+arr[i].getBeta()+")"+'\t';
		}
		System.out.println(finalDist);
		
		
		
		
	
		
		
		
	}
	
	@Override
	public String toString(){
		return "BayesAveragePredictor";
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
		return false;
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
		
		
	}

	@Override
	public void setLearningRateStrategy(LearningRateStrategy strategy) {
			
	}

	@Override
	/**
	 * Doesn't handle metadata
	 */
	public SlidingWindowCountMinSketch buildMetadataSketch() {
	
		return null;
	}

	

}
