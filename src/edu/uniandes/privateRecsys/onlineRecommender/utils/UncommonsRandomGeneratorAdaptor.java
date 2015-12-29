package edu.uniandes.privateRecsys.onlineRecommender.utils;

import org.apache.commons.math3.random.RandomGenerator;
import org.uncommons.maths.random.MersenneTwisterRNG;
/**
 * An adaptor for producing/consuming random numbers from MersenneTwisterRNG and adapting to the commons random generator interface
 * @author AndresM
 *
 */
@SuppressWarnings("serial")
public class UncommonsRandomGeneratorAdaptor extends MersenneTwisterRNG implements RandomGenerator {

	
	
	public UncommonsRandomGeneratorAdaptor(int seed){
		super();
		super.setSeed(seed);
		
		
		
	}
	
	@Override
	public void setSeed(int seed) {
		super.setSeed(seed);
		
	}
	/**
	 * Have no idea what I'm doing, taken from {@code  org.apache.commons.math3.random.AbstractRandomGenerator}
	 */
	@Override
	public void setSeed(int[] seed) {
		 // the following number is the largest prime that fits in 32 bits (it is 2^32 - 5)
        final long prime = 4294967291l;

        long combined = 0l;
        for (int s : seed) {
            combined = combined * prime + s;
        }
        setSeed(combined);
		
	}


}
