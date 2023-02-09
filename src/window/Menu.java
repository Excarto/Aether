import static java.lang.Math.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.imageio.*;

// Menu displaying the background image and animated options at game launch.
// Each submenu with different options is implemented as another instance of a Menu object

public class Menu extends Window{
	
	final static int OPTION_SPACING = 48;
	final static int TEXT_SIZE = 28;
	final static Font OPTION_FONT = new Font("Courier", Font.BOLD, TEXT_SIZE);
	final static Color OPTION_COLOR = new Color(100, 100, 100);
	final static int ANIMATION_SPEED = 1500;
	final static int MIN_LINE_SIZE = 80;
	
	private static BufferedImage title;
	private static Sound music;
	private static Font versionFont;
	
	private List<Option> options;
	private PeriodicTimer timer;
	protected boolean drawTitle;
	
	public Menu(){
		super(Size.FULL);
		
		drawTitle = true;
		options = new ArrayList<Option>();
		
		this.addMouseListener(new MouseAdapter(){
			public void mousePressed (MouseEvent e){
				if (!Main.isStarting){
					for (int x = 0; x < options.size(); x++){
						Option option = options.get(x);
						if (option.highlighted){
							option.playSound();
							option.act();
						}
					}
				}
			}
		});
		
		timer = new PeriodicTimer(1000/Main.options.menuFramesPerSec, 5, 50){
			public void runTimerTask(){
				animateFrame();
				repaint((1000/Main.options.menuFramesPerSec)/2);
		}};
	}
	
	public void addOption(Option newOption){
		options.add(newOption);
		newOption.lineSize = MIN_LINE_SIZE;
		newOption.highlighted = false;
		int bottomSpace = 100 + ((Main.resY - Main.RES_Y_SHORT)/2)/2;
		int posY = Main.resY - bottomSpace - OPTION_SPACING*(options.size()+1)/2;
		if (options.size() > 5)
			posY -= OPTION_SPACING*(options.size()-5)/2;
		for (Option option : options){
			option.posY = posY;
			posY += OPTION_SPACING;
		}
	}
	
	public void clearOptions(){
		options.clear();
	}
	
	public int getNumOptions(){
		return options.size();
	}
	
	public void paint(Graphics graphics){
		Graphics2D g = (Graphics2D)graphics;
		g.setRenderingHints(Main.menuHints);
		
		if (drawTitle){
			g.drawImage(title,
					-(title.getWidth() - Main.resX)/2,
					20 + Main.resY/20,
					null);
			
			g.setColor(Color.GRAY);
			g.setFont(versionFont);
			g.drawString("v"+(Main.VERSION/100.0)+"."+Main.SUBVERSION, Main.resX-55, Main.resY-15);
		}
		
		
		
		if (!Main.isStarting){
			for (int x = 0; x < options.size(); x++){
				Option option = options.get(x);
				g.setFont(option.font);
				int posY = option.posY;
				
				Utility.drawOutlinedText(g, option.text,
						Main.resX/2, posY,
						option.highlighted ? Color.LIGHT_GRAY : OPTION_COLOR, Color.BLACK);
				
				g.setColor(new Color(30, 180, 30));
				g.drawLine(0, posY-TEXT_SIZE/3,
						option.lineSize, posY-TEXT_SIZE/3);
				g.drawLine(Main.resX, posY-TEXT_SIZE/3,
						Main.resX-option.lineSize, posY-TEXT_SIZE/3);
				
				g.drawLine(option.lineSize, posY-TEXT_SIZE*5/6,
						option.lineSize, posY+TEXT_SIZE/6);
				g.drawLine(Main.resX-option.lineSize, posY-TEXT_SIZE*5/6,
						Main.resX-option.lineSize, posY+TEXT_SIZE/6);
				
				g.drawLine(option.lineSize, posY-TEXT_SIZE*5/6,
						option.lineSize+10, posY-TEXT_SIZE*5/6);
				g.drawLine(option.lineSize, posY+TEXT_SIZE/6,
						option.lineSize+10, posY+TEXT_SIZE/6);
				g.drawLine(Main.resX-option.lineSize, posY-TEXT_SIZE*5/6,
						Main.resX-option.lineSize-10, posY-TEXT_SIZE*5/6);
				g.drawLine(Main.resX-option.lineSize, posY+TEXT_SIZE/6,
						Main.resX-option.lineSize-10, posY+TEXT_SIZE/6);
			}
		}
	}
	
	public void suspend(){
		timer.stop(false);
		if (!(Main.getCurrentWindow() instanceof Menu))
			music.stop();
	}
	
	public void resume(){
		startAnimation();
		Toolkit.getDefaultToolkit().sync();
	}
	
	public void start(){
		startAnimation();
	}
	
	private void startAnimation(){
		timer.start();
		music.setVolume(Main.options.musicVolume);
		music.play();
	}
	
	public void animateFrame(){
		for (int x = 0; x < options.size(); x++){
			Option option = options.get(x);
			Point mousePosition = Main.getMousePosition();
			option.highlighted = mousePosition != null && option.isSelected(mousePosition);
			if (option.highlighted){
				option.lineSize = (int)min(option.lineSize + ANIMATION_SPEED/Main.options.menuFramesPerSec,
						Main.resX/2-option.textWidth/2-8);
			}else
				option.lineSize = max(MIN_LINE_SIZE, option.lineSize-(ANIMATION_SPEED/2)/Main.options.menuFramesPerSec);
		}
	}
	
	protected String getBackgroundFile(){
		return "data/main_menu_background.png";
	}
	
	public static void load(){
		music = new Sound(new File("data/menu_music.wav"));
		music.setFollowSound(music);
		music.load();
		
		try{
			title = Main.convert(ImageIO.read(new File("data/title.png")));
		}catch(IOException e){
			title = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		}
		
		versionFont = Main.getDefaultFont(12);
	}
	
	public abstract class Option{
		private String text;
		private int lineSize;
		private int posY;
		private boolean highlighted;
		private int textWidth;
		private Font font;
		
		public Option(String text){
			this.text = text;
			this.highlighted = false;
			
			int maxWidth = Main.resX - 2*MIN_LINE_SIZE - 40;
			font = OPTION_FONT;
			while ((textWidth = getFontMetrics(font).stringWidth(text)) > maxWidth)
				font = new Font(font.getFamily(), font.getStyle(), font.getSize()-1);
		}
		
		public boolean isSelected(Point pos){
			return abs(pos.x - Main.resX/2) < textWidth/2 &&
					abs(pos.y+TEXT_SIZE/3 - posY) < TEXT_SIZE/2;
		}
		
		public void playSound(){
			clickSound.play();
		}
		
		public abstract void act();
	}
}
