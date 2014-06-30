package edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.similarity.precompute.SimilarItem;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Matrix;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.hadoop.similarity.cooccurrence.measures.EuclideanDistanceSimilarity;

import edu.uniandes.privateRecsys.onlineRecommender.BaseModelPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.IItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.IUserItemAggregator;
import edu.uniandes.privateRecsys.onlineRecommender.ItemProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.LearningRateStrategy;
import edu.uniandes.privateRecsys.onlineRecommender.NoPrivacyAggregator;
import edu.uniandes.privateRecsys.onlineRecommender.UserModelTrainerPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.UserProfileUpdater;
import edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters.AbstractRecommenderTester;
import edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters.OnlineRecommenderTester;
import edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters.RSMetadataDataset;
import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.metadata.MetadataMapFileLoader;
import edu.uniandes.privateRecsys.onlineRecommender.metadata.SpectralDataLoader;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.OrdinalRatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.vo.ComparableSimilarItem;
import edu.uniandes.privateRecsys.onlineRecommender.vo.ErrorReport;
import edu.uniandes.privateRecsys.onlineRecommender.vo.Prediction;

public class DenseFactorUserItemRepresentationWithMetadata extends
DenseFactorUserItemRepresentation {

	
	private static final double MIN_SIMILARITY = 0;
	private int numNeighbors=5;
	private Matrix metadataMatrix;
	EuclideanDistanceSimilarity euclideanSimilarity= new EuclideanDistanceSimilarity();
	

	public DenseFactorUserItemRepresentationWithMetadata(AverageDataModel model,
			RatingScale scale, int fDimensions, String matrixRepresentationFile, int numNeighbors, boolean spectralFile, int numHyperParams) throws TasteException, IOException {
		super(model, scale, fDimensions, numHyperParams,false);
		if(spectralFile)
				this.metadataMatrix=SpectralDataLoader.loadSpectralRepresentation(matrixRepresentationFile);
		else
				this.metadataMatrix=MetadataMapFileLoader.loadMetadataMap(matrixRepresentationFile, true);
		
		this.numNeighbors=numNeighbors;
		
		
		if(model.getNumItems()!=metadataMatrix.numRows()){
			throw new TasteException("Average model doesn't match the number of items in the spectral matrix model="+model.getNumItems()+" spectral matrix="+metadataMatrix.numRows());
		}
	}

	@Override
	public Prediction calculatePrediction(long itemId, long userId, int minTrains) throws TasteException
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
			
			return Prediction.createNoAblePrediction(userId,itemId);
		}
		else {
			String[] ratingScale = this.ratingScale.getScale();
			
			double sumprob = 0;
			if (item != null && user != null) {
				
				
				Vector itemVector = null;
				if(numTrainsItem>=minTrains){
					itemVector =item.getProbabilityVector();
				}
				else{
					itemVector=buildVectorFromSpectralData(itemId,minTrains);
					
				}	
				
				if(itemVector==null){
					return Prediction.createNoAblePrediction(userId,itemId);
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

			}
			
			
			
		}
		if(numTrainsItem>=minTrains){
			return Prediction.createPrediction(userId,itemId,prediction);
		}
		return Prediction.createHyrbidPrediction(userId,itemId,prediction);
	}

	public Vector buildVectorFromSpectralData(long itemId, int minTrains) throws TasteException {
		
		long itemPosition=this.getItemPosition(itemId);
		Vector spectralRepresentation=metadataMatrix.viewRow((int) itemPosition);
		
		
		Queue<ComparableSimilarItem> topItems = new PriorityQueue<ComparableSimilarItem>(numNeighbors + 1, Collections.reverseOrder());
		Iterator<Long> itemIterator= this.getItemIds();
		boolean full=false;
		double totalSimilarity=0;
		while(itemIterator.hasNext()){
			long otherItemId= itemIterator.next();
			if(this.getNumberTrainsItem(otherItemId)>minTrains && otherItemId!=itemId){
				long otherItemPosition=this.getItemPosition(otherItemId);
				Vector otherItemSpectral=metadataMatrix.viewRow((int) otherItemPosition);
				double similarity = spectralRepresentation.dot(otherItemSpectral)/(spectralRepresentation.norm(2)*otherItemSpectral.norm(2));
				if(similarity>MIN_SIMILARITY){
					topItems.add(new ComparableSimilarItem(otherItemId, similarity));
					totalSimilarity+=similarity;
					if (full) {
			          SimilarItem polled=topItems.poll();
			          totalSimilarity-=polled.getSimilarity();
			        } 
					else if (topItems.size() > numNeighbors) {
			          full = true;
			          SimilarItem polled=topItems.poll();
			          totalSimilarity-=polled.getSimilarity();
					}
				}
			}
		}
		Vector ret=null;
		for (SimilarItem item : topItems) {
			double multiplier=(double)item.getSimilarity()/(double)totalSimilarity;
			Vector itemVector=this.getPrivateItemProfile(item.getItemID()).getProbabilityVector();
			Vector multiplied=itemVector.times(multiplier);
			if(ret==null)
				ret=multiplied;
			else
				ret=ret.plus(multiplied);
		}
		
		
		
		return ret;
	}
	
	

}
