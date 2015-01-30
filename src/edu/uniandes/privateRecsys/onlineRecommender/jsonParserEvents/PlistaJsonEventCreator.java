package edu.uniandes.privateRecsys.onlineRecommender.jsonParserEvents;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

import edu.uniandes.privateRecsys.onlineRecommender.vo.ClickImpressionEvent;
import edu.uniandes.privateRecsys.onlineRecommender.vo.CreateUpdateEvent;
import edu.uniandes.privateRecsys.onlineRecommender.vo.FileEvent;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UnableToParseEvent;


/**
 * In charge of parsing plista log files to recreate the events for the registered observers
 * @author Andres M
 *
 */
public class PlistaJsonEventCreator extends Observable{

	private File directory;
	private JsonFactory jsonFactory= new JsonFactory();
	private int currentDay;
	private int limitDay;
	private BufferedReader[] readers= null;
	private FileEvent[] currentEvents= null;
	private LinkedList<String> prefixes= new LinkedList<String>();
	private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private int numEventsCreated;
	private String suffixString="";
	
	
	/**
	 * Constructor of the event creator
	 * @param directory directory where the files are hosted
	 * @param dayFrom begining day of logs
	 * @param dayTo end date of logs
	 * @throws IOException if the files are not found or an error reading files is produced
	 */
	public PlistaJsonEventCreator(String directory, int dayFrom, int dayTo,LinkedList<String> prefixes) throws IOException{
		this.directory=new File(directory);
		if(!this.directory.isDirectory()) throw new IllegalStateException(directory +"is not a directory");
		this.currentDay=dayFrom;
		this.limitDay=dayTo;
		this.prefixes=prefixes;
		
		readers= new BufferedReader[prefixes.size()];
		currentEvents= new FileEvent[prefixes.size()];
		
	}
	/**
	 * Method that starts the notifications of the file parsing
	 * @throws FileNotFoundException
	 */
	public void startEvents() throws FileNotFoundException{
		
		while(this.currentDay<=this.limitDay){
			
			 this.suffixString="_2013-06-"+String.format("%02d", this.currentDay)+".log";
			System.out.println(suffixString);
			openFileReaders(suffixString);
			FileEvent nextFileEvent= null;
			while((nextFileEvent=getNextEvent())!=null){
				
				setChanged();
				notifyObservers(nextFileEvent);
				numEventsCreated++;
			
			}
			
				closeReaders();
				this.currentDay++;
			
			
		}
	}
	
	private FileEvent getNextEvent() {
		
		fillMostRecentItems();
		FileEvent mostRecent=null;
		Long timestamp=Long.MAX_VALUE;
		int posRecent=-1;
		for (int i = 0; i < currentEvents.length; i++) {
			if(currentEvents[i]!=null ){
				if(currentEvents[i].getTimestamp()<timestamp){
					mostRecent=currentEvents[i];
					timestamp=mostRecent.getTimestamp();
					posRecent=i;
				}
			}
		}
		if(mostRecent!=null)
			currentEvents[posRecent]=null;
		
		return mostRecent;
	}

	private void fillMostRecentItems() {
		
		for (int i = 0; i < currentEvents.length; i++) {
			if(currentEvents[i]==null)
				try {
					FileEvent ev=null;
					boolean validValue=false;
					do{
						String line=readers[i].readLine();
						if(line!=null)
							ev=processLine(prefixes.get(i),line);
						
						validValue=line==null || (ev!=null && ev.getTimestamp()!=-1);
					}while(!validValue);
					currentEvents[i]=ev;
				} catch (JsonParseException e) {
					
					e.printStackTrace();
				} catch (IOException e) {
				
					e.printStackTrace();
				}
		}
		
	}

	private void closeReaders() {
		for (int i = 0; i < readers.length; i++) {
			if(readers[i]!=null){
				try {
					readers[i].close();
				} catch (IOException e) {}
			}
		}
		
		
	}

	private void openFileReaders(String suffixString) throws FileNotFoundException {
		for (int i = 0; i < readers.length; i++) {
			readers[i]= new BufferedReader(new FileReader(new File(directory,prefixes.get(i)+suffixString)));
		}
		
	}

	
	

	private FileEvent processLine(String type,String line) throws JsonParseException, IOException {
		
			HashMap map=null;
			try{
				if(line!=null){
					JsonParser parser=this.jsonFactory.createJsonParser(line);
				
					ObjectMapper mapper = new ObjectMapper();
					parser.setCodec(mapper);
				
				
					map=parser.readValueAs(HashMap.class);
					parser.close();
				}
				else{
					return null;
				}	
				
			}
			catch(java.io.EOFException e){
				return null;
			}
			catch(Exception e){
				e.printStackTrace();
				
			};
				
			if(map!=null){
				long userId=0;
				long itemId=-1;
				
				if(type.equals("create")||type.equals("update")){
					return processMapAsCreateUpdate(type,map);
				}
				else if(type.equals("click")||type.equals("impression")){
					return processMapAsClickImpression(type,map,line);
				}
				
					
				
				Long timeStamp=(Long) map.get("timestamp");
				LinkedHashMap ctx=(LinkedHashMap) map.get("context");
				LinkedHashMap simple=(LinkedHashMap) ctx.get("simple");
				try{
				itemId=Long.parseLong(simple.get("25").toString());
				}catch(Exception e){};
				try{
				userId=Long.parseLong(simple.get("57").toString());
				}catch(Exception e){};
					
		
			}
			return new UnableToParseEvent();
	
	}

	
	private FileEvent processMapAsCreateUpdate(String type,HashMap map) {
		/*{"domainid":"1677","created_at":"2013-06-01 00:00:29","flag":8,"title":"\"Berlin Tag &amp; Nacht\" Fan-Tour","url":"http:\/\/www.tagesspiegel.de\/mediacenter\/videos\/berlin\/berlin-tag-und-nacht-fan-tour\/8281900.html","img":"","text":"","published_at":null,"version":1,"updated_at":"2013-06-01 00:00:29","id":"127965616"}

		 **/
		
		long itemId=-1;
		Object itemIdObj= map.get("id");
		
		try {
			itemId=Long.parseLong(itemIdObj.toString());
		} catch (NumberFormatException e) {}
		
		if(itemId==-1)
			return new UnableToParseEvent();
		
		long created_at_long=-1;
		String created_at=(String) map.get("created_at");
		if(created_at!=null)
			try {
				created_at_long=this.formatter.parse(created_at).getTime();
			} catch (ParseException e) {}
		long updated_at_long=-1;
		String updated_at=(String) map.get("updated_at");
		if(updated_at!=null)
			try {
				updated_at_long=this.formatter.parse(updated_at).getTime();
			} catch (ParseException e) {}
		
		
		FileEvent ret=new CreateUpdateEvent(type,itemId,created_at_long>updated_at_long?created_at_long:updated_at_long);
		return ret;
	}
	
	private FileEvent processMapAsClickImpression(String type, HashMap map, String line) {
		/*{"type":"click","context":{"simple":{"4":457399,"5":317849,"6":10,"7":18849,"9":26886,"13":1,"14":33331,"16":48812,"17":48985,"18":0,"19":52193,"20":75,"22":62177,"23":23,"24":1,"25":127830214,"27":694,"29":17332,"35":315148,"37":1199528,"39":4487,"42":0,"47":654013,"49":47,"52":1,"56":1138207,"57":2309723554},"lists":{"8":[18841,18842],"10":[3,9,14],"11":[157987]},"clusters":{"1":{"8":255},"2":[19,15,79,53,65,15,6],"3":[86,40,22,72,20,12],"33":{"35112":7,"1782157":7,"4751424":6,"35607788":6,"3997463":5,"7415943":4,"209820":2,"75193":2,"4122":2,"75931":2,"94701":2,"7046":2,"2056234":2,"6015":1,"1673":1,"4736504":1,"9513234":1,"381":1,"19091":1,"2188523":1,"273283":0,"18232":0,"69418":0},"46":{"793434":255},"51":{"5":255}}},"recs":{"ints":{"3":[127691561]}},"timestamp":1370037599973}*/
		
		long itemId=-1;
		long userId=-1;
		long publisherID=-1;
		
		Long timeStamp=(Long) map.get("timestamp");
		LinkedHashMap ctx=(LinkedHashMap) map.get("context");
		LinkedHashMap simple=(LinkedHashMap) ctx.get("simple");
		LinkedHashMap clusters=null;
		Map keywords=null;
		if(ctx.get("clusters") instanceof LinkedHashMap)
			clusters=(LinkedHashMap) ctx.get("clusters");
		
		try{
			if(simple!=null && simple.get("25")!=null)	
				itemId=Long.parseLong(simple.get("25").toString());
		}catch(Exception e){e.printStackTrace();};
		try{
			if(simple!=null && simple.get("57")!=null)	
				userId=Long.parseLong(simple.get("57").toString());
		}catch(Exception e){e.printStackTrace();};
		try{
			if(simple!=null && simple.get("27")!=null)	
				publisherID=Long.parseLong(simple.get("27").toString());
		}catch(Exception e){e.printStackTrace();};
		try{
			
			if(clusters!=null && clusters.get("33")!=null)
				if(clusters.get("33") instanceof Map)
					keywords=(Map) clusters.get("33");
		}catch(Exception e){e.printStackTrace();};
		
		FileEvent ret= new ClickImpressionEvent(type,itemId,userId,publisherID,timeStamp,keywords,line);
		return ret;
	}
	
	
	public static void main(String[] args) {
		long timeinit=System.nanoTime();
		Observer obs= new Observer(){
			public int numNotif=0;
			@Override
			public void update(Observable o, Object arg) {
				
				numNotif++;
				if(numNotif%100000==0)
					System.out.println(numNotif);
			}
			
		};
		try {
			LinkedList<String> prefixes= new LinkedList<String>();
			prefixes.add("create");
			prefixes.add("update");
			prefixes.add("click");
			prefixes.add("impression");
			
			PlistaJsonEventCreator plistaEventCreator=new PlistaJsonEventCreator("data/plista", 01, 10, prefixes);
			plistaEventCreator.addObserver(obs);
			plistaEventCreator.startEvents();
			System.out.println(plistaEventCreator.getNumEventscreated());
						
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(System.nanoTime()-timeinit);
		
	}
	private int getNumEventscreated() {
		return this.numEventsCreated;
		
	}
	public String getCurrentSuffix() {
		// TODO Auto-generated method stub
		return this.suffixString;
	}

}

