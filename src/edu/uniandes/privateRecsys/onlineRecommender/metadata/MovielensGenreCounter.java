package edu.uniandes.privateRecsys.onlineRecommender.metadata;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import org.apache.mahout.math.Matrix;

public class MovielensGenreCounter {

	
	
	public void genreCount(String trainFile) throws IOException{
		Matrix c=MetadataMapFileLoader.loadBinaryMetadataMap(trainFile);
		
		Set<String> ss=c.getColumnLabelBindings().keySet();
		for (String string : ss) {
			if(string.startsWith("genre")){
				System.out.println(string+" id:"+c.getColumnLabelBindings().get(string));
			}
		}
		
		
	}
	
	public static void main(String[] args) {
		String file="data/ml-10M100K/metadata/mapFileUpdatedFinal.data";
		try {
			new MovielensGenreCounter().genreCount(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}
