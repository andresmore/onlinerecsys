package edu.uniandes.privateRecsys.onlineRecommender.utils;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;
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
	


	public double nextGaussian() {
		synchronized (randNumbers) {
			if (!started) {
				LOG.info("Starting random generation thread");
				this.thread.start();
				this.started = true;
			}
		}

		Double rand = null;

		try {
			if (randNumbers.isEmpty()) {
				//Sleep longer and non-blocking to reduce time waiting for buffer to refill
				Thread.sleep(500);
				
			}
		} catch (InterruptedException e) {
			
		}
		
		//Blocking 
		synchronized (randNumbers) {
			while ((rand = randNumbers.poll()) == null) {
				try {
					Thread.sleep(10);
					LOG.info("Woke up" + randNumbers.size());
				} catch (InterruptedException e) {
					LOG.severe("Didn't wait for random generation");
				}
				
			}
		}

		return rand;

	}
			
		
	



	public UncommonsRandomGeneratorAdaptor getAdaptor() {
		// TODO Auto-generated method stub
		return this.adaptor;
	}
	
	

}
