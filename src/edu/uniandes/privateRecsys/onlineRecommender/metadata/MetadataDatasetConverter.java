package edu.uniandes.privateRecsys.onlineRecommender.metadata;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.management.RuntimeErrorException;

import org.apache.mahout.common.iterator.FileLineIterator;
import org.apache.mahout.math.Matrix;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.FileEventCreator;
import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.vo.UserTrainEvent;

public class MetadataDatasetConverter implements Observer {
	
	
	
	
	private PrintWriter writer;
	private Matrix mat;
	private Map<String, Integer> bindings;
	

	public void convertDatasetFile(String inputDatasetFile, String metadataMapFile,String outputDatasetFile) throws IOException, PrivateRecsysException{
		this.writer= new PrintWriter(new FileWriter(outputDatasetFile));
		this.mat=MetadataMapFileLoader.loadBinaryMetadataMap(metadataMapFile);
		this.bindings=mat.getRowLabelBindings();
	
		
		FileEventCreator fec=  new FileEventCreator(new File(inputDatasetFile), -1, -1);
		fec.addObserver( this);
		fec.startEvents();
		
		
		int numLinesIterator=0;
		try {
			/*int row186=bindings.get("2688");
			Vector vectorTest=mat.viewRow(row186);
			for (int i = 0; i < vectorTest.size(); i++) {
				if(vectorTest.get(i)==1){
					for (String key : colBindings.keySet()) {
						if(colBindings.get(key)==i)
							System.out.println("Col "+i+" binding in 2688, value "+key);
					}
				}
				
			}
			*/
			
			
		
			
			
				
			
		
			
			
			
			
			
		}catch(Exception e){
			System.err.println(numLinesIterator);
			e.printStackTrace();
		}finally{
			
			if(writer!=null)
				writer.close();
		}
		
		
		
	}
	
	public static void main(String[] args) throws IOException {
		
		String originalFile="data/ml-10M100K/orderedRatings.dat";
		String metadataMapFile="data/ml-10M100K/metadata/mapFileUpdatedFinal.data";
		//Matrix mat=MetadataMapFileLoader.loadBinaryMetadataMap(metadataMapFile);
		//System.out.println(mat.columnSize()+" "+mat.rowSize());
		try {
			new MetadataDatasetConverter().convertDatasetFile(originalFile, metadataMapFile, originalFile+".meta2");
		} catch (PrivateRecsysException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void update(Observable o, Object arg) {
		UserTrainEvent event = (UserTrainEvent) arg;
		
		String itemId=""+event.getItemId();
		Integer row= bindings.get(itemId);
		if(row!=null){
			Vector vectorRow = mat.viewRow(row);
			
		
			writer.println(event.getUserId()+","+event.getItemId()+","+event.getRating()+","+event.getTimestamp()+","+vectorRow.asFormatString());
		}
		else{
			throw new RuntimeException("Item id "+itemId+" not found ");
		}
		
	}

}
