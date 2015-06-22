package edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.UserMetadataInfo;
import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.postgresdb.PosgresDAO;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.utils.PrivateRandomUtils;
/**
 * Class that manages a incremental factor model backed on a DB on posgresDAO 
 * @deprecated ignores user bias and hyperParams
 * @author Andres M
 *
 */
@Deprecated 
public class IncrementalDBFactorUserItemRepresentation implements
		FactorUserItemRepresentation {


	private final static Logger LOG = Logger.getLogger(IncrementalDBFactorUserItemRepresentation.class
	      .getName());

	private RatingScale ratingScale;
	private int fDimensions;
	
	
	//private ConcurrentHashMap<Long, Vector> itemFactors;
	//private ConcurrentHashMap<Long, Vector>[] privateUserFactors;

	
	private ConcurrentHashMap<Long, AtomicInteger> numTrainsUser= new ConcurrentHashMap <>();
	private ConcurrentHashMap <Long, AtomicInteger> numTrainsItem= new ConcurrentHashMap <>();
	
	private HashSet<Long> restrictedUserIds;

	private PosgresDAO sqlDAO;

	

	public HashSet<Long> getRestrictedUserIds() {
		return restrictedUserIds;
	}

	public IncrementalDBFactorUserItemRepresentation(RatingScale scale, int fDimensions, boolean hasPrivateStrategy) throws PrivateRecsysException{
		this.ratingScale=scale;
		this.fDimensions=fDimensions;
		if(hasPrivateStrategy){
			throw new PrivateRecsysException("Private stategy not yet implemented");
		}
		sqlDAO=PosgresDAO.getInstance();
		
		
		
		
	}
	
	synchronized public void clearNumTrains(){
		this.numTrainsItem.clear();
		this.numTrainsUser.clear();
	}
	
	@Override
	public UserProfile getPrivateUserProfile(long userId) throws TasteException {
		if (isAllowed(userId)) {
			try {
				if (!this.sqlDAO.containsUserKey(userId))
					insertUser(userId);
				LinkedList<BetaDistribution> bias= new LinkedList<>();
				Vector emptyHyperParams= new DenseVector();
				return UserProfile.buildDenseProfile(
						this.sqlDAO.getUserFactors(ratingScale,userId), ratingScale,bias,emptyHyperParams, null, null, null,null, this.numTrainsUser.get(userId).get());
			} catch (SQLException e) {
				
				e.printStackTrace();
			}
		}
		return null;
	}
	
	private void insertUser(long userId) throws SQLException {
		String[] scale= this.ratingScale.getScale();
		HashMap<String, Vector> userProfile= new HashMap<String, Vector>();
		for (int i = 0; i < scale.length; i++) {
			
			Vector vec=PrivateRandomUtils.normalRandom(0, 1, this.fDimensions);
			userProfile.put(scale[i],vec);
		}
		userProfile=VectorProjector.projectUserProfileIntoSimplex(userProfile, scale, this.fDimensions);
		this.sqlDAO.insertUser(userId,userProfile);
		
	}

	@Override
	public UserProfile getPublicUserProfile(long userId) throws TasteException {
		return getPrivateUserProfile(userId);
	}

	private boolean isAllowed(long userId) {
		if(this.restrictedUserIds==null)
			return true;
		
		return this.restrictedUserIds.contains(userId);
	}

	@Override
	public synchronized ItemProfile getPrivateItemProfile(long itemId) throws TasteException {
		
		try {
			
			if(!this.sqlDAO.containsItemKey(itemId))
				insertItem(itemId);
				
					
			 
			
			
			return ItemProfile.buildDenseProfile(sqlDAO.getItemFactors(itemId),null);
		} catch (SQLException e) {
		
			LOG.log(Level.SEVERE, "SQLException", e);
			throw new TasteException(e.getMessage());
		}
		
		
		
	}

	private void insertItem(long itemId) throws SQLException {
		
		
		Vector vec=PrivateRandomUtils.normalRandom(0, 1, this.fDimensions);
		vec=VectorProjector.projectVectorIntoSimplex(vec);
		
		sqlDAO.putItem(itemId, vec);
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
			HashMap<String, Vector> trainedProfiles, HashMap<String, BetaDistribution> bias, Vector hyperParams, UserMetadataInfo info) throws TasteException {
		AtomicInteger trains = this.numTrainsUser.get(userId);

		if (trains == null)
			numTrainsUser.put(userId, new AtomicInteger(1));
		else
			trains.incrementAndGet();
		String[] scale= this.ratingScale.getScale();
		
		try {
			this.sqlDAO.updateUser(userId,trainedProfiles);
		} catch (SQLException e) {
			throw new TasteException(e.getMessage());
		}
		

	}

	

	@Override
	public void updatePublicTrainedProfile(long userId,
			HashMap<String, Vector> trainedProfiles) throws TasteException {

	}

	@Override
	public void updateItemVector(long itemId, Vector itemVector)
			throws TasteException {
		AtomicInteger trains = this.numTrainsItem.get(itemId);

		if (trains == null)
			numTrainsItem.put(itemId, new AtomicInteger(1));

		else
			trains.incrementAndGet();
	
	
		try {
			this.sqlDAO.updateItem(itemId, itemVector);
		} catch (SQLException e) {
			throw new TasteException(e.getMessage());
		}

	}
	

	@Override
	public int getfDimensions() {
		
		return this.fDimensions;
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
	synchronized public Object blockItem(long itemId) {
		AtomicInteger trains = this.numTrainsItem.get(itemId);

		if (trains == null){
			trains=new AtomicInteger(0);
			numTrainsItem.put(itemId, trains);
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
	
	
	
	public static void main(String[] args) {
		
	}

	@Override
	public boolean hasPrivateStrategy() {
	
		return false;
	}

	@Override
	public double getNumberTrainsItems() {
		
		return 0;
	}

	@Override
	public void addUserEvent(long userId, long itemId, String rating) {
		
		
	}

	@Override
	public void saveItemMetadata(long itemId, String metadata) {

		
	}

	@Override
	public Set<Long> getRatedItems(Long userID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Long> getPositiveElements(Long userId, String file) {
		// TODO Auto-generated method stub
		return null;
	}
	

}
