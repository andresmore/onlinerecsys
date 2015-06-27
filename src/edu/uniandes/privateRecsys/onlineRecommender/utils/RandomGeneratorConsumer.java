package edu.uniandes.privateRecsys.onlineRecommender.utils;

import java.lang.Thread.State;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import org.apache.commons.math3.random.RandomGenerator;
import org.uncommons.maths.random.XORShiftRNG;

import edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters.DifferentialPrivacyOnlineRecommenderTesterWithThreshold;
/**
 * Consumes the random numbers created in a thread by the RandomGeneratorThread
 * @author AndresM
 *
 */
@SuppressWarnings("serial")
public class RandomGeneratorConsumer {

	private final static Logger LOG = Logger.getLogger(RandomGeneratorConsumer.class
		      .getName());

	private LinkedBlockingQueue<Double> randNumbers= new LinkedBlockingQueue<Double>();

	private RandomGeneratorThread thread;

	private UncommonsRandomGeneratorAdaptor adaptor;

	private boolean started=false;
	
	
	public RandomGeneratorConsumer(UncommonsRandomGeneratorAdaptor adaptor){
		this.adaptor=adaptor;
		this.thread=  new RandomGeneratorThread(randNumbers,adaptor);
		
	}
	


	public synchronized double nextGaussian() {
		if(!started){
			LOG.info("Starting random generation thread");
			this.thread.start();
			this.started=true;
		}
		
		
			try {
				while(randNumbers.isEmpty()){
				 Thread.sleep(1000);
				 LOG.info("Woke up");
				}
		//		return randNumbers.poll();
			} catch (InterruptedException e) {
				LOG.severe("Didn't wait for random generation");
			}
		
		
			return randNumbers.poll();
		
		
	}



	public UncommonsRandomGeneratorAdaptor getAdaptor() {
		// TODO Auto-generated method stub
		return this.adaptor;
	}
	
	

}
