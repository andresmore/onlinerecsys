package edu.uniandes.privateRecsys.onlineRecommender.jsonParserEvents.ted;

import java.util.ArrayList;

public class Comment {
	private String user_id;
	private ArrayList<Comment> replies;
	public String getUser_id() {
		return user_id;
	}
	public void setUser_id(String user_id) {
		this.user_id = user_id;
	}
	public ArrayList<Comment> getReplies() {
		return replies;
	}
	public void setReplies(ArrayList<Comment> replies) {
		this.replies = replies;
	}
	
	
	
	
}
