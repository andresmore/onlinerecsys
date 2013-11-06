package edu.uniandes.privateRecsys.onlineRecommender.jsonParserEvents;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

public class JsonImpressionClickMetadataConsolidator{
	
	//private HashMap<Long, Integer> itemIdsCount= new HashMap<>();
	private File directory;
	private JsonFactory jsonFactory= new JsonFactory();
	
	private HashMap<Long, Map> itemId_bucketMap=new HashMap<>();
	private String dataFile;
	
	public JsonImpressionClickMetadataConsolidator(String directory, String dataFile) {
		this.directory=new File(directory);
		this.dataFile=dataFile;
		
		
		
	}
	
	

	public void createKeyworkdFile() throws JsonParseException, IOException{
		if(!directory.isDirectory()) throw new IllegalStateException("file "+directory+" is not directory");
        for(File file : directory.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return ((name.startsWith("impression") || (name.startsWith("click")))&& name.endsWith(".log"));
				
			}
		})) {
               
        		processSingleFile(file);
        }
        
        printFile();
	}

	

	private void printFile() throws FileNotFoundException {

		PrintWriter pr=null;
		int numPrinted=0;
		try{
			pr= new PrintWriter(new File(this.directory,this.dataFile));
			for (Long itemId : this.itemId_bucketMap.keySet()) {
				Map mp=this.itemId_bucketMap.get(itemId);
				pr.println(mp);
				numPrinted++;
				
				if(numPrinted%1000==0)
					System.out.println(numPrinted+" map lines");
			}
			
			
		}finally{
			if(pr!=null)
				pr.close();
			System.out.println(numPrinted+" map lines");
		}
		
	}



	private void processSingleFile(File file) throws FileNotFoundException {
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
				
			if(!completed&&map!=null){
				
				long itemId=-1;
				long publisherID=-1;
				String type=(String) map.get("type");
				Long timeStamp=(Long) map.get("timestamp");
				LinkedHashMap ctx=(LinkedHashMap) map.get("context");
				LinkedHashMap simple=(LinkedHashMap) ctx.get("simple");
				LinkedHashMap clusters=null;
				if(ctx.get("clusters") instanceof LinkedHashMap)
					clusters=(LinkedHashMap) ctx.get("clusters");
				
				try{
					if(simple!=null && simple.get("25")!=null)	
						itemId=Long.parseLong(simple.get("25").toString());
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
				
				if(itemId!=-1 ){
					
					int sizeKeywords=0;
					if(keywords!=null){
						sizeKeywords=keywords.size();
					}
					Map mapKeywords=itemId_bucketMap.get(itemId);
					if(mapKeywords==null || mapKeywords.size()<(sizeKeywords+2)){
						HashMap aggregatedMap= new HashMap();
						if(keywords!=null){
							Set keywordsSet=keywords.keySet();
							for (Object object : keywordsSet) {
								aggregatedMap.put("keyword:"+object.toString(), keywords.get(object));
							}
						}
						aggregatedMap.put("id:"+itemId, 1);
						aggregatedMap.put("publisherId:"+publisherID, 1);
						itemId_bucketMap.put(itemId, aggregatedMap);
						long numItems=itemId_bucketMap.size();
						
						if(numItems%10000==0){
							System.out.println("Num items is now "+numItems);
						}
					}
				}
				
		
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
	
	private Map mergeMaps(Map mapKeywords, Map keywords) {
		//TODO:FIX
		 mapKeywords.putAll(keywords);
		return mapKeywords;
	}



	public static void main(String[] args) {
		try {
			new JsonImpressionClickMetadataConsolidator("data/plista","clickedItemsKeywords2.map").createKeyworkdFile();
			//new JsonImpressionClickMetadataConsolidator("data/plista","clickedItemsKeywords.map").processSingleFile(new File("data/plista/impression_2013-06-05.log"));
		} catch (IOException e) {
			
			e.printStackTrace();
		} catch(RuntimeException e){
			e.printStackTrace();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	

}

