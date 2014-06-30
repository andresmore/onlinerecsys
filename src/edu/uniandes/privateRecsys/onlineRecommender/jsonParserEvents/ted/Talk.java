package edu.uniandes.privateRecsys.onlineRecommender.jsonParserEvents.ted;

import java.util.ArrayList;

public class Talk {
	private String film_date;
	private String description;
	private String title; 
	private ArrayList<String> related_tags;  
	private ArrayList<Comment> comments;
	private String ted_event;
	private ArrayList<String> related_themes;
	private String speaker;
	private String id;
	public String getFilm_date() {
		return film_date;
	}
	public void setFilm_date(String film_date) {
		this.film_date = film_date;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public ArrayList<String> getRelated_tags() {
		return related_tags;
	}
	public void setRelated_tags(ArrayList<String> related_tags) {
		this.related_tags = related_tags;
	}
	public ArrayList<Comment> getComments() {
		return comments;
	}
	public void setComments(ArrayList<Comment> comments) {
		this.comments = comments;
	}
	public String getTed_event() {
		return ted_event;
	}
	public void setTed_event(String ted_event) {
		this.ted_event = ted_event;
	}
	public ArrayList<String> getRelated_themes() {
		return related_themes;
	}
	public void setRelated_themes(ArrayList<String> related_themes) {
		this.related_themes = related_themes;
	}
	public String getSpeaker() {
		return speaker;
	}
	public void setSpeaker(String speaker) {
		this.speaker = speaker;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
}
