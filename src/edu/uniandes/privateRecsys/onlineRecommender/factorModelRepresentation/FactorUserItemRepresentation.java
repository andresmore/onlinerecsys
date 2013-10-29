package edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;


public interface FactorUserItemRepresentation {

	UserProfile getPrivateUserProfile(long userId) throws TasteException;

	ItemProfile getPrivateItemProfile(long itemId) throws TasteException;
	
	RatingScale getRatingScale();
	
	public int getNumberTrainsUser(long userId);
	public int getNumberTrainsItem(long itemId);

	void updatePrivateTrainedProfile(long userId,
			HashMap<String, Vector> trainedProfiles, Vector bias) throws TasteException;

	UserProfile getPublicUserProfile(long userId) throws TasteException;

	void updatePublicTrainedProfile(long userId,
			HashMap<String, Vector> trainedProfiles) throws TasteException;

	void updateItemVector(long itemId,Vector itemVector) throws TasteException;
	
	public int getfDimensions();

	 Object blockUser(long userId);

	Object blockItem(long itemId);

	Prediction calculatePrediction(long itemId, long userId, int minTrains) throws TasteException;

	void setRestrictUsers(HashSet<Long> restrictedUserIds);

	Set<Long> getItemsId(int minTrains);
	Set<Long> getUsersId();

	

}
