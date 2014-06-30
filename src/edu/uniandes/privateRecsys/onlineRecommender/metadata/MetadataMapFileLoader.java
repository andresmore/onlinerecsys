package edu.uniandes.privateRecsys.onlineRecommender.metadata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.mahout.math.Matrix;
import org.apache.mahout.math.SparseMatrix;

public class MetadataMapFileLoader {
	
	private final static Logger LOG = Logger.getLogger(MetadataMapFileLoader.class
		      .getName()); 
	
	/**
	 * Creates a metadata matrix from the comma separated file, each line has the concepts of the matrix
	 * @param file the metadata file
	 * @return metadata matrix, null if not able to read or not found
	 * @throws IOException if reading the file has problems
	 */
	public static Matrix loadMetadataMap(String file, boolean applyIDF) throws IOException{
		System.out.println(file);
		File f= new File(file);
		BufferedReader reed= null;
		if(f.exists()){
			try {
				reed= new BufferedReader(new FileReader(f));
				Matrix metadataMatrix=loadPresentConcepts(reed, applyIDF,false);
				
				return metadataMatrix;
				
			} catch (IOException e) {
				throw e;
			}finally{
				if(reed!=null){
					try {
						reed.close();
					} catch (IOException e) {}
				}
			}
			
			
		}else{
			throw new FileNotFoundException(file+" not found");
		}
		
		
	}
	
	/**
	 * Creates a metadata matrix from the comma separated file, each line has the concepts of the matrix
	 * @param file the metadata file
	 * @return metadata matrix, null if not able to read or not found
	 * @throws IOException if reading the file has problems
	 */
	public static Matrix loadBinaryMetadataMap(String file) throws IOException{
		System.out.println(file);
		File f= new File(file);
		BufferedReader reed= null;
		if(f.exists()){
			try {
				reed= new BufferedReader(new FileReader(f));
				Matrix metadataMatrix=loadPresentConcepts(reed, false,true);
				
				return metadataMatrix;
				
			} catch (IOException e) {
				throw e;
			}finally{
				if(reed!=null){
					try {
						reed.close();
					} catch (IOException e) {}
				}
			}
			
			
		}else{
			throw new FileNotFoundException(file+" not found");
		}
		
		
	}
	
	private static Matrix loadPresentConcepts(BufferedReader reed, boolean applyIDF, boolean binary)
			throws FileNotFoundException, IOException {
		
		//Overall concepts present in the map and their position in the list
		HashMap<String, Integer> columnBindings= new HashMap<String, Integer>();
		//Frequency of concept in maps
		HashMap<String, Integer> conceptFrequency= new HashMap<String, Integer>();
		
		//File representation in memory
		LinkedList<HashMap<String, Double> > itemRepresentationMap= new LinkedList<HashMap<String,Double>>();
		
		//Bindings of the rows of the matrix
		Map<String,Integer> bindings= new HashMap<String, Integer>();
		
		
		
		String line=null;
		
		int numConcepts=0;
		int numItems=0;
	
		while((line=reed.readLine())!=null){
			//HashMap where the concept for each item will be reconstructed
			HashMap<String, Double> translation=new HashMap<String, Double>();
			line=line.replace("{", "");
			line=line.replace("}", "");
			line=line.replace("[", "");
			line=line.replace("]", "");
			String[] hashMap=line.split(", ");
			for (int i = 0; i < hashMap.length; i++) {
				String valueKey=hashMap[i];
				//System.out.println(valueKey);
				String[]arrValKey=valueKey.split("=");
				String concept=arrValKey[0];
				double value=Double.parseDouble(arrValKey[1]);
				if(!concept.startsWith("id")){
					translation.put(concept, value);
					if(!columnBindings.containsKey(concept)){
						
						columnBindings.put(concept, numConcepts);
						
						numConcepts++;
						conceptFrequency.put(concept, 1);
					}
					else{
						conceptFrequency.put(concept, conceptFrequency.get(concept)+1);
					}
				
				}
				else{
					String id=concept.split(":")[1];
					bindings.put(id, numItems);
				}
				
			}
			itemRepresentationMap.add(translation);
			
			numItems++;
			
			
		}
		LOG.info("Finished loading map into memory, matrix size will be ("+numItems+","+numConcepts+")");
		Matrix sparseItemRepresentation= new SparseMatrix(numItems, numConcepts);
		sparseItemRepresentation.setRowLabelBindings(bindings);
		sparseItemRepresentation.setColumnLabelBindings(columnBindings);
		fillInItemMatrix(sparseItemRepresentation,itemRepresentationMap,columnBindings, conceptFrequency, applyIDF,binary);
		
		
		return sparseItemRepresentation;
	}	
	
	private static void fillInItemMatrix(Matrix metadataMatrix,  LinkedList<HashMap<String, Double>> itemRepresentationMap, HashMap<String, Integer> existingConcepts, HashMap<String, Integer> conceptFrequency, boolean applyIDF, boolean binary) {
		
		int numItemsMetadata= metadataMatrix.numRows();
		for (int itemPosition = 0; itemPosition < itemRepresentationMap.size(); itemPosition++) {
			HashMap<String, Double> translation=itemRepresentationMap.get(itemPosition);
			
			for (String concept : translation.keySet()) {
				double value=translation.get(concept);
				int conceptPos=existingConcepts.get(concept);
				if(applyIDF){
					//Apply idf to columns of matrix
					int numValues=conceptFrequency.get(concept);
					double multValue=Math.log((double)numItemsMetadata/(double)numValues);
					value=numValues*multValue;
				}
				else if(binary){
					value=1;
				}
				metadataMatrix.setQuick(itemPosition, conceptPos, value);
				
			}
		}
		LOG.info("Metadata matrix filled");
		
	}
	
	
	public static void main(String[] args) {
		try {
			String file="data/plista/clickedItemsKeywords2.map";
			if(args.length>0){
				file=args[0];
				Matrix meta=MetadataMapFileLoader.loadMetadataMap(file,true);
			}
			
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}		
		
		
	}

}
