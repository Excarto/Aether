import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import java.lang.System;
import java.util.concurrent.*;

// Window that appears if someone goes through the menu into the game quickly enough that the background
// load thread is not yet done. This class also contains the implementation of the load thread itself

public final class LoadWindow extends Window{
	private static final int UNITS_LOAD = 20, WEAPONS_LOAD = 2, DEBRIS_LOAD = 2, ARENAS_LOAD = 2, EXPLOSIONS_LOAD = 10, SYSTEMS_LOAD = 1;
	private static final int BAR_WIDTH = 400, BAR_HEIGHT = 12, BAR_YPOS = Main.resY*29/32;
	
	private static BufferedImage loadScreen;
	private static int loaded;
	private static int numToLoad;
	private static LoadWindow window;
	private static Thread loadThread;
	private static CountDownLatch loadLatch;
	
	public LoadWindow(){
		super(Size.FULL);
		try{
			loadScreen = ImageIO.read(new File("data/load_screen.png"));
		}catch(IOException e){
			loadScreen = new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
		}
		loadThread.setPriority(6);
		window = this;
	}
	
	public void paint(Graphics g){
		if (!isLoaded()){
			g.drawImage(loadScreen, -(loadScreen.getWidth()-Main.resX)/2, 0, null);
			g.setColor(Color.DARK_GRAY);
			g.fillRect((Main.resX-BAR_WIDTH)/2, BAR_YPOS, BAR_WIDTH, BAR_HEIGHT);
			g.setColor(Color.BLACK);
			g.drawRect((Main.resX-BAR_WIDTH)/2, BAR_YPOS, BAR_WIDTH, BAR_HEIGHT);
			g.fillRect((Main.resX-BAR_WIDTH)/2+2, BAR_YPOS+2,
					(int)((double)(BAR_WIDTH-4)*loaded/numToLoad), BAR_HEIGHT-3);
			g.setColor(Color.LIGHT_GRAY);
			g.drawString("Loading", Main.resX/2-16, BAR_YPOS+30);
		}else{
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, Main.resX, Main.resY);
		}
	}
	
	// Launch a new thread that does the necessary loading in the background, before triggering the loadLatch
	public static void start(){
		numToLoad = Main.shipTypes.length*UNITS_LOAD+Main.craftTypes.length*UNITS_LOAD+
				Main.weaponTypes.length*WEAPONS_LOAD+Main.debrisTypes.length*DEBRIS_LOAD+
				Main.explosionTypes.length*EXPLOSIONS_LOAD+Main.arenas.length*ARENAS_LOAD+
				Main.systemTypes.length*SYSTEMS_LOAD+1;
		loadLatch = new CountDownLatch(1);
		
		loadThread = new Thread("LoadThread"){
			public void run(){
				try{
					Game.load();
					GameWindow.load();
					Thruster.load();
					Arena.background.load();
					for (ShipType type : Main.shipTypes){
						type.load();
						loadIncrement(UNITS_LOAD);
					}
					for (CraftType type : Main.craftTypes){
						type.load();
						loadIncrement(UNITS_LOAD);
					}
					for (WeaponType type : Main.weaponTypes){
						type.load();
						loadIncrement(WEAPONS_LOAD);
					}
					for (SystemType type : Main.systemTypes){
						type.load();
						loadIncrement(SYSTEMS_LOAD);
					}
					for (DebrisType type : Main.debrisTypes){
						type.load();
						loadIncrement(DEBRIS_LOAD);
					}
					for (ExplosionType type : Main.explosionTypes){
						type.load();
						loadIncrement(EXPLOSIONS_LOAD);
					}
					for (Arena arena : Main.arenas){
						arena.load();
						loadIncrement(ARENAS_LOAD);
					}
					
					System.gc();
					loadIncrement(1);
					
					loadLatch.countDown();
				}catch(Exception e){
					if (!Main.isExiting){
						e.printStackTrace();
						Main.crash("Failed to load game data");
					}
				}
			}
		};
		loadThread.setPriority(1);
		loadThread.start();
	}
	
	// Adjust the load bar progress
	private static void loadIncrement(int amount){
		loaded += amount;
		if (window != null)
			window.repaint((Main.resX-BAR_WIDTH)/2, BAR_YPOS, BAR_WIDTH, BAR_HEIGHT);
	}
	
	public static boolean isLoaded(){
		return loadLatch.getCount() == 0;
	}
	
	// Wait for the load thread to finish
	public static void awaitLoad(){
		try{
			loadLatch.await();
		}catch (InterruptedException e){
			e.printStackTrace();
		}
	}
}
