package nl.unimaas.games.tron.util;

public class KnuthRandom {
	private final static long MULTIPLIER = 0x5DEECE66DL;
    private final static long APPEND = 0xBL;
    private final static long MASK = (1L << 48) - 1;
    private static long seed = System.nanoTime();
    
    public final static int nextInt() {  
        final long nextseed = (seed * MULTIPLIER + APPEND) & MASK;
        seed = nextseed;

        return (int) (nextseed&Integer.MAX_VALUE);
    }
    
    public final static int nextInt(int max) {
    	int ret = (int) (nextDouble() * max);
    	if (ret == max)
    		return ret - 1;
    	else
    		 return ret;
    }
    
    public final static double nextDouble() {  
    	final long nextseed = (seed * MULTIPLIER + APPEND) & MASK;
    	seed = nextseed;

        return (nextseed&Integer.MAX_VALUE)/(double)Integer.MAX_VALUE;
    }
    
    public final static void setSeed(long seed) {
    	KnuthRandom.seed = seed;
    }
}
