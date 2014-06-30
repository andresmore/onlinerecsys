package edu.uniandes.privateRecsys.onlineRecommender.jsonParserEvents.ted;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

/**
 * Parses de TedDataset to create a MAHOUT recsys version of the files with the metadata format
 * @author Andres M
 *
 */
public class TedDatasetJSonParser {
	
	private JsonFactory jsonFactory= new JsonFactory();
	private String talkFile;
	private String userFile;
	private String endFile;
	
	private HashMap<String, Long> userIds= new HashMap<String, Long>(); 
	
	
	
	public TedDatasetJSonParser(String talkFile, String userFile, String endFile) throws JsonParseException, JsonMappingException, IOException{
		this.talkFile=talkFile;
		this.userFile=userFile;
		this.endFile=endFile;
		
		
	}
	
	public List<Talk> parseTalkFile() throws JsonParseException, JsonMappingException, IOException{
		 ObjectMapper mapper = new ObjectMapper();
		 mapper.configure(
				    DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		 List<Talk> talks = mapper.readValue(new File(talkFile), new TypeReference<List<Talk>>() {});
		 
		 return talks;
	}
	
	public void createEndFile() throws JsonParseException, JsonMappingException, IOException{
		
		List<Talk> userList=parseTalkFile();
		for (Talk talk : userList) {
			
		}
		
	}
	
	
	public static void main(String[] args) {
		try {
			new TedDatasetJSonParser("C:\\Users\\Andres M\\Documents\\Documents\\datasets\\ted_dataset\\TED_dataset\\ted_talks-25-Apr-2012.json", 
					"C:\\Users\\Andres M\\Documents\\Documents\\datasets\\ted_dataset\\TED_dataset\\ted_users-25-Apr-2012.json", 
					"C:\\Users\\Andres M\\Documents\\Documents\\datasets\\ted_dataset\\TED_dataset\\finaFile.dat").createEndFile();
			
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
