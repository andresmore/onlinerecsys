package edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.GenericPreference;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.ConceptBreaker;
import edu.uniandes.privateRecsys.onlineRecommender.FilterElement;
import edu.uniandes.privateRecsys.onlineRecommender.UserMetadataInfo;
import edu.uniandes.privateRecsys.onlineRecommender.UserModelTrainerPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters.RSDataset;
import edu.uniandes.privateRecsys.onlineRecommender.metadata.SlidingWindowCountMinSketch;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.utils.PrivateRandomUtils;

public class IncrementalFactorUserItemRepresentation implements
		FactorUserItemRepresentation {

	
	private RatingScale ratingScale;
	private int fDimensions;
	
	private ConcurrentHashMap<Long, Vector> itemFactors;
	private ConcurrentHashMap<Long, LinkedList<Long>> itemMetadata;
	private ConcurrentHashMap<Long, Vector>[] privateUserFactors;
	private ConcurrentHashMap<Long, Vector>[] privateUserMetadataFactors;
	private ConcurrentHashMap<Long, LinkedList<Long> > privateUserConcepts;
	private ConcurrentHashMap<Long, SlidingWindowCountMinSketch> privateUserSketch;
	private ConcurrentHashMap<Long, LinkedList<BetaDistribution>> privateUserBias;
	private ConcurrentHashMap<Long, Vector>[] publicUserFactors;
	private ConcurrentHashMap<Long, Vector> privateHyperParams;
	private ConcurrentHashMap<Long, LinkedList<Preference>> privateUserHistory;
	
	
	
	private ConcurrentHashMap<Long, AtomicInteger> numTrainsUser= new ConcurrentHashMap <>();
	private ConcurrentHashMap <Long, AtomicInteger> numTrainsItem= new ConcurrentHashMap <>();
	
	private AtomicInteger numTrainsItems= new AtomicInteger();
	private boolean hasPrivateInfo;
	private HashSet<Long> restrictedUserIds;
	private UserModelTrainerPredictor modelTrainerPredictor;
	protected RSDataset dataSet;
	

	public HashSet<Long> getRestrictedUserIds() {
		return restrictedUserIds;
	}

	@SuppressWarnings("unchecked")
	public IncrementalFactorUserItemRepresentation(RSDataset data, int fDimensions, boolean hasPrivateStrategy, UserModelTrainerPredictor trainerPredictor){
		this.dataSet=data;
		this.ratingScale=data.getScale();
		this.fDimensions=fDimensions;
		this.modelTrainerPredictor=trainerPredictor;
		this.hasPrivateInfo=hasPrivateStrategy;
		
		if(this.modelTrainerPredictor.hasProbabilityPrediction()){
			this.itemFactors= new ConcurrentHashMap<>();
			this.privateUserFactors= new ConcurrentHashMap[ratingScale.getRatingSize()];
			for (int i = 0; i < privateUserFactors.length; i++) {
				privateUserFactors[i]= new ConcurrentHashMap<>();
			}
			if(this.hasPrivateInfo){
				this.publicUserFactors= new ConcurrentHashMap[ratingScale.getRatingSize()];
				for (int i = 0; i < publicUserFactors.length; i++) {
					publicUserFactors[i]= new ConcurrentHashMap<>();
				}
			}	
			else{
				this.publicUserFactors= privateUserFactors;
			}
			
		}
		
		if(this.modelTrainerPredictor.hasMetadataPredictor()){
			this.privateUserMetadataFactors= new ConcurrentHashMap[ratingScale.getRatingSize()];
			this.privateUserConcepts= new ConcurrentHashMap<>();
			this.privateUserSketch= new ConcurrentHashMap<Long, SlidingWindowCountMinSketch>();
			for (int i = 0; i < privateUserMetadataFactors.length; i++) {
				privateUserMetadataFactors[i]= new ConcurrentHashMap<>();
			}
		}
		if(this.modelTrainerPredictor.hasBiasPredictor()){
			this.privateUserBias= new ConcurrentHashMap<>();
		}
		
		if(trainerPredictor.hasHyperParameters()){
			this.privateHyperParams= new ConcurrentHashMap<>();
		}
		if(trainerPredictor.hasUserHistory())
		{
			this.privateUserHistory= new ConcurrentHashMap<>();
		}
		if(trainerPredictor.saveItemMetadata()){
			this.itemMetadata= new ConcurrentHashMap<Long, LinkedList<Long>>();
		}
		
		
	}
	
	@Override
	synchronized public UserProfile getPrivateUserProfile(long userId) throws TasteException {
		if(isAllowed(userId)){
			
			
			if (!checkIfUserExists(userId))
				insertUser(userId);

			LinkedList<Vector> userVectors = new LinkedList<>();
			LinkedList<Vector> metadataVectors = new LinkedList<>();
			for (int i = 0; i < this.ratingScale.getRatingSize(); i++) {
				if (modelTrainerPredictor.hasProbabilityPrediction())
					userVectors.add(this.privateUserFactors[i].get(userId));
				if (modelTrainerPredictor.hasMetadataPredictor())
					metadataVectors.add(this.privateUserMetadataFactors[i]
							.get(userId));
			}
			
			
			LinkedList<BetaDistribution> userBiasVector = null;
			
			if(this.modelTrainerPredictor.hasBiasPredictor())
					userBiasVector=this.privateUserBias.get(userId);
			
			
			Vector userHyperParams = null;
			if(this.modelTrainerPredictor.hasHyperParameters())
				userHyperParams=privateHyperParams.get(userId);
			
			
			LinkedList<Long> existingConcepts = null;
			SlidingWindowCountMinSketch sketch = null;
			if(this.modelTrainerPredictor.hasMetadataPredictor()){
				existingConcepts = this.privateUserConcepts.get(userId);
				sketch = this.privateUserSketch.get(userId);
			}
			
			LinkedList<Preference> userHistory= null;
			if(this.modelTrainerPredictor.hasUserHistory())
				userHistory=this.privateUserHistory.get(userId);
			
			int numTrains=this.numTrainsUser.get(userId)==null? 0:this.numTrainsUser.get(userId).get();
			
			return UserProfile.buildDenseProfile(userVectors, ratingScale,
					userBiasVector, userHyperParams, metadataVectors,
					existingConcepts,
					sketch,userHistory,
					numTrains);

		}
		return null;
	}
	
	private boolean checkIfUserExists(long userId) {
		if(this.modelTrainerPredictor.hasProbabilityPrediction()&&!privateUserFactors[0].containsKey(userId))
			return false;
		if(this.modelTrainerPredictor.hasBiasPredictor()&&!privateUserBias.containsKey(userId))
			return false;
		if(this.modelTrainerPredictor.hasMetadataPredictor()&&!privateUserSketch.containsKey(userId))
			return false;
		if(this.modelTrainerPredictor.hasHyperParameters()&&!privateHyperParams.containsKey(userId))
			return false;
		
		
		return true;
	}

	private void insertUser(long userId) {
		
		String[] scale= this.ratingScale.getScale();
		
		LinkedList<BetaDistribution> userPriors= new LinkedList<>();
		
		HashMap<String, Vector> userProfile= new HashMap<String, Vector>();
		
		for (int i = 0; i < scale.length; i++) {
			
			if(this.modelTrainerPredictor.hasProbabilityPrediction()){
				Vector vec=PrivateRandomUtils.normalRandom(0, 0.01, this.fDimensions);
				userProfile.put(scale[i],vec);
			}
			
			if(this.modelTrainerPredictor.hasBiasPredictor())
				userPriors.add(new BetaDistribution(PrivateRandomUtils.getCurrentRandomGenerator(),1,1,BetaDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY));
		}
		
		if(this.modelTrainerPredictor.hasProbabilityPrediction()){
			userProfile=VectorProjector.projectUserProfileIntoSimplex(userProfile, scale, this.fDimensions);
			
		}
		
		if(this.modelTrainerPredictor.hasBiasPredictor())
			this.privateUserBias.put(userId, userPriors);
		
		if(this.modelTrainerPredictor.hasHyperParameters()){
			Vector userHyperParams= new DenseVector(modelTrainerPredictor.getHyperParametersSize());
			this.privateHyperParams.put(userId, userHyperParams);
		}
		
		if (this.modelTrainerPredictor.hasMetadataPredictor()) {
			this.privateUserConcepts.put(userId, new LinkedList<Long>());

			this.privateUserSketch.put(userId,this.modelTrainerPredictor.buildMetadataSketch());
			/* new SlidingWindowCountMinSketch(
					UserProfile.SKETCH_DEPTH, UserProfile.SKETCH_WIDTH,
					UserProfile.SEED, UserProfile.NUMBER_OF_SEGMENTS,
					UserProfile.WINDOW_LENGHT, UserProfile.HASH_A)*/
		}

		for (int i = 0; i < scale.length; i++) {
			if (this.modelTrainerPredictor.hasProbabilityPrediction()) {
				privateUserFactors[i].put(userId, userProfile.get(scale[i]));

				if (this.hasPrivateInfo) {
					publicUserFactors[i].put(userId, userProfile.get(scale[i]));
				}
			}
			if (this.modelTrainerPredictor.hasMetadataPredictor()) {
				Vector privateMetadataVector = new DenseVector(0);
				privateUserMetadataFactors[i]
						.put(userId, privateMetadataVector);
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
					dist,emptyHyperParams,null, null, null,null, 0);
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
		
					
		if(this.modelTrainerPredictor.hasProbabilityPrediction()&&!this.itemFactors.containsKey(itemId))
			insertItem(itemId);
		
		LinkedList<Long> metadataVector=this.modelTrainerPredictor.saveItemMetadata()?this.itemMetadata.get(itemId):null;
		Vector probVector = this.modelTrainerPredictor.hasProbabilityPrediction()?this.itemFactors.get(itemId):null;
		return ItemProfile.buildDenseProfile(probVector,metadataVector);
		
		
	}

	private Vector insertItem(long itemId) {
		
		
		Vector vec=PrivateRandomUtils.normalRandom(0, 1, this.fDimensions);
		vec=VectorProjector.projectVectorIntoSimplex(vec);
		
		return itemFactors.putIfAbsent(itemId, vec);
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
		for (int i = 0; i < scale.length; i++) {
			if(this.modelTrainerPredictor.hasProbabilityPrediction())
				privateUserFactors[i].put(userId, trainedProfiles.get(scale[i]));
			
			if(bias!=null)
				userPriors.add(bias.get(scale[i]));
		}
		if(bias!=null &&modelTrainerPredictor.hasBiasPredictor()){
			privateUserBias.put(userId, userPriors);
			
		}	
		if(hyperParams!=null&& modelTrainerPredictor.hasHyperParameters()){
			privateHyperParams.put(userId, hyperParams);
		}
		if(info!=null && modelTrainerPredictor.hasMetadataPredictor()){
			privateUserConcepts.put(userId,info.getIncludedConcepts());
			privateUserSketch.put(userId, info.getUserSketch());
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

	@Override
	public void addUserEvent(long userId, long itemId, String rating) {
		if(this.modelTrainerPredictor.hasUserHistory()){
			LinkedList<Preference> userHistory=privateUserHistory.get(userId);
			if(userHistory==null)
				userHistory= new LinkedList<>();
			
			GenericPreference pref= new GenericPreference(userId, itemId, Float.parseFloat(rating));	
			userHistory.add(pref);
			
			this.privateUserHistory.putIfAbsent(userId, userHistory);
		}
	}

	@Override
	public void saveItemMetadata(long itemId, String metadataStr) {

		if (this.modelTrainerPredictor.saveItemMetadata()) {
			if (!this.itemMetadata.containsKey(itemId)) {
				if (metadataStr != null) {
					LinkedList<Long> metadata = ConceptBreaker.breakConcepts(metadataStr);
					Collections.sort(metadata);
					this.itemMetadata.putIfAbsent(itemId, metadata);
				}
			}
		}
	}

	@Override
	public Set<Long> getRatedItems(Long userId) {
		if(! this.modelTrainerPredictor.hasUserHistory()) {
		HashSet<Long> itemIds= new FilterElement(userId,this.dataSet.getTrainSet()).getElementsFromFile();
		return itemIds;
		}
		else {
			LinkedList<Preference> preferences=privateUserHistory.get(userId);
			
			HashSet<Long> itemIds=new HashSet<>();
			for (Preference pref : preferences) {
				itemIds.add(pref.getItemID());
			}
			return itemIds;
		}
	}
	

	@Override
	public Set<Long> getPositiveElements(Long userId, String file) {
		HashSet<Long> itemIds= new FilterElement(userId,file).getElementsFromFile();
		return itemIds;
	}
	
	

	

}
