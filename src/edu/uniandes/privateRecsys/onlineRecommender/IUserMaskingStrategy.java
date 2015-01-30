package edu.uniandes.privateRecsys.onlineRecommender;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public interface IUserMaskingStrategy {

	public abstract UserProfile maskProfile(UserProfile user, UserTrainEvent event,
			FactorUserItemRepresentation userItemRep) throws TasteException;

}