package edu.uniandes.privateRecsys.onlineRecommender.vo;

import org.apache.mahout.cf.taste.similarity.precompute.SimilarItem;

public class ComparableSimilarItem extends SimilarItem implements Comparable<ComparableSimilarItem> {

	public ComparableSimilarItem(long itemID, double similarity) {
		super(itemID, similarity);
		
	}

	@Override
	public int compareTo(ComparableSimilarItem o) {
		// TODO Auto-generated method stub
		return Double.compare(this.getSimilarity(), o.getSimilarity());
	}

	

}
