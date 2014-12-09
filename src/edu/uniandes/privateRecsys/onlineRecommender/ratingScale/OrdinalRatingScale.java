package edu.uniandes.privateRecsys.onlineRecommender.ratingScale;

import java.util.HashMap;
import java.util.LinkedList;


public class OrdinalRatingScale implements RatingScale {

	private final String[] scale;
	private final String[] publicScale;
	private final double[] scaleAsValues;
	private final HashMap<String, String> translations;

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
		this.scaleAsValues= new double[publicScale.length];
		for (int i = 0; i < publicScale.length; i++) {
			this.scaleAsValues[i]=Double.parseDouble(publicScale[i]);
		}
	
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
		for (int i = 0; i < publicScale.length; i++) {
			if(publicScale[i].equals(rating))
				return true;
		}
		return false;
	}

	@Override
	public String getRatingAlias(String rating) {
		
		String alias=translations.get(rating);
		if(alias!=null)
			return alias;
		
		return rating;
	}

	@Override
	public double[] scaleAsValues() {
		// TODO Auto-generated method stub
		return scaleAsValues;
	}

	@Override
	public int getIndexForPreference(String rating) {
		for (int i = 0; i < publicScale.length; i++) {
			if(publicScale[i].equals(rating))
				return i;
		}
		return -1;
	}

	

}
