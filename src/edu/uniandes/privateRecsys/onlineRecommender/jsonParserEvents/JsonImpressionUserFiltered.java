package edu.uniandes.privateRecsys.onlineRecommender.jsonParserEvents;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

import javax.management.RuntimeErrorException;

import edu.uniandes.privateRecsys.onlineRecommender.vo.ClickImpressionEvent;
import edu.uniandes.privateRecsys.onlineRecommender.vo.FileEvent;

public class JsonImpressionUserFiltered implements Observer {
	
	//private HashMap<Long, Integer> itemIdsCount= new HashMap<>();
	private File directory;
	private File outputDirectory;

	private HashSet<Long> restrictedUserIds= new HashSet<Long>();
	private String currentSuffix;
	private PrintWriter clickPrintWriter;
	private PrintWriter impressionPrintWriter;
	
	
	public JsonImpressionUserFiltered(String directory, String ouputDirectory, String userFile) throws IOException{
		this.directory=new File(directory);
		this.outputDirectory=new File(ouputDirectory);
	
		LinkedList<String> prefixes=new LinkedList<String>();
		loadUsers(userFile);
		
		prefixes.add("click");
		prefixes.add("impression");
		PlistaJsonEventCreator plistaEventCreator=new PlistaJsonEventCreator(directory, 1, 30, prefixes);
		plistaEventCreator.addObserver(this);
		plistaEventCreator.startEvents();
		closePrintWriters();
		
		
	}
	
	private void loadUsers(String usersFile) throws  IOException {
		BufferedReader red=null;
		String line= null;
		try{
			red=new BufferedReader(new FileReader(usersFile));
			while((line=red.readLine())!=null){
				String id=line.split(",")[0];
				if(!id.equals("0")&&!id.equals("null")){
					id=id.replaceAll("[\\\"]", "");
					Long longId=Long.parseLong(id);
					restrictedUserIds.add(longId);
				}
			}
		} catch (FileNotFoundException e) {
			throw e;
		} catch (NumberFormatException e) {} 
		catch (IOException e) {
			
			throw e;
		}finally{
			if(red!=null)
				try {
					red.close();
				} catch (IOException e) {}
		}
		
	}

	@Override
	public void update(Observable o, Object arg) {
		PlistaJsonEventCreator observable=(PlistaJsonEventCreator) o;
		FileEvent event=(FileEvent) arg;
		if(event.getEventType().equals(FileEvent.ITEM_CLICKED_EVENT)||event.getEventType().equals(FileEvent.ITEM_IMPRESSION_EVENT)){
			ClickImpressionEvent clickedImpressionEv=(ClickImpressionEvent) event;
			try {
				processClickImpressionEvent(observable.getCurrentSuffix(),clickedImpressionEv);
			} catch (FileNotFoundException e) {
				
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		
	}

	private void processClickImpressionEvent(
			String suffix, ClickImpressionEvent clickedImpression) throws FileNotFoundException {
		if(this.currentSuffix!=suffix){
			this.currentSuffix=suffix;
			closePrintWriters();
			updatePrintWriters();
		}
		if (this.restrictedUserIds.contains(clickedImpression.getUserId())) {
			if (clickedImpression.getEventType().equals(
					FileEvent.ITEM_CLICKED_EVENT)) {
				clickPrintWriter.println(clickedImpression.getLine());
			}
			if (clickedImpression.getEventType().equals(
					FileEvent.ITEM_IMPRESSION_EVENT)) {
				impressionPrintWriter.println(clickedImpression.getLine());
			}
		}
		
		
		
	}

	private void updatePrintWriters() throws FileNotFoundException {
		clickPrintWriter= new PrintWriter(new File(outputDirectory,"click"+this.currentSuffix));
		impressionPrintWriter= new PrintWriter(new File(outputDirectory,"impression"+this.currentSuffix));
	}

	private void closePrintWriters() {
		if(clickPrintWriter!=null)
			clickPrintWriter.close();
		if(impressionPrintWriter!=null)
			impressionPrintWriter.close();
	}
	
	public static void main(String[] args) throws IOException {
		new JsonImpressionUserFiltered("data/plista","data/plista/filtered","data/plista/usersCount.csv");
	}
	
	
	
	

	
	

}

