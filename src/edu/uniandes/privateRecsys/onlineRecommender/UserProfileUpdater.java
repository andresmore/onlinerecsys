package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashMap;
import java.util.logging.Logger;

import org.apache.commons.math.distribution.BetaDistribution;
import org.apache.commons.math.distribution.BetaDistributionImpl;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.Vector;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.ItemProfile;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserModelTrainerPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.VectorProjector;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class UserProfileUpdater implements IUserProfileUpdater {

	private final static Logger LOG = Logger.getLogger(UserProfileUpdater.class
		      .getName());
	private UserModelTrainerPredictor predictor;
	
	
	public UserProfileUpdater(UserModelTrainerPredictor predictor){
		this.predictor=predictor;
	}
	
	/* (non-Javadoc)
	 * @see edu.uniandes.privateRecsys.onlineRecommender.IUserProfileUpdater#processEvent(edu.uniandes.privateRecsys.onlineRecommender.vo.EventVO, edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation)
	 */
	@Override
	public UserProfile processEvent(UserTrainEvent event,
			FactorUserItemRepresentation userItemRep,double gamma) throws TasteException {
		
		long itemId=event.getItemId();
		long userId=event.getUserId();
		String rating=event.getRating();
		
		
		
		ItemProfile itemProfile=userItemRep.getPrivateItemProfile(itemId);
		Vector itemVector = itemProfile.getVector();
		UserProfile oldUserPrivate=userItemRep.getPrivateUserProfile(userId);
		HashMap<String, BetaDistribution> biasVector=oldUserPrivate.getUserBias();
		Vector hyperParameterVector= oldUserPrivate.getHyperParameters();
		
		if (oldUserPrivate != null) {
			double initPrediction = calculatePrediction(itemVector,
					oldUserPrivate.getUserProfiles(), userItemRep
							.getRatingScale().getScale());
			
			double initDistance=Math.abs(Double.parseDouble(rating)-initPrediction);
			int numTrains = userItemRep.getNumberTrainsUser(userId) + 1;

			String[] ratingScale = userItemRep.getRatingScale().getScale();
			HashMap<String, Vector> trainedProfiles = predictor.calculateProbabilityUpdate(
					gamma, rating, itemVector, oldUserPrivate, ratingScale);
			biasVector = predictor.calculatePriorsUpdate(event, biasVector, ratingScale);
			hyperParameterVector=predictor.calculatehyperParamsUpdate(event,trainedProfiles,biasVector,hyperParameterVector);
			userItemRep.updatePrivateTrainedProfile(userId, trainedProfiles,
					biasVector);
			double endPrediction = calculatePrediction(itemVector,
					trainedProfiles, userItemRep.getRatingScale().getScale());
			double endDistance=Math.abs(Double.parseDouble(rating)-endPrediction);
			
			if(initDistance<endDistance)
				LOG.fine("WARN: distance incremented "+rating+ " "+initPrediction+" "+endPrediction);
			// System.out.println("UserUpdater: Train was "+event.getRating()+", initPrediction="+initPrediction+", endPrediction="+endPrediction);
			LOG.fine("UserUpdater: Train was" + event.getRating()
					+ ", initPrediction=" + initPrediction + ", endPrediction="
					+ endPrediction);

		}
		return oldUserPrivate;
		
		
		
		
	}
	
	private HashMap<String, BetaDistribution> updatePriors(
			UserTrainEvent event, HashMap<String, BetaDistribution> biasVector,
			String[] ratingScale) {
		HashMap<String, BetaDistribution> ret= new HashMap<String, BetaDistribution>();
		for (int i = 0; i < ratingScale.length; i++) {
			BetaDistribution dist=biasVector.get(ratingScale[i]);
			if(event.getRating().equals(ratingScale[i])){
				ret.put(ratingScale[i], new BetaDistributionImpl(dist.getAlpha()+1, dist.getBeta()));
			}else{
				ret.put(ratingScale[i], new BetaDistributionImpl(dist.getAlpha(), dist.getBeta()+1));
			}
				
		}
		return ret;
	}
	private HashMap<String, Vector> updateProbabilityHypothesis(double gamma,
			String rating, Vector itemVector, UserProfile oldUserPrivate,
			String[] ratingScale) {
		HashMap<String, Vector> trainedProfiles= new HashMap<>();
		
		
		for (int i = 0; i < ratingScale.length; i++) {	
			Vector privateVector=oldUserPrivate.getProfileForScale(ratingScale[i]);
			int prob=ratingScale[i].equals(rating)?1:0;
			
			double dotProb=privateVector.dot(itemVector);
			
			double error=prob-dotProb;
			double multiplicator=gamma*(error);
			Vector privateVectorMult=itemVector.times(multiplicator);
			Vector result=privateVector.plus(privateVectorMult);
			
			trainedProfiles.put(ratingScale[i], result);
			
			
		}
		
		trainedProfiles=VectorProjector.projectUserProfileIntoSimplex(trainedProfiles,ratingScale, itemVector.size());
		return trainedProfiles;
	}
	public double calculatePrediction(Vector itemVector,HashMap<String, Vector> trainedProfiles, String[] ratingScale){
		double prediction=0;
	
		
		
		for (int i = 0; i < ratingScale.length; i++) {
			Vector userVector = trainedProfiles.get(ratingScale[i]);
			double dot = userVector.dot(itemVector);
		
			prediction += dot * Double.parseDouble(ratingScale[i]);
			
		}
		
		return prediction;
	}
}
