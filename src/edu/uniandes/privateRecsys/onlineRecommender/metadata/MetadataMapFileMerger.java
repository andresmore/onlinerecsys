package edu.uniandes.privateRecsys.onlineRecommender.metadata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;

public class MetadataMapFileMerger {
	
	
	
	public void mergeFiles(String movieFileMap, String mapFile, String destFile) throws IOException{
		HashMap<Long, HashSet<String>> movieDatConcepts=new HashMap<Long, HashSet<String>>();
		BufferedReader bf=null;
		try {
			bf= new BufferedReader(new FileReader(movieFileMap));
			String line=null;
			while((line=bf.readLine())!=null){
				
				String[] arr=line.split("::");
				if(arr.length>=3){
					Long movieId=Long.parseLong(arr[0]);
					String movieTitle=arr[1].replaceAll("\\s+","") ;
					movieTitle=movieTitle.replaceAll("\\s+","") ;
					String[] genres=arr[2].split("\\|");
					HashSet<String> concepts= new HashSet<>();
					concepts.add("id:"+movieId+"=1.0");
					concepts.add("movie:"+movieTitle+"=1.0");
					for (int i = 0; i < genres.length; i++) {
						concepts.add("genre:"+genres[i]+"=1.0");
					}
					movieDatConcepts.put(movieId, concepts);
				}
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(bf!=null){
				try {
					bf.close();
				} catch (IOException e) {}
			}	
		}
		System.out.println("MovieDataFile parsed");
		
		BufferedReader bf2=null;
		PrintWriter pr=null;
		try {
			bf2= new BufferedReader(new FileReader(mapFile));
			pr= new PrintWriter(new File(destFile));
			String line=null;
			while((line=bf2.readLine())!=null){
				pr.println(line);
				line=line.replace("{", "");
				line=line.replace("}", "");
				String[] hashMap=line.split(", ");
				for (int i = 0; i < hashMap.length; i++) {
					String valueKey=hashMap[i];
					//System.out.println(valueKey);
					String[]arrValKey=valueKey.split("=");
					String concept=arrValKey[0];
					
					if(concept.startsWith("id")){
					
						String id=concept.split(":")[1];
						HashSet<String> removed=movieDatConcepts.remove(Long.parseLong(id));
						if(removed!=null){
							System.out.println("Removed from hashmap "+id);
						}	
					}
					
				}
				
			}
			
			System.out.println(" hashmap end size is "+movieDatConcepts.size());
			System.in.read();
			for (Long movieId : movieDatConcepts.keySet()) {
				HashSet<String> concepts=movieDatConcepts.get(movieId);
				String conceptRepresentation=concepts.toString().replaceAll("\\[", "{");
				conceptRepresentation=concepts.toString().replaceAll("\\]", "}");
				pr.println(conceptRepresentation);
				System.out.println();
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(bf2!=null)
				bf2.close();
			if(pr!=null)
				pr.close();
		}
		
	}
	
	public static void main(String[] args) {
		try {
			new MetadataMapFileMerger().mergeFiles("data/ml-10M100K/movies.dat", "data/ml-10M100K/metadata/mapFile.data", "data/ml-10M100K/metadata/mapFileUpdated.data");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

}
