package edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation;

import org.apache.mahout.math.Vector;

public class ItemProfile {

	private Vector vector;

	public ItemProfile(Vector vector) {
		this.vector=vector;
	}

	public static ItemProfile buildDenseProfile(
			Vector itemVect) {
		
		
		return new ItemProfile(itemVect);
	}

	public Vector getVector() {
		
		return vector;
	}

}
