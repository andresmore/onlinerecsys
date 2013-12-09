package edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.DenseVector;

import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.UserMetadataInfo;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.utils.PrivateRandomUtils;

public class IncrementalFactorUserItemRepresentation implements
		FactorUserItemRepresentation {

	private RatingScale ratingScale;
	private int fDimensions;
	
	private Map<Long, Vector> itemFactors;
	private Map<Long, Vector>[] privateUserFactors;
	private Map<Long, Vector>[] privateUserMetadataFactors;
	private Map<Long, LinkedList<String> > privateUserConcepts;
	private Map<Long, LinkedList<BetaDistribution>> privateUserBias;
	private Map<Long, Vector>[] publicUserFactors;
	private Map<Long, Vector> privateHyperParams;
	
	private ConcurrentHashMap<Long, AtomicInteger> numTrainsUser= new ConcurrentHashMap <>();
	private ConcurrentHashMap <Long, AtomicInteger> numTrainsItem= new ConcurrentHashMap <>();
	
	private AtomicInteger numTrainsItems= new AtomicInteger();
	private boolean hasPrivateInfo;
	private HashSet<Long> restrictedUserIds;
	private int hyperParamDimension;

	public HashSet<Long> getRestrictedUserIds() {
		return restrictedUserIds;
	}

	@SuppressWarnings("unchecked")
	public IncrementalFactorUserItemRepresentation(RatingScale scale, int fDimensions, boolean hasPrivateStrategy, int hyperParameterDimension){
		this.ratingScale=scale;
		this.fDimensions=fDimensions;
		this.hyperParamDimension=hyperParameterDimension;
		this.itemFactors= new ConcurrentHashMap<>();
		this.hasPrivateInfo=hasPrivateStrategy;
		this.privateUserFactors= new ConcurrentHashMap[scale.getRatingSize()];
		this.privateUserMetadataFactors= new ConcurrentHashMap[scale.getRatingSize()];
		this.privateUserConcepts= new ConcurrentHashMap<>();
		
		this.privateUserBias= new ConcurrentHashMap<>();
		this.privateHyperParams= new ConcurrentHashMap<>();
	
		
		for (int i = 0; i < privateUserFactors.length; i++) {
			privateUserFactors[i]= new ConcurrentHashMap<>();
			privateUserMetadataFactors[i]= new ConcurrentHashMap<>();
		}
		
		if(this.hasPrivateInfo){
			this.publicUserFactors= new ConcurrentHashMap[scale.getRatingSize()];
			for (int i = 0; i < publicUserFactors.length; i++) {
				publicUserFactors[i]= new ConcurrentHashMap<>();
			}
		}	
		else{
			this.publicUserFactors= privateUserFactors;
		}
	}
	
	@Override
	public UserProfile getPrivateUserProfile(long userId) throws TasteException {
		if(isAllowed(userId)){
		if(!privateUserFactors[0].containsKey(userId))
			insertUser(userId);
		
		LinkedList<Vector> userVectors= new LinkedList<>();
		LinkedList<Vector> metadataVectors= new LinkedList<>();
		for (int i = 0; i < this.privateUserFactors.length; i++) {
			userVectors.add(this.privateUserFactors[i].get(userId));
			metadataVectors.add(this.privateUserMetadataFactors[i].get(userId));
		}
		
		return UserProfile.buildDenseProfile(userVectors, ratingScale, this.privateUserBias.get(userId),this.privateHyperParams.get(userId), metadataVectors, this.privateUserConcepts.get(userId), this.numTrainsUser.get(userId).get());
		}
		return null;
	}
	
	private void insertUser(long userId) {
		
		String[] scale= this.ratingScale.getScale();
		
		LinkedList<BetaDistribution> userPriors= new LinkedList<>();
		
		HashMap<String, Vector> userProfile= new HashMap<String, Vector>();
		
		for (int i = 0; i < privateUserFactors.length; i++) {
			
			Vector vec=PrivateRandomUtils.normalRandom(0, 1, this.fDimensions);
			userProfile.put(scale[i],vec);
			
			userPriors.add(new BetaDistribution(PrivateRandomUtils.getCurrentRandomGenerator(),1,1,BetaDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY));
		}
		userProfile=VectorProjector.projectUserProfileIntoSimplex(userProfile, scale, this.fDimensions);
		
		this.privateUserBias.put(userId, userPriors);
		if(hyperParamDimension>0){
			Vector userHyperParams= new DenseVector(hyperParamDimension);
			this.privateHyperParams.put(userId, userHyperParams);
		}
		
		this.privateUserConcepts.put(userId, new LinkedList<String>());
		for (int i = 0; i < privateUserFactors.length; i++) {
			privateUserFactors[i].put(userId, userProfile.get(scale[i]));
			Vector privateMetadataVector=new DenseVector(0);
			privateUserMetadataFactors[i].put(userId, privateMetadataVector);
			if(this.hasPrivateInfo){
				publicUserFactors[i].put(userId, userProfile.get(scale[i]));
			}
		}
		
	}

	@Override
	public UserProfile getPublicUserProfile(long userId) throws TasteException {
		if(!hasPrivateInfo)
			return getPrivateUserProfile(userId);
		
		if (isAllowed(userId)) {
			if (!publicUserFactors[0].containsKey(userId))
				insertUser(userId);
			LinkedList<BetaDistribution> dist= new LinkedList<>();
			LinkedList<Vector> userVectors = new LinkedList<>();
			for (int i = 0; i < this.publicUserFactors.length; i++) {
				userVectors.add(this.publicUserFactors[i].get(userId));
				dist.add(new BetaDistribution(PrivateRandomUtils.getCurrentRandomGenerator(),1, 1,BetaDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY));
			}
			Vector emptyHyperParams=null;
			return UserProfile.buildDenseProfile(userVectors, ratingScale,
					dist,emptyHyperParams,null, null, 0);
		}
		return null;
	}

	private boolean isAllowed(long userId) {
		if(this.restrictedUserIds==null)
			return true;
		
		return this.restrictedUserIds.contains(userId);
	}

	@Override
	public ItemProfile getPrivateItemProfile(long itemId) throws TasteException {
		if(!this.itemFactors.containsKey(itemId))
			insertItem(itemId);
		return ItemProfile.buildDenseProfile(this.itemFactors.get(itemId));
		
		
	}

	private void insertItem(long itemId) {
		
		
		Vector vec=PrivateRandomUtils.normalRandom(0, 1, this.fDimensions);
		vec=VectorProjector.projectVectorIntoSimplex(vec);
		
		itemFactors.put(itemId, vec);
	}

	@Override
	public RatingScale getRatingScale() {
		return ratingScale;
	}

	@Override
	public int getNumberTrainsUser(long userId) {
		
		AtomicInteger trains=this.numTrainsUser.get(userId);
		return trains==null?0:trains.get();
	}
	@Override
	public int getNumberTrainsItem(long itemId) {
		AtomicInteger trains=this.numTrainsItem.get(itemId);
		return trains==null?0:trains.get();
	}

	@Override
	public void updatePrivateTrainedProfile(long userId,
			HashMap<String, Vector> trainedProfiles,HashMap<String, BetaDistribution> bias, Vector hyperParams, UserMetadataInfo info) throws TasteException {
		AtomicInteger trains = this.numTrainsUser.get(userId);

		if (trains == null)
			numTrainsUser.put(userId, new AtomicInteger(1));
		else
			trains.incrementAndGet();
		
		String[] scale= this.ratingScale.getScale();
		
		LinkedList<BetaDistribution> userPriors= null;
		
		if(bias!=null)
			userPriors=new LinkedList<BetaDistribution>();
		for (int i = 0; i < privateUserFactors.length; i++) {
			privateUserFactors[i].put(userId, trainedProfiles.get(scale[i]));
			
			if(bias!=null)
				userPriors.add(bias.get(scale[i]));
		}
		if(bias!=null ){
			privateUserBias.put(userId, userPriors);
			
		}	
		if(hyperParams!=null){
			privateHyperParams.put(userId, hyperParams);
		}
		if(info!=null){
			privateUserConcepts.put(userId,info.getIncludedConcepts());
			for (int i = 0; i < scale.length; i++) {
				privateUserMetadataFactors[i].put(userId, info.getTrainedProfiles().get(scale[i]));
			}
		}
	}

	

	@Override
	public void updatePublicTrainedProfile(long userId,
			HashMap<String, Vector> trainedProfiles) throws TasteException {
		
		if(hasPrivateInfo){
			String[] scale= this.ratingScale.getScale();
		
			for (int i = 0; i < publicUserFactors.length; i++) {
				publicUserFactors[i].put(userId, trainedProfiles.get(scale[i]));
				
			}
		}
		

	}

	@Override
	public void updateItemVector(long itemId, Vector itemVector)
			throws TasteException {
		AtomicInteger trains = this.numTrainsItem.get(itemId);

		if (trains == null)
			numTrainsItem.put(itemId, new AtomicInteger(1));

		else
			trains.incrementAndGet();
	
		this.numTrainsItems.incrementAndGet();
		
		itemFactors.put(itemId, itemVector);

	}
	

	@Override
	public int getfDimensions() {
		
		return this.fDimensions;
	}

	@Override
	 public Object blockUser(long userId) {
		AtomicInteger trains = null;
		synchronized (numTrainsUser) {
			trains = this.numTrainsUser.get(userId);	
		}
		

		if (trains == null){
			AtomicInteger newTrains=new AtomicInteger(0);
			trains=numTrainsUser.putIfAbsent(userId, newTrains);
			if(trains==null){
				//put succeded
				trains=newTrains;
			}
		}	
		
			
		return trains;
	}

	@Override
	public Object blockItem(long itemId) {
		AtomicInteger trains = null;
		synchronized (numTrainsItem) {
			trains = this.numTrainsItem.get(itemId);
		}
		if (trains == null){
			AtomicInteger newTrains=new AtomicInteger(0);
			trains=numTrainsItem.putIfAbsent(itemId, newTrains);
			if(trains==null){
				//put succeded
				trains=newTrains;
			}
		}	
		
			
		return trains;
	}

	

	public long getNumUsers() {
		
		return this.numTrainsUser.size();
	}

	public long getNumItems() {
		
		return this.numTrainsItem.size();
	}
	
	@Override
	public void setRestrictUsers(HashSet<Long> restrictedUserIds) {
		this.restrictedUserIds=restrictedUserIds;
		
	}
	@Override
	public Set<Long> getItemsId(int minTrains) {
		if(minTrains==-1)
		return this.numTrainsItem.keySet();
		
		HashSet<Long> ret=  new HashSet<Long>();
		for (Long key : this.numTrainsItem.keySet()) {
			if(this.numTrainsItem.get(key).intValue()>=minTrains)
				ret.add(key);
		}
		return ret;
	}

	@Override
	public Set<Long> getUsersId() {
		
		return this.numTrainsUser.keySet();
	}

	@Override
	public boolean hasPrivateStrategy() {
		
		return this.hasPrivateInfo;
	}

	@Override
	public double getNumberTrainsItems() {
		
		return this.numTrainsItems.get();
	}

}
