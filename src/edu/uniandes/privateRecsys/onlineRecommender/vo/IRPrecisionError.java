package edu.uniandes.privateRecsys.onlineRecommender.vo;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.mahout.cf.taste.impl.common.FullRunningAverage;

public class IRPrecisionError implements ErrorReport {

	private ConcurrentLinkedQueue <Double> precisions= new ConcurrentLinkedQueue <Double>();
	private ConcurrentLinkedQueue <Double> precisionsAt5= new ConcurrentLinkedQueue <Double>();
	private ConcurrentLinkedQueue <Double> precisionsAt10= new ConcurrentLinkedQueue <Double>();
	private ConcurrentLinkedQueue <Double> aucs= new ConcurrentLinkedQueue <Double>();
	private ConcurrentLinkedQueue<Integer> posFirstHit;
	public IRPrecisionError(ConcurrentLinkedQueue <Double> precisions,
			ConcurrentLinkedQueue <Double> precisionsAt5,
			ConcurrentLinkedQueue <Double> precisionsAt10, ConcurrentLinkedQueue <Double> aucs, ConcurrentLinkedQueue<Integer> posFirstHit) {
		super();
		this.precisions = precisions;
		this.precisionsAt5 = precisionsAt5;
		this.precisionsAt10 = precisionsAt10;
		this.aucs = aucs;
		this.posFirstHit=posFirstHit;
	}
	@Override
	public double getErrorTest() {
		
		return getAveragePrecision();
	}
	public double getAveragePrecision() {
		FullRunningAverage avg= new FullRunningAverage();
		for (Double precision : precisions) {
			avg.addDatum(precision);
		}
		return avg.getAverage();
	}
	public double getAveragePrecisionsAt5() {
		FullRunningAverage avg= new FullRunningAverage();
		for (Double precision : precisionsAt5) {
			avg.addDatum(precision);
		}
		return avg.getAverage();
	} 
	public double getAveragePrecisionsAt10() {
		FullRunningAverage avg= new FullRunningAverage();
		for (Double precision : precisionsAt10) {
			avg.addDatum(precision);
		}
		return avg.getAverage();
	}
	public double getAverageAUC() {
		FullRunningAverage avg= new FullRunningAverage();
		for (Double precision : aucs) {
			avg.addDatum(precision);
		}
		return avg.getAverage();
	}
	
	public double getHitRate() {
		FullRunningAverage avg= new FullRunningAverage();
		for(Integer first: this.posFirstHit) {
			if(first!=0) {
				avg.addDatum(1);
			}
			else {
				avg.addDatum(0);
			}
		}
		return avg.getAverage();
	}
	public double getAverageReciprocalHitRate() {
		FullRunningAverage avg= new FullRunningAverage();
		for(Integer first: this.posFirstHit) {
			
			if(first!=0) {
				avg.addDatum(1.0/first);
			}
			else {
				avg.addDatum(0);
			}
			
		}
		return avg.getAverage();
	}
	
	
	
	
	@Override
	public String toString(){
	   return "HitRate="+getHitRate()+" ARHR="+getAverageReciprocalHitRate()+" Average_precision="+getAveragePrecision()+" P@5="+getAveragePrecisionsAt5()+" P@10="+getAveragePrecisionsAt10()+" AUC="+getAverageAUC();	
	}

}
