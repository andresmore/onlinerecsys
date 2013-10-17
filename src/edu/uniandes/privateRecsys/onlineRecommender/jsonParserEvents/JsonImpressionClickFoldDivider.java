package edu.uniandes.privateRecsys.onlineRecommender.jsonParserEvents;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Observable;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.uncommons.maths.random.XORShiftRNG;

public class JsonImpressionClickFoldDivider {
	
	//private HashMap<Long, Integer> itemIdsCount= new HashMap<>();
	private File directory;
	private JsonFactory jsonFactory= new JsonFactory();
	private int nFold;
	private File totalItemFile;
	private PrintWriter[] printWriters;
	private int[] bucketDistrib;
	private HashMap<Long, Integer> itemId_bucketMap=new HashMap<>();
	
	public JsonImpressionClickFoldDivider(String directory, int nFold, String prefix) throws IOException{
		this.directory=new File(directory);
		this.totalItemFile=new File(this.directory.getAbsolutePath()+File.separatorChar+"clickedItems.csv");
		this.nFold=nFold;
		this.printWriters= new PrintWriter[nFold];
		this.bucketDistrib= new int[nFold];
		for (int i = 0; i < printWriters.length; i++) {
			printWriters[i]= new PrintWriter(new File(directory, prefix+i));
		}
		fillBucketsItemFile();
		
		
	}
	
	private void fillBucketsItemFile() throws IOException {
		XORShiftRNG genRand= new XORShiftRNG();
		
		BufferedReader red=null;
		String line= null;
		try{
			red=new BufferedReader(new FileReader(totalItemFile));
			while((line=red.readLine())!=null){
				String id=line.split(",")[0];
				if(!id.equals("0")&&!id.equals("null")){
					id=id.replaceAll("[\\\"]", "");
					Long longId=Long.parseLong(id);
					
					if(!itemId_bucketMap.containsKey(longId)){
						int bucket=genRand.nextInt(this.nFold);
						itemId_bucketMap.put(longId, bucket);
						this.bucketDistrib[bucket]++;
					}
				}
			}
		}finally{
			red.close();
		}
		String rep="";
		for (int i = 0; i < this.bucketDistrib.length; i++) {
			rep+=this.bucketDistrib[i];
			if(i<this.bucketDistrib.length-1)
				rep+=',';
		}
		System.out.println(rep);
		
	}
	

	public void createNFoldFiles() throws JsonParseException, IOException{
		if(!directory.isDirectory()) throw new IllegalStateException("wtf mate?");
        for(File file : directory.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return ((name.startsWith("impression") || (name.startsWith("click")))&& name.endsWith(".log"));
				
			}
		})) {
                processSingleFile(file);
        }
        closePrinters();
	}

	private void closePrinters() {
		for (int i = 0; i < printWriters.length; i++) {
			PrintWriter pr=null;
			try{
				pr=printWriters[i];	
			}finally{
				if(pr!=null)
					pr.close();
			}
		}
		
	}

	private void processSingleFile(File file) throws JsonParseException, IOException {
		System.out.println("Parsing file "+file);
BufferedReader bf=null;
		
		bf=new BufferedReader(new FileReader(file));
		boolean completed=false;
		HashMap map=null;
		Map keywords=null;
		
		
		try{
		
		
		do{
			try{
				String line=bf.readLine();
				if(line!=null){
					JsonParser parser=this.jsonFactory.createJsonParser(line);
				
					ObjectMapper mapper = new ObjectMapper();
					parser.setCodec(mapper);
				
				
					map=parser.readValueAs(HashMap.class);
					parser.close();
				}
				else{
					completed=true;
				}
				
			}
			catch(java.io.EOFException e){completed=true;}
			catch(Exception e){
				e.printStackTrace();
				
			};
				
			if(!completed&& map!=null){
				long userId=0;
				long itemId=-1;
				String type=(String) map.get("type");
				Long timeStamp=(Long) map.get("timestamp");
				LinkedHashMap ctx=(LinkedHashMap) map.get("context");
				LinkedHashMap simple=(LinkedHashMap) ctx.get("simple");
				try{
				itemId=Long.parseLong(simple.get("25").toString());
				}catch(Exception e){};
				try{
				userId=Long.parseLong(simple.get("57").toString());
				}catch(Exception e){};
				if(itemId!=-1){
					Integer bucket=itemId_bucketMap.get(itemId);
					StringBuilder sb= new StringBuilder();
					sb.append(userId);
					sb.append(",");
					sb.append(itemId);
					sb.append(",");
					sb.append(type.equals("click")?2:1);
					sb.append(",");
					sb.append(timeStamp);
					if(userId==0){
						for (int i = 0; i < printWriters.length; i++) {
							printWriters[i].println(sb.toString());
						}
					}
					else if(bucket!=null){
						PrintWriter pr=this.printWriters[bucket];
						pr.println(sb.toString());
						
					}
				}
				
				
			//System.out.println(tok.asString());
			//System.out.println(tok.asByteArray());
			
			//System.out.println();
			}
		}while (!completed) ;
		}finally{
			if(bf!=null)
				try {
					bf.close();
				} catch (IOException e) {}
			
			System.out.println("Num items is now "+this.itemId_bucketMap.size());
		}
		
		
	}
	/*
	public Map<Long,Integer> getSortedMap(){
		 MapValueComparator<Long,Integer> bvc =  new MapValueComparator<>(this.itemIdsCount);
	     TreeMap<Long,Integer> sorted_map = new TreeMap<Long,Integer>(bvc);
	     
	     sorted_map.putAll(this.itemIdsCount);
	     System.out.println(sorted_map);
	     return sorted_map;
	}
	
	*/
	
	public static void main(String[] args) throws JsonParseException, IOException {
		new JsonImpressionClickFoldDivider("data/plista", 5,"nFold-n").createNFoldFiles();
	}
	
	

}

