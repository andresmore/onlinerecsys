package edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.UserMetadataInfo;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;


public interface FactorUserItemRepresentation {

	UserProfile getPrivateUserProfile(long userId) throws TasteException;

	ItemProfile getPrivateItemProfile(long itemId) throws TasteException;
	
	RatingScale getRatingScale();
	
	public int getNumberTrainsUser(long userId);
	public int getNumberTrainsItem(long itemId);

	void updatePrivateTrainedProfile(long userId,
			HashMap<String, Vector> trainedProfiles, HashMap<String, BetaDistribution> bias, Vector hyperParameters, UserMetadataInfo info) throws TasteException;

	UserProfile getPublicUserProfile(long userId) throws TasteException;

	void updatePublicTrainedProfile(long userId,
			HashMap<String, Vector> trainedProfiles) throws TasteException;

	void updateItemVector(long itemId,Vector itemVector) throws TasteException;
	
	public int getfDimensions();
	
	public boolean hasPrivateStrategy();

	 Object blockUser(long userId);

	Object blockItem(long itemId);


	void setRestrictUsers(HashSet<Long> restrictedUserIds);

	Set<Long> getItemsId(int minTrains);
	Set<Long> getUsersId();

	double getNumberTrainsItems();

	void addUserEvent(long userId, long itemId, String rating);

	void saveItemMetadata(long itemId, String linkedList);

	

}
