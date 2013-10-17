package edu.uniandes.privateRecsys.onlineRecommender.metadata;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.mahout.clustering.spectral.common.AffinityMatrixInputJob;
import org.apache.mahout.clustering.spectral.common.MatrixDiagonalizeJob;
import org.apache.mahout.clustering.spectral.common.VectorMatrixMultiplicationJob;
import org.apache.mahout.common.distance.CosineDistanceMeasure;
import org.apache.mahout.common.distance.EuclideanDistanceMeasure;
import org.apache.mahout.common.distance.ManhattanDistanceMeasure;
import org.apache.mahout.math.Matrix;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.decomposer.lanczos.LanczosState;
import org.apache.mahout.math.hadoop.DistributedRowMatrix;
import org.apache.mahout.math.hadoop.decomposer.DistributedLanczosSolver;
import org.apache.mahout.math.hadoop.decomposer.EigenVerificationJob;

public class MetadataSpectralConverter {
	private final static Logger LOG = Logger.getLogger(MetadataSpectralConverter.class
		      .getName()); 
	public static final double OVERSHOOTMULTIPLIER = 1.5;
	private static double  SIGMA=10;
	/**
	 * Converts a metadata file to a spectral representation, reimplements part of SpectralKmenasDriver from mahout
	 * @param localMetadataFile
	 * @param localOutputSequentialFile
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws ClassNotFoundException 
	 */
	public static void convertDataToSpectralRepresentation(String localMetadataFile, String localOutputSequentialFile, int clusters) throws IOException, ClassNotFoundException, InterruptedException{
		
	 
		
		Matrix mat=MetadataMapFileLoader.loadMetadataMap(localMetadataFile, true);
		int numItems=mat.numRows();
		LOG.info("Loaded metadata, numItems is "+numItems+" with "+mat.numCols()+" attributes");
		File affinityTempFile = File.createTempFile("tempfile", ".tmp"); 
		
		printAffinityFile(mat, affinityTempFile);
		LOG.info("Printed affinity file "+affinityTempFile.getAbsolutePath());
		
		Configuration conf= new Configuration();
		
		FileSystem fs = FileSystem.get(conf);
		
		Path inputPath= new Path("input","input");
		Path outputPath= new Path("output");
		Path outputTmp= new Path("outputTemp");
		
		Path affSeqFiles = new Path(outputPath, "seqfile");
		
		Path pathfileMetadata= new Path(affinityTempFile.getAbsolutePath()); 
		File outputSequenceFile= new File(localOutputSequentialFile);
		Path outputlocalFileSequentialFile= new Path(outputSequenceFile.getAbsolutePath());
	
		
		fs.copyFromLocalFile(pathfileMetadata, inputPath);
		
		AffinityMatrixInputJob.runJob(inputPath, affSeqFiles, numItems, numItems);
		
		// Construct the affinity matrix using the newly-created sequence files
		DistributedRowMatrix A = 
						new DistributedRowMatrix(affSeqFiles, new Path(outputTmp, "afftmp"), numItems, numItems); 
		
		Configuration depConf = new Configuration(conf);
		A.setConf(depConf);
		
		// Construct the diagonal matrix D (represented as a vector)
		Vector D = MatrixDiagonalizeJob.runJob(affSeqFiles, numItems);
		
		//Calculate the normalized Laplacian of the form: L = D^(-0.5)AD^(-0.5)
		DistributedRowMatrix L = VectorMatrixMultiplicationJob.runJob(affSeqFiles, D,
				new Path(outputPath, "laplacian"), outputTmp);
		L.setConf(depConf);
		
		
		int overshoot = Math.min((int) ((double) clusters * OVERSHOOTMULTIPLIER), numItems);
		DistributedLanczosSolver solver = new DistributedLanczosSolver();
		LanczosState state = new LanczosState(L, overshoot, solver.getInitialVector(L));
		Path lanczosSeqFiles = new Path(outputPath, "eigenvectors");
		
		solver.runJob(conf,
		              state,
		              overshoot,
		              true,
		              lanczosSeqFiles.toString());
		
		// perform a verification
		EigenVerificationJob verifier = new EigenVerificationJob();
		Path verifiedEigensPath = new Path(outputPath, "eigenverifier");
		verifier.runJob(conf, 
						lanczosSeqFiles, 
						L.getRowPath(), 
						verifiedEigensPath, 
						true, 
						1.0, 
						clusters);
		
		Path cleanedEigens = verifier.getCleanedEigensPath();
		DistributedRowMatrix W = new DistributedRowMatrix(
				cleanedEigens, new Path(cleanedEigens, "tmp"), clusters, numItems);
		W.setConf(depConf);
		DistributedRowMatrix Wtrans = W.transpose();
		Path data = Wtrans.getRowPath();
		
		fs.copyToLocalFile(data, outputlocalFileSequentialFile);
		
		
				
		
		
	}
	public static void printAffinityFile(Matrix metadataRepresentation, File temp) {
		PrintWriter pr= null;
		ManhattanDistanceMeasure m= new ManhattanDistanceMeasure();
		//CosineDistanceMeasure m= new CosineDistanceMeasure();
		try {
			pr=new PrintWriter(temp);
			for(int i=0;i<metadataRepresentation.rowSize();i++){
				pr.println(i+","+i+","+0);
				Vector row1=metadataRepresentation.viewRow(i);
				for (int j = i+1; j < metadataRepresentation.rowSize(); j++) {
					Vector row2=metadataRepresentation.viewRow(j);
					
					double distance=m.distance(row1, row2);//row1.getDistanceSquared(row2);
					double affinity= Math.exp(-distance/(2*Math.pow(SIGMA, 2)));
					pr.println(i+","+j+","+affinity);
					pr.println(j+","+i+","+affinity);
				
				}
				
				
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(pr!=null){
				try{
					pr.close();
				}catch(Exception e){}
			}			
		}
		
	}	
	
	public static void main(String[] args) {
		try {
			MetadataSpectralConverter.convertDataToSpectralRepresentation("data/ml-10M100K/metadata/mapFile.data", "data/ml-10M100K/metadata/spectral10",  10);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*try {
			Matrix meta=MetadataMapFileLoader.loadMetadataMap("data/plista/clickedItemsKeywords2.map",true);
			
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	
	

}
