package edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation;



import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.DenseMatrix;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.UserMetadataInfo;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.utils.PrivateRandomUtils;
import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;

@Deprecated
public class DenseFactorUserItemRepresentation implements
		FactorUserItemRepresentation {

	private AverageDataModel model;
	protected RatingScale ratingScale;
	private int fDimensions;
	private DenseMatrix itemFactors;
	private DenseMatrix[] privateUserFactors;
	
	private BetaDistribution[][] privateUserBias;
	private DenseMatrix privateUserHyperParams;
	private DenseMatrix[] publicUserFactors;
	
	private ConcurrentHashMap<Long, AtomicInteger> numTrainsUser= new ConcurrentHashMap <>();
	private ConcurrentHashMap <Long, AtomicInteger> numTrainsItem= new ConcurrentHashMap <>();
	private AtomicInteger numTrainsItems= new AtomicInteger();
	private ConcurrentHashMap <Long, Long> userId_userPos= new ConcurrentHashMap <>();
	private ConcurrentHashMap <Long, Long> itemId_itemPos= new ConcurrentHashMap <>();
	private HashSet<Long> restrictedUserIds;
	private boolean hasPrivate;
	

	public DenseFactorUserItemRepresentation(AverageDataModel model,
			RatingScale scale, int fDimensions, int numHyperParams, boolean hasPrivate) throws TasteException {
		this.model=model;
		this.ratingScale=scale;
		this.fDimensions=fDimensions;
		this.hasPrivate=hasPrivate;
		createDenseRatingModel(this.fDimensions,numHyperParams );
		updateHashMapIds();
		
		
	}

	private void updateHashMapIds() throws TasteException {
		Iterator<Long>  iter=model.getUserIDs();
		 long pos1=0;
		 while(iter.hasNext()){
			userId_userPos.put(iter.next(), pos1); 
			pos1++;	 
		 }
		 long pos2=0;
		 Iterator<Long>  iter2=model.getItemIDs();
		 while(iter2.hasNext()){
			 itemId_itemPos.put(iter2.next(), pos2); 
			 pos2++;	 
		 }
		 
		
	}

	private void createDenseRatingModel(int fDimensions, int numHyperParams) throws TasteException {
		int ratingSize=this.ratingScale.getRatingSize();
		int numUsers= model.getNumUsers();
		int numItems=model.getNumItems();
		this.itemFactors= new DenseMatrix(numItems, fDimensions);
		PrivateRandomUtils.randomizeMatrix(itemFactors,true);
		
		this.privateUserFactors= new DenseMatrix[ratingSize];
		this.publicUserFactors= new DenseMatrix[ratingSize];
		this.privateUserBias= new BetaDistribution[numUsers][ratingSize];
		this.privateUserHyperParams= new DenseMatrix(numUsers, numHyperParams);
		
		for (int i = 0; i < privateUserFactors.length; i++) {
			privateUserFactors[i]= new DenseMatrix(numUsers, fDimensions);
			PrivateRandomUtils.randomizeMatrix(privateUserFactors[i],false);
			
		}
		
		VectorProjector.projectUserProfileMatricesIntoSimplex(privateUserFactors,ratingScale, fDimensions);
		for (int i = 0; i < publicUserFactors.length; i++) {
			publicUserFactors[i]=(DenseMatrix) privateUserFactors[i].clone();
			
		}
		
		for (int i = 0; i < privateUserBias.length; i++) {
			for (int j = 0; j < privateUserBias[0].length; j++) {
				privateUserBias[i][j]=new BetaDistribution(PrivateRandomUtils.getCurrentRandomGenerator(),1,1,BetaDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
			}
		}
		
		
	}

	@Override
	public UserProfile getPrivateUserProfile(long userId) throws TasteException {
		
		int userPos=(int) getUserPosition(userId);
		if(userPos==-1)
			throw new TasteException("User "+userId+" not found");
		
		LinkedList<Vector> privateVectors= new LinkedList<Vector>();
		LinkedList<BetaDistribution> privateBias= new LinkedList<>();
		for (int i = 0; i < privateUserFactors.length; i++) {
		
			DenseMatrix mat=privateUserFactors[i];
			
		
			privateVectors.add(mat.viewRow(userPos));
			privateBias.add(this.privateUserBias[userPos][i]);
		
		}
		UserProfile profile= UserProfile.buildDenseProfile(privateVectors,ratingScale, privateBias, this.privateUserHyperParams.viewRow(userPos), null, null, this.numTrainsUser.get(userId).get());
		return profile;
	}

	@Override
	public UserProfile getPublicUserProfile(long userId) throws TasteException {
		int userPos=(int) getUserPosition(userId);
		if(userPos==-1)
			throw new TasteException("User "+userId+" not found");
		LinkedList<Vector> publicVectors= new LinkedList<Vector>();
		LinkedList<BetaDistribution> privateBias= new LinkedList<>();
		
		for (int i = 0; i < publicUserFactors.length; i++) {
		
			DenseMatrix mat=publicUserFactors[i];
		
			 
			publicVectors.add(mat.viewRow(userPos));
			privateBias.add(new BetaDistribution(PrivateRandomUtils.getCurrentRandomGenerator(),1, 1,BetaDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY));
		
		}
		Vector emptyHyperParms= null;
		UserProfile profile= UserProfile.buildDenseProfile(publicVectors,ratingScale,privateBias,emptyHyperParms,null, null, 0);
		return profile;
	}
	

	@Override
	public ItemProfile getPrivateItemProfile(long itemId) throws TasteException {
		int itemPos=(int) getItemPosition(itemId);
		if(itemPos==-1)
			throw new TasteException("Item "+itemId+" not found");
		
		ItemProfile profile=ItemProfile.buildDenseProfile(itemFactors.viewRow(itemPos));
		return profile;
	}
	
	protected long getItemPosition(long itemId) throws TasteException {
		Long itemPos=this.itemId_itemPos.get(itemId);
			 
		return itemPos==null?-1:itemPos;
	}

	protected long getUserPosition(long userId) throws TasteException {
		Long userPos=this.userId_userPos.get(userId);
		return userPos==null?-1:userPos;
	}

	@Override
	public RatingScale getRatingScale() {
		
		return this.ratingScale;
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
			HashMap<String, Vector> trainedProfiles, HashMap<String, BetaDistribution> bias, Vector hyperParamenters, UserMetadataInfo info) throws TasteException {

		AtomicInteger trains = this.numTrainsUser.get(userId);

		if (trains == null)
			numTrainsUser.put(userId, new AtomicInteger(1));
		else
			trains.incrementAndGet();

		int userPos = (int) getUserPosition(userId);
		privateUserHyperParams.assignRow(userPos, hyperParamenters);
		for (int i = 0; i < this.privateUserFactors.length; i++) {
			String rating = this.ratingScale.getScale()[i];
			Vector toReplace = trainedProfiles.get(rating);

			privateUserFactors[i].assignRow(userPos, toReplace);
			privateUserBias[userPos][i]=bias.get(rating);

		}

		// System.out.println("Updated "+userId+" "+(trains+1));
	}

	@Override
	  public void updatePublicTrainedProfile(long userId,
			HashMap<String, Vector> trainedProfiles) throws TasteException {
		int userPos = (int) getUserPosition(userId);

		for (int i = 0; i < this.privateUserFactors.length; i++) {
			String rating = this.ratingScale.getScale()[i];
			Vector toReplace = trainedProfiles.get(rating);
			//synchronized (publicUserFactors[i]) {
				publicUserFactors[i].assignRow(userPos, toReplace);
			//}
		}

	}

	@Override
	  public void updateItemVector(long itemId, Vector itemVector)
			throws TasteException {
		int itemPos = (int) getItemPosition(itemId);
		
		AtomicInteger trains = this.numTrainsItem.get(itemId);

			if (trains == null)
				numTrainsItem.put(itemId, new AtomicInteger(1));

			else
				trains.incrementAndGet();
			
			this.numTrainsItems.incrementAndGet();
		
			itemFactors.assignRow(itemPos, itemVector);
		

	}

	public int getfDimensions() {
		return fDimensions;
	}

	@Override
	synchronized public Object blockUser(long userId) {

		AtomicInteger trains = this.numTrainsUser.get(userId);

		if (trains == null){
			trains=new AtomicInteger(0);
			numTrainsUser.put(userId, trains);
		}	
		
			
		return trains;
	}

	@Override
	public Object blockItem(long itemId) {
		AtomicInteger trains = this.numTrainsItem.get(itemId);

		if (trains == null){
			trains=new AtomicInteger(0);
			numTrainsItem.put(itemId, trains);
		}	
		
			
		return trains;
	}
	
	public Prediction calculatePrediction(long itemId, long userId, int minTrains)throws TasteException
			{
		double prediction=0;
		UserProfile user=null;
		ItemProfile item = null;
		try {
			user = this
					.getPublicUserProfile(userId);
			item = this
					.getPrivateItemProfile(itemId);
		} catch (TasteException e) {
			return Prediction.createNoAblePrediction(userId,itemId);
		}
		
		
		int numTrainsItem=this.getNumberTrainsItem(itemId);
		int numTrainsUser=this.getNumberTrainsUser(userId);
		if (numTrainsUser < minTrains){
			//System.err.println("Not enough trains for (user,item) ("+userId+","+itemId+")");rn 0;
			return Prediction.createNoAblePrediction(userId,itemId);
		}
		else {
			String[] ratingScale = this.ratingScale.getScale();
			
			double sumprob = 0;
			if (item != null && user != null) {
				Vector itemVector = item.getVector();
				if(numTrainsItem<minTrains){
					//Equiprobable vector
						itemVector=itemVector.assign(1);
						itemVector=VectorProjector.projectVectorIntoSimplex(itemVector);
						
				}
					
				Vector dotVector= new DenseVector(ratingScale.length);
				for (int i = 0; i < ratingScale.length; i++) {
					Vector userVector = user
							.getProfileForScale(ratingScale[i]);
					double dot = userVector.dot(itemVector);
					sumprob += dot;
					prediction += dot * Double.parseDouble(ratingScale[i]);
					dotVector.setQuick(i, dot);
				}
				//System.out.println("RAting prediction should be: "+rating+", dots are: "+dotVector);
				/*for (int i = 0; i < ratingScale.length; i++) {
					Vector userVector = user
							.getProfileForScale(ratingScale[i]);
					double dot = userVector.dot(itemVector);
					if(dot>sumprob){
						sumprob = dot;
						prediction = Double.parseDouble(ratingScale[i]);
					}

				}*/
			}
			
			
			
		}
		return Prediction.createPrediction(userId,itemId,prediction);
	}
	
	 protected Iterator<Long> getItemIds(){
		return model.getItemIDs();
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
		
		return this.hasPrivate;
	}

	@Override
	public double getNumberTrainsItems() {
	
		return this.numTrainsItems.get();
	}



	
	

}
