package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashMap;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FullRunningAverage;
import org.apache.mahout.math.Vector;
import org.uncommons.maths.random.XORShiftRNG;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.utils.PrivateRandomUtils;
import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;
/**
 * Class that trains a beta distribution for each rating, for prediction uses the numerical mean of the distribution
 * @author Andres M
 *
 */
public class SimpleAveragePredictor implements UserModelTrainerPredictor {

	private FactorUserItemRepresentation model;

	@Override
	public void setModelRepresentation(FactorUserItemRepresentation model) {
		this.model=model;

	}

	/**
	 * No sampling
	 */
	@Override
	public Prediction calculatePrediction(long itemId, long userId,
			int minTrains) throws TasteException {
		UserProfile profile=model.getPrivateUserProfile(userId);
		HashMap<String, BetaDistribution> dist=profile.getUserBias();
		double prediction=this.calculatePrediction(dist);
		return Prediction.createPrediction(userId, itemId, prediction);
	}

	@Override
	public HashMap<String, Vector> calculateProbabilityUpdate(double gamma,
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
	public Vector calculatehyperParamsUpdate(double gamma,UserTrainEvent event,Vector itemVector,
			HashMap<String, Vector> trainedProfiles,
			HashMap<String, BetaDistribution> biasVector,
			Vector oldHyperparameters, int numTrains) {
	
		return oldHyperparameters;
	}

	@Override
	public int getHyperParametersSize() {
		
		return 0;
	}
	
	

	public double calculatePrediction(
			HashMap<String, BetaDistribution> biasVector) {
		double avgModelEstimation=0;
		double sumSamples=0;
		for (String scale : biasVector.keySet()) {
			
			//System.out.println(Thread.currentThread()+"Sampling ");
			double sample=biasVector.get(scale).getNumericalMean();
			//System.out.println(Thread.currentThread()+"Sampled "+(System.nanoTime()-initTime));
			avgModelEstimation+=sample*Double.parseDouble(scale);
			sumSamples+=sample;
		}
		return avgModelEstimation/sumSamples;
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
	public UserMetadataInfo calculateMetadataUpdate(UserTrainEvent event,
			double gamma, UserMetadataInfo trainedMetadataProfiles) {
		// TODO Auto-generated method stub
		return null;
	}


}
