package edu.uniandes.privateRecsys.onlineRecommender.ratingScale;

import java.util.HashMap;

public interface RatingScale {
    
	/**
	 * Returns the size of the rating
	 * @return
	 */
	public int getRatingSize();
	public String[] getScale();
	public boolean hasScale(String rating);
	public String getRatingAlias(String rating);
	double[] scaleAsValues();
	int getIndexForPreference(String rating);
	public HashMap<String,String> getTranslations();

}
