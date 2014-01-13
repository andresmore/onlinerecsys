package edu.uniandes.privateRecsys.onlineRecommender.metadata;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;

import org.apache.mahout.common.iterator.FileLineIterator;
import org.apache.mahout.math.Matrix;
import org.apache.mahout.math.Vector;

public class MetadataDatasetConverter {
	
	
	public void convertDatasetFile(String inputDatasetFile, String metadataMapFile,String outputDatasetFile){
		
		
		
		
		FileLineIterator iterator = null;
		PrintWriter writer=null;
		int numLinesIterator=0;
		try {
			Matrix mat=MetadataMapFileLoader.loadBinaryMetadataMap(metadataMapFile);
			Map<String, Integer> bindings=mat.getRowLabelBindings();
			Map<String, Integer> colBindings=mat.getColumnLabelBindings();
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
			
			iterator = new FileLineIterator(new File(inputDatasetFile));
			writer= new PrintWriter(new FileWriter(outputDatasetFile));
			
			while (iterator.hasNext()) {
				String line = new String(iterator.next());
				numLinesIterator++;
				//ItemId is in the second place
				String[] array=line.split(",|:");
				String itemId=array[1];
				Integer row= bindings.get(itemId);
				if(row!=null){
					Vector vectorRow = mat.viewRow(row);
					
					line+=","+vectorRow.asFormatString();
					writer.println(line);
				}
				else{
					throw new Exception("Item id "+itemId+" not found ");
				}
			
				
			}
			
			
			
			
			
		}catch(Exception e){
			System.err.println(numLinesIterator);
			e.printStackTrace();
		}finally{
			if(iterator!=null)
				try {
					iterator.close();
				} catch (IOException e) {}
			if(writer!=null)
				writer.close();
		}
		
		
		
	}
	
	public static void main(String[] args) {
		new MetadataDatasetConverter().convertDatasetFile("data/ml-1m/rb.test.cv", "data/ml-1m/mapFile.data", "data/ml-1m/rb.test.meta.cv");
	}

}
