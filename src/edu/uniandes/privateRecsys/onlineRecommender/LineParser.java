package edu.uniandes.privateRecsys.onlineRecommender;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class LineParser implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1910340241593499292L;
	private HashSet<String> separators;

	public LineParser() {
		this.separators = new HashSet<String>();
	}
	
	public UserTrainEvent processLine(String line, int maxFields) {
		Iterator<String> tokens=this.split(line,maxFields);
		
	
			 String userIDString = new String(tokens.next());
			 String itemIDString = new String(tokens.next());
			 String preferenceValueString = new String(tokens.next());
			 boolean hasTimestamp = tokens.hasNext();
			 String timestampString = hasTimestamp ? new String(tokens.next()) : null;
			 boolean hasMetadata = tokens.hasNext();
			 String metadata=hasMetadata? new String(tokens.next()):null;
		try{	 
		return new UserTrainEvent(Long.parseLong(userIDString), Long.parseLong(itemIDString), preferenceValueString,hasTimestamp? Long.parseLong(timestampString):0,metadata);
		}
		catch(Exception e){
			System.err.println("for input "+line);
			throw e;
		}
	}


	private Iterator<String> split(String line, int maxFields) {
		LinkedList<String> list= new LinkedList<String>();
		StringBuilder builder= new StringBuilder();
		for (int i = 0; i < line.length(); i++) {
			char at= line.charAt(i);
			if( this.separators.contains(Character.toString(at)) &&list.size()<maxFields-1  ){
				if(builder.length()>0)
					list.add(builder.toString());
				
					builder= new StringBuilder();
				
			}
			else{
				builder.append(at);
			}
			
		}
		if(builder.length()>0)
			list.add(builder.toString());
		
		return list.iterator();
	}

	public void addSeparator(String string) {
		this.separators.add(string);
		
	}
	
	
}