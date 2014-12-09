package edu.uniandes.privateRecsys.onlineRecommender;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Observable;

import org.jfree.util.Log;

import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.vo.ReportErrorEvent;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;



public class FileEventCreator extends Observable{

	
	
	private static int defaultCharBufferSize = 8192*3;
	private static final int MAX_FIELDS=5;
	private HashSet<String> separators= new HashSet<String>();
	

	
	private int numEventReport;
	private int limitEvents;
	private File file;
	
	public FileEventCreator(File file, int numEventReport, int limitEvents) throws IOException {
		this.file=file;
		
		this.separators.add(":");
		this.separators.add(",");
		this.separators.add(""+'\t');
		
		
				
		this.numEventReport=numEventReport;
		this.limitEvents=limitEvents;
	}
	
	
	public int startEvents() throws PrivateRecsysException, IOException {

		BufferedReader iterator = null;
		int numlines = 0;
		try {
			iterator = new BufferedReader(new FileReader(file),defaultCharBufferSize);

			if (this.countObservers() == 0) {

				throw new PrivateRecsysException(
						"No observers registered for experiment, cancelling ...");
			}
			String line =null;
			while ((line=iterator.readLine())!=null) {

				 
				UserTrainEvent event = processLine(line);
				setChanged();
				notifyObservers(event);
				numlines++;
				if (limitEvents == numlines) {
					try{
						iterator.close();
						return limitEvents;
					}catch(Exception e){
						
					}
				}
				if ((numlines % this.numEventReport) == 0
						&& this.numEventReport != -1) {
					ReportErrorEvent event2 = new ReportErrorEvent();
					setChanged();
					notifyObservers(event2);
				}

				
			/*	 if(numlines%100000==0)
				  System.out.println(numlines+" events created");*/
				 
			}
		} catch (IOException e) {

		} finally {
			if (iterator != null)
				try{
					iterator.close();
				}catch (Exception e2) {}
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
	
	
	
	

}
