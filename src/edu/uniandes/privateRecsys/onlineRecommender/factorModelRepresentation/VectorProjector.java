package edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation;

import java.util.HashMap;

import org.apache.mahout.math.DenseMatrix;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Sorting;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.function.DoubleComparator;
import org.apache.mahout.math.function.DoubleDoubleFunction;
import org.apache.mahout.math.function.Functions;

import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;

public class VectorProjector {

	private static final DoubleDoubleFunction MINUS_NO_ZERO = new DoubleDoubleFunction() {
		
		@Override
		public double apply(double vectorArgument, double otherArgument) {
			
			return vectorArgument!=0?vectorArgument-otherArgument:0;
		}
	} ;
	private static final DoubleDoubleFunction REPLACE_NEGATIVES = new DoubleDoubleFunction() {
		
		@Override
		public double apply(double vectorArgument, double otherArgument) {
			// TODO Auto-generated method stub
			return vectorArgument<0?otherArgument:vectorArgument;
		}
	};

	/**
	 * Implementation of projection explained in http://arxiv.org/abs/1101.6081
	 * by Xiaojing Ye
	 * 
	 * @param toProject
	 * @return
	 */
	public static Vector projectVectorIntoSimplex(Vector toProject) {

		DoubleComparator comp = new DoubleComparator() {

			@Override
			public int compare(double o1, double o2) {
				// TODO Auto-generated method stub
				return Double.compare(o1, o2)*-1;
			}
		};
		
		double[] toArray = copyToArray(toProject);
		Sorting.quickSort(toArray, 0, toProject.size() , comp);
		double tempSum=0;
		double tMax=0;
		boolean step5=false;
		for (int i = 0; i < toArray.length-1&&!step5; i++) {
			tempSum=tempSum+toArray[i];
			tMax=(tempSum-1)/(i+1);
			if (tMax>=toArray[i+1]) {
				step5=true;
			}
		}
		//step4:
		if(!step5)
			tMax=(tempSum+toArray[toArray.length-1]-1)/toArray.length;
		
		
		toProject.assign(Functions.MINUS,tMax);
		//toProject.assign(VectorProjector.REPLACE_NEGATIVES,0.001);
		toProject.assign(Functions.MAX, 0);
		double[] array2=copyToArray(toProject);
			return toProject;
		
	}

	private static double[] copyToArray(Vector vector) {
		double[] toArray = new double[vector.size()];
		for (int i = 0; i < toArray.length; i++) {
			toArray[i] = vector.getQuick(i);
		}

		return toArray;
	}
	
	public static void main(String[] args) {
		double[] test={0.053126975515139674,0.017844574472162367,0.3610358097220433, 0.14267489776184894, 0,0,0.327, 0.3780798180862419};
		Vector testVector= new DenseVector(test);
		Vector result=VectorProjector.projectVectorIntoSimplex(testVector);
		System.out.println(result);
		
		
	}

	public static HashMap<String, Vector> projectUserProfileIntoSimplex(
			HashMap<String, Vector> profile, String[] scale, int dimensions) {
		
		
		for (int i = 0; i <dimensions; i++) {
			Vector toProject= new DenseVector(scale.length);
			for (int j = 0; j < scale.length; j++) {
				Vector other=profile.get(scale[j]);
				toProject.set(j, other.get(i));
			}
			toProject=VectorProjector.projectVectorIntoSimplex(toProject);
			for (int j = 0; j < scale.length; j++) {
				Vector other=profile.get(scale[j]);
				other.set(i, toProject.get(j));
			}
			
		}
		
		return profile;
	}
	
	/**
	 * Projects user profile into simplex
	 * @param userFactors
	 * @param ratingScale
	 * @param fDimensions
	 */
	public static void projectUserProfileMatricesIntoSimplex(
			DenseMatrix[] userFactors, RatingScale ratingScale,
			int fDimensions) {
		int numUsers=userFactors[0].numRows();
		String[] scale=ratingScale.getScale();
		for (int i = 0; i < numUsers; i++) {
			
			HashMap<String, Vector> profile= new HashMap<>();
			for(int j=0;j<userFactors.length;j++){
				DenseMatrix mat=userFactors[j];
				profile.put(scale[j],mat.viewRow(i));
				
				
			}
			profile=VectorProjector.projectUserProfileIntoSimplex(profile, scale, fDimensions);
			for(int j=0;j<userFactors.length;j++){
				DenseMatrix mat=userFactors[j];
				mat.assignRow(i, profile.get(scale[j]));
				
				
			}
			
			
		}
		
	}

}
