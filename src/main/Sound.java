import static java.lang.Math.*;
import java.util.concurrent.*;
import java.io.*;
import javax.sound.sampled.*;

// Class to load sound data from file, play/pause on demand via multiple sound channels
// If all channels are taken, then choose one to stop based on priority

public class Sound{
	private final static int BUFFER_SIZE = 8192;
	private final static double VOLUME_RANGE = 35.0;
	private final static int MAX_SEPARATION_TIME = 220;
	
	private static AudioChannel[] channels;
	private static double masterVolume;
	private static boolean paused;
	
	public final File soundFile;
	
	private AudioFormat format;
	private DataLine.Info info;
	private byte[] data;
	private Sound followSound;
	private double volume;
	private int length;
	private int runCount;
	private long lastPlayTime;
	private int separationTime;
	
	public Sound(File soundFile){
		this.soundFile = soundFile.canRead() ? soundFile : null;
		volume = 0.5;
		lastPlayTime = java.lang.System.currentTimeMillis();
	}
	
	public void load(){
		if (soundFile != null){
			try{
				data = new byte[(int)soundFile.length()];
				FileInputStream fileStream = new FileInputStream(soundFile);
				fileStream.read(data);
				fileStream.close();
				
				AudioInputStream stream = getStream();
				format = stream.getFormat();
				info = new DataLine.Info(SourceDataLine.class, format);
				length = (int)(stream.getFrameLength()*1000/format.getFrameRate());
				separationTime = min(MAX_SEPARATION_TIME, length/3);
			}catch(Exception e){
				Main.crash(soundFile.getPath());
			}
		}
	}
	
	public void setVolume(double volume){
		this.volume = volume;
	}
	
	public void play(){
		play(1.0);
	}
	public void play(double volume){
		if (data != null && (followSound == null || runCount == 0)){
			// Delay if multiple sounds on top of each other so it's not just really loud
			long playTime = java.lang.System.currentTimeMillis();
			int difference = (int)(playTime - lastPlayTime);
			int delay = 0;
			if (difference < separationTime)
				delay = (int)((0.25 + 0.75*random())*(separationTime - difference));
			lastPlayTime = playTime;
			playSound(this, delay, volume);
		}
	}
	
	public void stop(){
		for (int x = 0; x < channels.length; x++){
			if (channels[x].sound == this)
				channels[x].interrupt();
		}
	}
	
	public void setFollowSound(Sound followSound){
		this.followSound = followSound;
	}
	
	private AudioInputStream getStream(){
		try{
			return AudioSystem.getAudioInputStream(new ByteArrayInputStream(data));
		}catch (UnsupportedAudioFileException|IOException e){
			e.printStackTrace();
		}
		return null;
	}
	
	private static synchronized void playSound(Sound sound, int delay, double volume){
		if (masterVolume > 0 && sound.volume > 0){
			AudioChannel lowestPriority = channels[0];
			for (int x = 1; x < channels.length; x++){
				if (channels[x].priority < lowestPriority.priority)
					lowestPriority = channels[x];
			}
			lowestPriority.play(sound, delay, volume);
		}
	}
	
	public static void stopAll(){
		for (int x = 0; x < channels.length; x++)
			channels[x].interrupt();
	}
	
	public static void initialize(int numChannels, double masterVolume){
		deinitialize();
		Sound.masterVolume = masterVolume;
		channels = new AudioChannel[numChannels];
		for (int x = 0; x < channels.length; x++)
			channels[x] = new AudioChannel();
	}
	
	public static void deinitialize(){
		if (channels != null){
			for (int x = 0; x < channels.length; x++)
				channels[x].deinitialize();
		}
	}
	
	public static void setPause(boolean paused){
		Sound.paused = paused;
	}
	
	private static class AudioChannel extends Thread{
		long priority;
		boolean interrupted, running;
		Sound sound;
		byte[] buffer;
		CountDownLatch latch;
		int delay;
		double playVolume;
		
		AudioChannel(){
			super("Sound");
			running = true;
			buffer = new byte[BUFFER_SIZE];
			latch = new CountDownLatch(1);
			this.start();
		}
		
		void play(Sound sound, int delay, double volume){
			this.sound = sound;
			this.delay = delay;
			this.playVolume = volume;
			sound.runCount++;
			
			if (sound.followSound != null){
				priority = Long.MAX_VALUE;
			}else
				priority = java.lang.System.currentTimeMillis() + (long)(sound.length*3/4);
			
			interrupted = true;
			latch.countDown();
		}
		
		public void run(){
			while (running){
				try{
					latch.await();
				}catch (InterruptedException e){
					e.printStackTrace();
				}
				if (!running)
					break;
				latch = new CountDownLatch(1);
				interrupted = false;
				
				AudioInputStream stream = null;
				SourceDataLine line = null;
				try{
					stream = sound.getStream();
					stream.mark(Integer.MAX_VALUE);
					
					line = (SourceDataLine)AudioSystem.getLine(sound.info);
					line.open(sound.format);
					line.start();
					
					float gain = (float)((sound.volume*masterVolume*playVolume - 1.0)*VOLUME_RANGE);
					try{
						((FloatControl)line.getControl(FloatControl.Type.MASTER_GAIN)).setValue(gain);
					}catch (Exception e){}
					
					int bytesRead = 0;
					while (running && !interrupted){
						if (delay > 0){
							try{
								Thread.sleep(delay);
							}catch (InterruptedException ex){}
							delay = 0;
						}
						
						if (paused){
							try{
								Thread.sleep(500);
							}catch (InterruptedException ex){}
						}else{
							if ((bytesRead = stream.read(buffer, 0, BUFFER_SIZE)) > 0){
								line.write(buffer, 0, bytesRead);
							}else{
								if (sound.followSound == sound){
									stream.reset();
								}else
									break;
							}
						}
					}
					if (running && !interrupted){
						line.drain();
						if (sound.followSound != null)
							play(sound.followSound, 0, playVolume);
					}else
						line.flush();
				}catch (IOException | LineUnavailableException e){
					e.printStackTrace();
				}finally{
					try{
						if (stream != null)
							stream.close();
					}catch (IOException e){}
					if (line != null)
						line.close();
				}
				
				sound.runCount = max(0, sound.runCount-1);
			}
		}
		
		public void interrupt(){
			interrupted = true;
			priority = 0;
		}
		
		public void deinitialize(){
			running = false;
			latch.countDown();
		}
	}
	
}
