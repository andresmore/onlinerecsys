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

public class DifferentialPrivacyAggregator implements IUserItemAggregator {

	
	private double sigma=0;
	private XORShiftRNG rand=new XORShiftRNG();

	public DifferentialPrivacyAggregator( double epsilon) {
		
		this.sigma=1/epsilon;
	}
	

	/* (non-Javadoc)
	 * @see edu.uniandes.privateRecsys.onlineRecommender.IUserItemAggregator#aggregateEvent(edu.uniandes.privateRecsys.onlineRecommender.vo.EventVO, edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation)
	 */
	@Override
	public UserProfile aggregateEvent(UserProfile oldProfile,UserTrainEvent event,
			FactorUserItemRepresentation userItemRep) throws TasteException {
		
		long itemId=event.getItemId();
		long userId=event.getUserId();
		String rating=event.getRating();
		long time=event.getTime();
		
		UserProfile privateUserProfile=userItemRep.getPrivateUserProfile(userId);
		
		
		
		String[] ratingScale=userItemRep.getRatingScale().getScale();
		HashMap<String, Vector> trainedProfiles= new HashMap<>();
		for (int i = 0; i < ratingScale.length; i++) {
			Vector privateVector=privateUserProfile.getProfileForScale(ratingScale[i]);
			Vector randVector=PrivateRandomUtils.laplaceRandom(this.rand,0, sigma, privateVector.size());
			Vector noiseVector=privateVector.plus(randVector) ;
			trainedProfiles.put(ratingScale[i],noiseVector);
			
		} 
		trainedProfiles=VectorProjector.projectUserProfileIntoSimplex(trainedProfiles, userItemRep.getRatingScale().getScale(), userItemRep.getfDimensions());
		userItemRep.updatePublicTrainedProfile(userId,trainedProfiles);
		
		return userItemRep.getPublicUserProfile(userId);
		
	}

}
