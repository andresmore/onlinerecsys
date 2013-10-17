package edu.uniandes.privateRecsys.onlineRecommender.vo;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.mahout.cf.taste.impl.common.FullRunningAverage;

public class IRPrecisionError implements ErrorReport {

	private ConcurrentLinkedQueue <Double> precisions= new ConcurrentLinkedQueue <Double>();
	private ConcurrentLinkedQueue <Double> precisionsAt5= new ConcurrentLinkedQueue <Double>();
	private ConcurrentLinkedQueue <Double> precisionsAt10= new ConcurrentLinkedQueue <Double>();
	private ConcurrentLinkedQueue <Double> aucs= new ConcurrentLinkedQueue <Double>();
	public IRPrecisionError(ConcurrentLinkedQueue <Double> precisions,
			ConcurrentLinkedQueue <Double> precisionsAt5,
			ConcurrentLinkedQueue <Double> precisionsAt10, ConcurrentLinkedQueue <Double> aucs) {
		super();
		this.precisions = precisions;
		this.precisionsAt5 = precisionsAt5;
		this.precisionsAt10 = precisionsAt10;
		this.aucs = aucs;
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

}
