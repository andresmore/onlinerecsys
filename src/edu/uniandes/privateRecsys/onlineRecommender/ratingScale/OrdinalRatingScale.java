package edu.uniandes.privateRecsys.onlineRecommender.ratingScale;


public class OrdinalRatingScale implements RatingScale {

	private String[] scale;

	public OrdinalRatingScale(String[] strings) {
		this.scale=strings;
	}

	@Override
	public int getRatingSize() {
		
		return scale.length;
	}

	public String[] getScale() {
		return scale;
	}

	@Override
	public boolean hasScale(String rating) {
		for (int i = 0; i < scale.length; i++) {
			if(scale[i].equals(rating))
				return true;
		}
		return false;
	}

	

}
