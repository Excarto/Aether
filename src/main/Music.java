import java.io.*;
import java.util.*;

// Stores array of songs in randomized order

public class Music{
	
	private Sound[] songs;
	private Sound firstSong;
	private String customDir;
	private int[] order;
	
	public Music(String dir, String customDir){
		this.customDir = customDir;
		
		File[] files = new File(dir).listFiles(new FileFilter(){
			public boolean accept(File pathname){
				return pathname.getName().endsWith(".wav");
			}
		});
		Arrays.sort(files);
		
		songs = new Sound[files.length];
		for (int x = 0; x < songs.length; x++){
			songs[x] = new Sound(files[x]);
			songs[x].load();
		}
		
		order = new int[songs.length];
	}
	
	public void randomize(int seed, String first){
		Random random = new Random(seed);
		for (int x = 0; x < order.length; x++)
			order[x] = x;
		for (int x = 0; x < order.length; x++){
			int index = random.nextInt(order.length);
			int temp = order[index];
			order[index] = order[x];
			order[x] = temp;
		}
		
		int firstIndex = 0;
		for (int x = 0; x < order.length; x++){
			if (order[x] == 0)
				firstIndex = x;
		}
		
		firstSong = songs[firstIndex];
		if (first != null){
			int newFirstIndex = -1;
			for (int x = 0; x < order.length; x++){
				if (shortName(songs[x].soundFile).equals(first + ".wav"))
					newFirstIndex = x;
			}
			if (newFirstIndex != -1){
				order[firstIndex] = order[newFirstIndex];
				order[newFirstIndex] = 0;
				firstIndex = newFirstIndex;
				firstSong = songs[firstIndex];
			}else{
				
				File[] files = new File(customDir).listFiles(new FileFilter(){
					public boolean accept(File pathname){
						return pathname.getName().endsWith(".wav");
					}
				});
				Arrays.sort(files);
				
				for (int x = 0; x < files.length; x++){
					if (shortName(files[x]).equals(first + ".wav")){
						firstSong = new Sound(files[x]);
						firstSong.load();
					}
				}
			}
		}
		
		firstSong.setFollowSound(songs[firstIndex]);
		
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
		
	}	
	
	public void play(){
		firstSong.play();
	}
	
	public void setVolume(double volume){
		for (int x = 0; x < songs.length; x++)
			songs[x].setVolume(volume);
	}
	
	private static String shortName(File file){
		return file.getName().replaceAll("\\s+","").replace("_","").replace("-","").toLowerCase();
	}
	
}
