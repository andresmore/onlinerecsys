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
	}
	
	public static LinkedList<Long> breakConcepts(String metadataVector) {
		HashSet<Long> concepts= new HashSet<Long>();
		LinkedList<Long> conceptsRet=new LinkedList<>();
		StringBuilder builder= new StringBuilder();
		
		for (int i = 0; i < metadataVector.length(); i++) {
			char at= metadataVector.charAt(i);
			if( metaDataSeparators.contains(at) ){
				if(builder.length()>0){
					try{
					Long concept=Long.parseLong(new String(builder.toString()));
					
					concepts.add(concept);
					}catch(NumberFormatException e){};
				}
					builder= new StringBuilder();
				
			}
			else{
				builder.append(at);
			}
			
		}
		if(builder.length()>0){
			try{
				Long concept=Long.parseLong(new String(builder.toString()));
				
				concepts.add(concept);
				}catch(NumberFormatException e){};
		}
		
		conceptsRet.addAll(concepts);
		
		
		
		
		return conceptsRet;
	}

}
