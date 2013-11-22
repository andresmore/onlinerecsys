package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashMap;
import java.util.logging.Logger;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.ItemProfile;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
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
			
			
			
			

			String[] ratingScale = userItemRep.getRatingScale().getScale();
			
			hyperParameterVector=predictor.calculatehyperParamsUpdate(gamma,event,itemVector, oldUserPrivate.getUserProfiles(),biasVector,hyperParameterVector,oldUserPrivate.getNumTrains()+1);
			HashMap<String, Vector> trainedProfiles = predictor.calculateProbabilityUpdate(
					gamma, rating, itemVector, oldUserPrivate, ratingScale);
			biasVector = predictor.calculatePriorsUpdate(event, biasVector, ratingScale);
		
			userItemRep.updatePrivateTrainedProfile(userId, trainedProfiles,
					biasVector,hyperParameterVector);
		
			
			
			// System.out.println("UserUpdater: Train was "+event.getRating()+", initPrediction="+initPrediction+", endPrediction="+endPrediction);
			
		}
		return oldUserPrivate;
		
		
		
		
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
