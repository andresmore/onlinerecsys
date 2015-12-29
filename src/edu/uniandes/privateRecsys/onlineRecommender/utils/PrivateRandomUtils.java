package edu.uniandes.privateRecsys.onlineRecommender.utils;

import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Matrix;
import org.apache.mahout.math.Vector;

import edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation.VectorProjector;

public class PrivateRandomUtils {

	private static int SEED=1;
	
	private static final RandomGeneratorConsumer genRand=new RandomGeneratorConsumer (new UncommonsRandomGeneratorAdaptor(SEED))	;
	private static final UncommonsRandomGeneratorAdaptor nonParallelGEnerator=new UncommonsRandomGeneratorAdaptor(SEED);
	/**
	 * Randomizes a Matrix based with values taken from a normal distribution between [0,1) using Rand.nextGaussian()
	 * @param matrix
	 * @param projectRows 
	 */
	public static void randomizeMatrix(Matrix matrix, boolean projectRows) {
		
		
		//Random rand=RandomUtils.getRand000om();
		for (int row = 0; row < matrix.rowSize(); row++) {
			for (int column = 0; column < matrix.columnSize(); column++) {
				matrix.setQuick(row, column, genRand.nextGaussian());
			}
			if(projectRows){
				Vector projected=VectorProjector.projectVectorIntoSimplex(matrix.viewRow(row));
			
				matrix.assignRow(row, projected);
			}
		}
		
	}
	
	 public static Vector normalRandom(double mu, double sigma, int size){
		Vector retVector= new DenseVector(size);
		
		
		
		for (int i = 0; i < retVector.size(); i++) {
			double value=mu+sigma*genRand.nextGaussian();
			retVector.setQuick(i, value);
		}
		
		return retVector;
	}
	 
	public static Vector laplaceRandom(double mu, double sigma, int size){
		Vector retVector= new DenseVector(size);
		
		
		
		for (int i = 0; i < retVector.size(); i++) {
			double value;
			do{
				value=-0.5+genRand.nextGaussian();
			}while(value<=-0.5||value>0.5);
			//u = rand(m, n)-0.5;
			double valueU=value;
			//b = sigma / sqrt(2);
			double valueB=sigma/Math.sqrt(2);
			//y = mu - b * sign(u).* log(1- 2* abs(u));
			if(1-2*Math.abs(valueU)<0)
				System.out.println("warning");
			value=mu-valueB*Math.signum(valueU)*Math.log(1-2*Math.abs(valueU));
			retVector.setQuick(i, value);
		}
		
		return retVector;
		
		
	}
	
	public static UncommonsRandomGeneratorAdaptor getCurrentRandomGenerator(){
		return nonParallelGEnerator;
	}

	public static int getSeed() {

		return SEED;
	}

}
