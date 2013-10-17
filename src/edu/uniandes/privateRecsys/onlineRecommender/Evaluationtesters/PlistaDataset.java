package edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters;

import java.util.LinkedList;

import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;

public class PlistaDataset extends RSDataset {

	private String userFile;
	private String directory;
	private LinkedList<String> prefixes;

	public PlistaDataset(String userFile,String directory,LinkedList<String> prefixes,
			RatingScale scale) {
		
		super(null, null, null, scale);
		this.userFile=userFile;
		this.directory=directory;
		this.prefixes=prefixes;
	}

	public String getUserFile() {
		return userFile;
	}

	public String getDirectory() {
		return directory;
	}

	public LinkedList<String> getPrefixes() {
		// TODO Auto-generated method stub
		return this.prefixes;
	}

}
