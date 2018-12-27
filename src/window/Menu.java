import static java.lang.Math.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.imageio.*;

public class Menu extends Window{
	
	private final static int OPTION_SPACING = 48;
	private final static int OPTION_START = 580;
	private final static int TEXT_SIZE = 28;
	private final static Font OPTION_FONT = new Font("Courier", Font.BOLD, TEXT_SIZE);
	private final static Font VERSION_FONT = new Font("Arial", Font.PLAIN, 12);
	private final static Color OPTION_COLOR = new Color(100, 100, 100);
	
	private List<Option> options;
	
	private static BufferedImage background;
	private static Sound music;
	
	private boolean running;
	
	public Menu(){
		super(false);
		
		options = new ArrayList<Option>();
		
		if (music == null){
			music = new Sound(new File("data/menu_music.wav"));
			music.setFollowSound(music);
			music.load();
		}
		
		if (background == null){
			try{
				background = Main.convert(ImageIO.read(new File("data/main_menu.png")));
			}catch(IOException e){
				background = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			}
		}
		
		this.addMouseListener(new MouseAdapter(){
			public void mousePressed (MouseEvent e){
				for (int x = 0; x < options.size(); x++){
					Option option = options.get(x);
					if (abs(e.getX()-Main.resX/2) < option.name.length()*TEXT_SIZE*0.32 &&
							abs(e.getY()+TEXT_SIZE/3-(OPTION_START+x*OPTION_SPACING)) < TEXT_SIZE/2)
						option.act();
				}
			}
		});
	}
	
	public void addOption(Option option){
		options.add(option);
	}
	
	public void paint(Graphics g){
		((Graphics2D)g).setRenderingHints(Main.menuHints);
		g.drawImage(background, -(background.getWidth()-Main.resX)/2, 0, null);
		
		g.setColor(Color.GRAY);
		g.setFont(VERSION_FONT);
		g.drawString("v"+(Main.VERSION/100.0)+"."+Main.SUBVERSION, Main.resX-50, Main.RES_Y-15);
		
		g.setFont(OPTION_FONT);
		if (running){
			for (int x = 0; x < options.size(); x++){
				Option option = options.get(x);
				
				int posX = Main.resX/2-(int)(option.name.length()*TEXT_SIZE*0.3);
				int posY = OPTION_START+x*OPTION_SPACING;
				g.setColor(Color.BLACK);
				for (int offsetX = -1; offsetX <= 1; offsetX++){
					for (int offsetY = -1; offsetY <= 1; offsetY++)
						g.drawString(option.name, posX+offsetX, posY+offsetY);
				}
				g.setColor(option.highlighted ? Color.LIGHT_GRAY : OPTION_COLOR);
				g.drawString(option.name, posX, posY);
				
				g.setColor(new Color(30, 180, 30));
				g.drawLine(0, OPTION_START+x*OPTION_SPACING-TEXT_SIZE/3,
						option.size, OPTION_START+x*OPTION_SPACING-TEXT_SIZE/3);
				g.drawLine(Main.resX, OPTION_START+x*OPTION_SPACING-TEXT_SIZE/3,
						Main.resX-option.size, OPTION_START+x*OPTION_SPACING-TEXT_SIZE/3);
				
				g.drawLine(option.size, OPTION_START+x*OPTION_SPACING-TEXT_SIZE*5/6,
						option.size, OPTION_START+x*OPTION_SPACING+TEXT_SIZE/6);
				g.drawLine(Main.resX-option.size, OPTION_START+x*OPTION_SPACING-TEXT_SIZE*5/6,
						Main.resX-option.size, OPTION_START+x*OPTION_SPACING+TEXT_SIZE/6);
				
				g.drawLine(option.size, OPTION_START+x*OPTION_SPACING-TEXT_SIZE*5/6,
						option.size+10, OPTION_START+x*OPTION_SPACING-TEXT_SIZE*5/6);
				g.drawLine(option.size, OPTION_START+x*OPTION_SPACING+TEXT_SIZE/6,
						option.size+10, OPTION_START+x*OPTION_SPACING+TEXT_SIZE/6);
				g.drawLine(Main.resX-option.size, OPTION_START+x*OPTION_SPACING-TEXT_SIZE*5/6,
						Main.resX-option.size-10, OPTION_START+x*OPTION_SPACING-TEXT_SIZE*5/6);
				g.drawLine(Main.resX-option.size, OPTION_START+x*OPTION_SPACING+TEXT_SIZE/6,
						Main.resX-option.size-10, OPTION_START+x*OPTION_SPACING+TEXT_SIZE/6);
			}
		}
	}
	
	public void suspend(){
		running = false;
		if (!(Main.getCurrentWindow() instanceof Menu))
			Sound.stopAll();
	}
	
	public void resume(){
		start();
		Toolkit.getDefaultToolkit().sync();
	}
	
	public void start(){
		new Thread("MenuAnimateThread"){
			public void run(){
				animate();
		}}.start();
		music.setVolume(Main.musicVolume);
		music.play();
		this.setPreferredSize(new Dimension(Main.resX, Main.RES_Y));
		repaint();
	}
	
	public void animate(){
		running = true;
		while(running){
			try{
				Thread.sleep(1000/Main.framesPerSec);
			}catch (InterruptedException e){}
			for (int x = 0; x < options.size(); x++){
				Option option = options.get(x);
				Point mousePosition = Main.getMousePosition();
				option.highlighted = mousePosition != null &&
						abs(mousePosition.x-Main.resX/2) < option.name.length()*TEXT_SIZE*0.32 &&
						abs(mousePosition.y+TEXT_SIZE/3-(OPTION_START+x*OPTION_SPACING)) < TEXT_SIZE/2;
				
				if (option.highlighted){
					option.size = (int)min(option.size+800/Main.framesPerSec, Main.resX/2-option.name.length()*TEXT_SIZE*0.288-10);
				}else
					option.size = max(80, option.size-400/Main.framesPerSec);
			}
			
			repaint();
			//repaint(0, OPTION_START-TEXT_SIZE, Main.resX, options.size()*OPTION_SPACING);
		}
	}
	
	public abstract class Option{
		private String name;
		private int size;
		private boolean highlighted;
		public Option(String name){
			this.name = name;
			this.size = 80;
			this.highlighted = false;
		}
		public abstract void act();
	}
}
