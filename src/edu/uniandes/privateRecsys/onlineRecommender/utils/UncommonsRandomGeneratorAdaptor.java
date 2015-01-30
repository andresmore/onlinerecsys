package edu.uniandes.privateRecsys.onlineRecommender.utils;

import org.apache.commons.math3.random.RandomGenerator;
import org.uncommons.maths.random.XORShiftRNG;

public class UncommonsRandomGeneratorAdaptor extends XORShiftRNG implements RandomGenerator {

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
