package edu.uniandes.privateRecsys.onlineRecommender.metadata;

import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.mahout.math.DenseMatrix;
import org.apache.mahout.math.Matrix;
import org.apache.mahout.math.SparseMatrix;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;



/**
 * Loads a matrix with a lower dimensional representation of a metadata file from a sequential mahout file
 * @author Andres M
 *
 */
public class SpectralDataLoader {
	
	private final static Logger LOG = Logger.getLogger(SpectralDataLoader.class
		      .getName()); 
	
	
	public static Matrix loadSpectralRepresentation(String path) throws IOException{
		
		
		LinkedList<Vector> vectorRepresentations=new LinkedList<Vector>();
		int numCols=0;
		Configuration conf= new Configuration();
		SequenceFile.Reader reader=null;
		try {
			reader = new SequenceFile.Reader(FileSystem.get(conf), new Path(path), conf);
			IntWritable key= new IntWritable();
			VectorWritable vector= new VectorWritable();
			
			while(reader.next(key, vector)){
				try{
					Vector cl = vector.get();
					numCols=cl.size();
					vectorRepresentations.add(cl);
				}
				catch(Exception e){
					
				}
			}	
		} catch (IOException e1) {
			throw e1;
		}finally{
			if(reader!=null)
				try {
					reader.close();
				} catch (IOException e) {}
		}
		Matrix mat= new DenseMatrix(vectorRepresentations.size(),numCols);
		int rowNum=0;
		for (Vector vec : vectorRepresentations) {
			mat.assignRow(rowNum++, vec);
		}
		return mat;
		
	}
	
	public static void main(String[] args) {
		try {
			Matrix mat=SpectralDataLoader.loadSpectralRepresentation("data/ml-10M100K/metadata/unitvectors/spectral-10-2");
			for (int i = 0; i < mat.numRows(); i++) {
				System.out.println(mat.viewRow(i));
			}
			System.out.println(mat.numRows());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
