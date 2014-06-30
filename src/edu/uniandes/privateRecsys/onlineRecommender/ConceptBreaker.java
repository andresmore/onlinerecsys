package edu.uniandes.privateRecsys.onlineRecommender;

import java.util.HashSet;
import java.util.LinkedList;

public class ConceptBreaker {
	
	private static  HashSet<Character> metaDataSeparators= new HashSet<Character>();
	static{
		metaDataSeparators= new HashSet<>(4);
		metaDataSeparators.add('{');
		metaDataSeparators.add('}');
		metaDataSeparators.add(',');
		metaDataSeparators.add(':');
		metaDataSeparators.add('.');
	}
	
	public static LinkedList<Long> breakConcepts(String metadataVector) {
		HashSet<Long> concepts= new HashSet<Long>();
		LinkedList<Long> conceptsRet=new LinkedList<>();
		long endLong=-1;
		final char[] charArray=metadataVector.toCharArray();
		
		for (int i = 0; i < metadataVector.length(); i++) {
			char at= charArray[i];
			if( metaDataSeparators.contains(at) ){
				if(at==':'){
					//TODO:take into account feature weight into calculations
					i+=4;
					
				}		
				if(endLong>-1){
					
					concepts.add(endLong);
					
				}
				endLong= -1;
				
			}
			else{
				if(endLong==-1)
						endLong=0;
				
				endLong = endLong * 10 + (int)(at - '0');
				
			}
			
		}
		if(endLong>-1){
			concepts.add(endLong);
		}
		
		conceptsRet.addAll(concepts);
		
		
		
		
		return conceptsRet;
	}

}
