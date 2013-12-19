package edu.uniandes.privateRecsys.onlineRecommender;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Observable;

import org.apache.mahout.common.iterator.FileLineIterator;

import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.vo.ReportErrorEvent;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;



public class FileEventCreator extends Observable{

	
	
	
	private static final int MAX_FIELDS=5;
	private HashSet<String> separators= new HashSet<String>();
	
	//Horrible hack to save memory
	private HashMap<Long,Long> allConcepts= new HashMap<Long,Long>();
	private HashSet<Character> metaDataSeparators;
	private int numEventReport;
	private int limitEvents;
	private File file;
	
	public FileEventCreator(File file, int numEventReport, int limitEvents) throws IOException {
		this.file=file;
		
		this.separators.add(":");
		this.separators.add(",");
		
		this.metaDataSeparators= new HashSet<>(4);
		metaDataSeparators.add('{');
		metaDataSeparators.add('}');
		metaDataSeparators.add(',');
		metaDataSeparators.add(':');
				
		this.numEventReport=numEventReport;
		this.limitEvents=limitEvents;
	}
	
	
	public int startEvents() throws PrivateRecsysException, IOException {

		FileLineIterator iterator = null;
		int numlines = 0;
		try {
			iterator = new FileLineIterator(file);

			if (this.countObservers() == 0) {

				throw new PrivateRecsysException(
						"No observers registered for experiment, cancelling ...");
			}

			while (iterator.hasNext()) {

				String line = new String(iterator.next());
				UserTrainEvent event = processLine(line);
				setChanged();
				notifyObservers(event);
				numlines++;
				if (limitEvents == numlines) {

					return limitEvents;
				}
				if ((numlines % this.numEventReport) == 0
						&& this.numEventReport != -1) {
					ReportErrorEvent event2 = new ReportErrorEvent();
					setChanged();
					notifyObservers(event2);
				}

				
				 /* if(numlines%100000==0)
				  System.out.println(numlines+" events created");
				 */
			}
		} catch (IOException e) {

		} finally {
			if (iterator != null)
				iterator.close();
		}

		// System.out.println("Number of events created is "+numlines);
		return numlines;
	}


	private UserTrainEvent processLine(String line) {
		Iterator<String> tokens=this.split(line,MAX_FIELDS);
		
	
			 String userIDString = new String(tokens.next());
			 String itemIDString = new String(tokens.next());
			 String preferenceValueString = new String(tokens.next());
			 boolean hasTimestamp = tokens.hasNext();
			 String timestampString = hasTimestamp ? new String(tokens.next()) : null;
			 boolean hasMetadata = tokens.hasNext();
			 HashSet<Long> metadata=hasMetadata? breakConcepts(new String(tokens.next())):null;
			 
		return new UserTrainEvent(Long.parseLong(userIDString), Long.parseLong(itemIDString), preferenceValueString, Long.parseLong(timestampString),metadata);
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
	
	private HashSet<Long> breakConcepts(String metadataVector) {
		HashSet<Long> concepts= new HashSet<Long>();
		
		StringBuilder builder= new StringBuilder();
		
		for (int i = 0; i < metadataVector.length(); i++) {
			char at= metadataVector.charAt(i);
			if( this.metaDataSeparators.contains(at) ){
				if(builder.length()>0){
					try{
					Long concept=Long.parseLong(new String(builder.toString()));
					
					if(!allConcepts.containsKey(concept))
						allConcepts.put(concept,concept);
					
					concepts.add(allConcepts.get(concept));
					}catch(NumberFormatException e){};
				}
					builder= new StringBuilder();
				
			}
			else{
				builder.append(at);
			}
			
		}
		if(builder.length()>0){
			try{
				Long concept=Long.parseLong(new String(builder.toString()));
				if(!allConcepts.containsKey(concept))
					allConcepts.put(concept,concept);
				
				concepts.add(allConcepts.get(concept));
				}catch(NumberFormatException e){};
		}
		
		
		
		
		return concepts;
	}
	
	

}
