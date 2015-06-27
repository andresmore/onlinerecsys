package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashMap;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.Vector;
import org.uncommons.maths.random.XORShiftRNG;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.VectorProjector;
import edu.uniandes.privateRecsys.onlineRecommender.utils.PrivateRandomUtils;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class ContinualDifferentialPrivacyAggregator implements IUserMaskingStrategy {


	private double sigma=0;

	private int T;
	private double lapDesv;

	public ContinualDifferentialPrivacyAggregator(double sigma, int T) {
		this.sigma=sigma;
		this.T=T;
	    this.lapDesv=(1+(Math.log(T)/Math.log(2)))/sigma;
	}
	

	/* (non-Javadoc)
	 * @see edu.uniandes.privateRecsys.onlineRecommender.IUserItemAggregator#aggregateEvent(edu.uniandes.privateRecsys.onlineRecommender.vo.EventVO, edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation)
	 */
	@Override
	public UserProfile maskProfile(UserProfile oldProfile,UserTrainEvent event,
			FactorUserItemRepresentation userItemRep) throws TasteException {
		
		
		long userId=event.getUserId();
		
		int numNoises=(int) (Math.log(T)/Math.log(2));
		
		int numTrains=userItemRep.getNumberTrainsUser(userId)+1;
		
		UserProfile privateUserProfile=userItemRep.getPrivateUserProfile(userId);
		if(numTrains>=T)
			throw new TasteException("Privacy budget exeeded, not able to continue");
		
		String[] ratingScale=userItemRep.getRatingScale().getScale();
		HashMap<String, Vector> trainedProfiles= new HashMap<>();
		for (int i = 0; i < ratingScale.length; i++) {
			Vector privateVector=privateUserProfile.getProfileForScale(ratingScale[i]);
			Vector randVector=privateVector.like();
			for (int j = 0; j < numNoises; j++) {
				randVector.plus(PrivateRandomUtils.laplaceRandom(0, sigma, privateVector.size()));
			}
			
			Vector noiseVector=privateVector.plus(randVector) ;
			trainedProfiles.put(ratingScale[i],noiseVector);
			
		} 
		trainedProfiles=VectorProjector.projectUserProfileIntoSimplex(trainedProfiles, userItemRep.getRatingScale().getScale(), userItemRep.getfDimensions());
		userItemRep.updatePublicTrainedProfile(userId,trainedProfiles);
		return userItemRep.getPublicUserProfile(userId);
		
	}

}
