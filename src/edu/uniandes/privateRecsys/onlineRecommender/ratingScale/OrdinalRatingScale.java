package edu.uniandes.privateRecsys.onlineRecommender.ratingScale;

import java.util.HashMap;
import java.util.LinkedList;


public class OrdinalRatingScale implements RatingScale {

	private String[] scale;
	private String[] publicScale;
	private HashMap<String, String> translations;

	public OrdinalRatingScale(String[] strings, HashMap<String,String> translations) {
		
		
		this.scale=strings;
		this.translations=translations;
		LinkedList<String> finalList= new LinkedList<String>();
		for (int i = 0; i < scale.length; i++) {
			if(!translations.keySet().contains(scale[i])){
				finalList.add(scale[i]);
			}
				
		}
		publicScale=finalList.toArray(new String[finalList.size()]);
		
	
	}

	@Override
	public int getRatingSize() {
		
		return publicScale.length;
	}

	public String[] getScale() {
		return publicScale;
	}

	@Override
	public boolean hasScale(String rating) {
		for (int i = 0; i < scale.length; i++) {
			if(scale[i].equals(rating))
				return true;
		}
		return false;
	}

	@Override
	public String getRatingAlias(String rating) {
		if(translations.containsKey(rating))
			return translations.get(rating);
		return rating;
	}

	

}
