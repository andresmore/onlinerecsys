package edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation;

import java.util.LinkedList;

import org.apache.mahout.math.Vector;

public class ItemProfile {

	private Vector probabilityVector;
	private LinkedList<Long> metadataVector;

	public ItemProfile(Vector vector, LinkedList<Long> metadataVector) {
		this.probabilityVector=vector;
		this.metadataVector=metadataVector;
	}

	public static ItemProfile buildDenseProfile(
			Vector itemVect, LinkedList<Long> metadataVector) {
		
		
		return new ItemProfile(itemVect,metadataVector);
	}

	public Vector getProbabilityVector() {
		
		return probabilityVector;
	}
	
	public LinkedList<Long> getMetadataVector(){
		return metadataVector;
	}

}
