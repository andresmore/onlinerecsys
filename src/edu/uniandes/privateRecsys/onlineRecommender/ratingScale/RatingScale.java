package edu.uniandes.privateRecsys.onlineRecommender.ratingScale;

public interface RatingScale {
    
	/**
	 * Returns the size of the rating
	 * @return
	 */
	public int getRatingSize();
	public String[] getScale();
	public boolean hasScale(String rating);

}