package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashMap;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class NoPrivacyAggregator implements IUserItemAggregator {

	

	public NoPrivacyAggregator() {
		
	}

	/* (non-Javadoc)
	 * @see edu.uniandes.privateRecsys.onlineRecommender.IUserItemAggregator#aggregateEvent(edu.uniandes.privateRecsys.onlineRecommender.vo.EventVO, edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation)
	 */
	@Override
	public void aggregateEvent(UserTrainEvent event,
			FactorUserItemRepresentation userItemRep, UserProfile userPrivateProfile) throws TasteException {
		
		long itemId=event.getItemId();
		long userId=event.getUserId();
		String rating=event.getRating();
		long time=event.getTime();
		
		
		
		
		
		
		String[] ratingScale=userItemRep.getRatingScale().getScale();
		HashMap<String, Vector> trainedProfiles= new HashMap<>();
		for (int i = 0; i < ratingScale.length; i++) {
			
			trainedProfiles.put(ratingScale[i], userPrivateProfile.getProfileForScale(ratingScale[i]));
			
		} 
		userItemRep.updatePublicTrainedProfile(userId,trainedProfiles);
		
		
	}

}
