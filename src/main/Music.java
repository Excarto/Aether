import java.io.*;
import java.util.*;

public class Music{
	
	private Sound[] songs;
	private int[] order;
	
	public Music(String folder){
		File[] files = new File(folder).listFiles();
		Arrays.sort(files);
		
		songs = new Sound[files.length];
		for (int x = 0; x < songs.length; x++){
			songs[x] = new Sound(files[x]);
			songs[x].load();
		}
		
		order = new int[songs.length];
	}
	
	public void randomize(int seed){
		Random random = new Random(seed);
		for (int x = 0; x < order.length; x++)
			order[x] = x;
		for (int x = 0; x < order.length; x++){
			int index = random.nextInt(order.length);
			int temp = order[index];
			order[index] = order[x];
			order[x] = temp;
		}
	}	
	
	public void play(){
		for (int x = 0; x < songs.length; x++){
			Sound song = null, next = null;
			for (int y = 0; y < songs.length; y++){
				if (order[y] == x)
					song = songs[y];
				if (order[y] == (x+1)%songs.length)
					next = songs[y];
			}
			song.setFollowSound(next);
		}
		
		for (int x = 0; x < songs.length; x++){
			if (order[x] == 0)
				songs[x].play();
		}
	}
	
	public void setVolume(double volume){
		for (int x = 0; x < songs.length; x++)
			songs[x].setVolume(volume);
	}
	
}
