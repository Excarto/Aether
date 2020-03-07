import java.util.concurrent.*;

public abstract class PeriodicTimer{
	static final int DEFAULT_MAX_SLEEP_MILIS = 8;
	static final long MAX_YIELD_NANOS = 300*1000;
	
	private final long nanos;
	private final int priority;
	private final long maxSleepNanos;
	
	private boolean[] running;
	private CountDownLatch completeLatch;
	
	protected abstract void runTimerTask();
	
	public PeriodicTimer(int milis){
		this(milis, Thread.NORM_PRIORITY, DEFAULT_MAX_SLEEP_MILIS);
	}
	public PeriodicTimer(int milis, int priority, int maxSleepMilis){
		this.nanos = milis*1000*1000;
		this.priority = priority;
		this.maxSleepNanos = 1000l*1000l*maxSleepMilis;
		running = new boolean[]{false};
	}
	
	public final synchronized void start(){
		if (running[0])
			return;
		running = new boolean[]{true};
		final boolean[] threadRunning = running;
		Thread thread = new Thread("AccurateTimerThread"){
			public void run(){
				runTimer(threadRunning);
			}
		};
		thread.setPriority(priority);
		thread.start();
	}
	
	public final synchronized void stop(boolean wait){
		if (!running[0])
			return;
		if (wait)
			completeLatch = new CountDownLatch(1);
		running[0] = false;
		if (wait){
			try{
				completeLatch.await();
			}catch (InterruptedException ex){}
		}
	}
	
	public final synchronized boolean isRunning(){
		return running[0];
	}
	
	private void runTimer(boolean[] threadRunning){
		while (threadRunning[0]){
			long now = java.lang.System.nanoTime();
	        final long end = now + nanos;
	        
	        runTimerTask();
	        
	        long timeLeft = end - java.lang.System.nanoTime();
	        while (timeLeft > maxSleepNanos){
	        	try{
	        		Thread.sleep(1);
	        	}catch (InterruptedException ex){}
	        	timeLeft = end - java.lang.System.nanoTime();
	        }
	        while (timeLeft > MAX_YIELD_NANOS){
	        	Thread.onSpinWait();
	        	Thread.yield();
	        	timeLeft = end - java.lang.System.nanoTime();
	        }
	        while (timeLeft > 0){
	        	Thread.onSpinWait();
	        	timeLeft = end - java.lang.System.nanoTime();
	        }
		}
		
		if (completeLatch != null)
			completeLatch.countDown();
    }
	
}
