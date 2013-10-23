package edu.uniandes.privateRecsys.onlineRecommender;

import org.apache.mahout.cf.taste.common.TasteException;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.FactorUserItemRepresentation;
import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.UserProfile;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public interface IItemProfileUpdater {

	public abstract void processEvent(UserTrainEvent event,
			FactorUserItemRepresentation userItemRep, double gamma, UserProfile oldUserProfile) throws TasteException;

}