package edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation;

import java.util.HashMap;
import java.util.LinkedList;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.mahout.math.DenseMatrix;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Sorting;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.function.DoubleComparator;
import org.apache.mahout.math.function.DoubleDoubleFunction;
import org.apache.mahout.math.function.Functions;

import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.OrdinalRatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.ratingScale.RatingScale;
import edu.uniandes.privateRecsys.onlineRecommender.utils.PrivateRandomUtils;

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
		Vector response=toProject.clone();
		DoubleComparator comp = new DoubleComparator() {

			@Override
			public int compare(double o1, double o2) {
				// TODO Auto-generated method stub
				return Double.compare(o1, o2)*-1;
			}
		};
		
		double[] toArray = copyToArray(response);
		Sorting.quickSort(toArray, 0, response.size() , comp);
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
		
		
		response.assign(Functions.MINUS,tMax);
		//toProject.assign(VectorProjector.REPLACE_NEGATIVES,0.001);
		response.assign(Functions.MAX, 0);
		double[] array2=copyToArray(response);
			return response;
		
	}

	private static double[] copyToArray(Vector vector) {
		double[] toArray = new double[vector.size()];
		for (int i = 0; i < toArray.length; i++) {
			toArray[i] = vector.getQuick(i);
		}

		return toArray;
	}
	
	public static void main(String[] args) {
		double[] test={0.053126975515139674,0.017844574472162367,0.3610358097220433, 0.14267489776184894,  0.3780798180862419};
		Vector itemVector= new DenseVector(test);
		itemVector=VectorProjector.projectVectorIntoSimplex(itemVector);
		System.out.println("item vector is");
		System.out.println(itemVector);
		HashMap<String, Vector> trainedProfiles= new HashMap<>();
		String[] ratingScaleV={"1","2","3","4","5"};
		String rating="5";
		HashMap<String, Vector> userProfile= new HashMap<String, Vector>();
		for (int i = 0; i < ratingScaleV.length; i++) {
			Vector vec=PrivateRandomUtils.normalRandom(0, 0.01, 5);
			userProfile.put(ratingScaleV[i],vec);
		}
		
		userProfile=VectorProjector.projectUserProfileIntoSimplex(userProfile, ratingScaleV, 5);
		LinkedList<BetaDistribution> dist= new LinkedList<>();
		Vector emptyHyperParams=null;
		double gamma=0.1;
		 HashMap<String,String> translations=new HashMap<String,String>();
		 translations.put(new String("0"), new String("1"));
		 translations.put(new String("0.5"), new String("1"));
		 translations.put(new String("1.5"), new String("2"));
		 translations.put(new String("2.5"), new String("3"));
		 translations.put(new String("3.5"), new String("4"));
		 translations.put(new String("4.5"), new String("5"));

		
		
		
		for (int i = 0; i < ratingScaleV.length; i++) {	
			Vector privateVector=userProfile.get(ratingScaleV[i]);
			System.out.println("original "+ratingScaleV[i]+" "+privateVector);
			int prob=ratingScaleV[i].equals(rating)?1:0;
			
			double dotProb=privateVector.dot(itemVector);
			System.out.println("dot product:" +dotProb);
			double loss=prob-dotProb;
			
			double multiplicator=gamma*(loss);
			Vector privateVectorMult=itemVector.times(multiplicator);
			Vector result=privateVector.plus(privateVectorMult);
			System.out.println("end vector:" +result);
			double endDotProb=result.dot(itemVector);
			System.out.println("enddot product:" +endDotProb);
			double stepLoss=prob-endDotProb;
			
			if(Math.abs(stepLoss)>Math.abs(loss)){
				//	System.err.println("Model increased loss");
			}
			trainedProfiles.put(ratingScaleV[i], result);
			
			
		}
		
		trainedProfiles=VectorProjector.projectUserProfileIntoSimplex(trainedProfiles,ratingScaleV, itemVector.size());
		for (int i = 0; i < ratingScaleV.length; i++) {
			System.out.println("final "+ratingScaleV[i]+" "+trainedProfiles.get(ratingScaleV[i]));
		}
		
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
