import static java.lang.Math.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.imageio.*;

public abstract class Window extends JPanel{
	public enum Size{
		FULL, NORMAL, SMALL;
	};
	
	public static final Composite DEFAULT_COMPOSITE = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
	public static final FlowLayout DEFAULT_LAYOUT = new FlowLayout(FlowLayout.CENTER, 1, 1);
	public static final FlowLayout NO_BORDER = new FlowLayout(FlowLayout.CENTER, 0, 0);
	public static final Color DEFAULT_PANEL_BACKGROUND = new Color(227, 227, 227);
	public static final Color COMPONENT_BACKGROUND = new Color(201, 203, 205);
	public static final Color PANEL_LIGHT = new Color(220, 220, 220);
	public static final Color PANEL_VERY_LIGHT = new Color(240, 240, 240);
	public static final Color SELECTED_COLOR = new Color(70, 210, 100);
	public static final Border SELECTED_BORDER = BorderFactory.createEtchedBorder(SELECTED_COLOR, Color.GRAY);
	public static final Border UNSELECTED_BORDER = BorderFactory.createEtchedBorder();
	public static final Font TITLE_FONT = new Font("Courier", Font.BOLD, 23);
	
	public static final InputVerifier INT_VERIFIER = new InputVerifier(){
		public boolean verify(JComponent input){
			try{
				Integer.parseInt(((JTextField)input).getText());
			}catch (NumberFormatException e){
				return false;
			}
			return true;
	}};
	
	private static BufferedImage backgroundImage = null;
	private static String backgroundImageFile = null;
	protected static BufferedImage dividerImg;
	protected static Sound clickSound;
	
	public final Size size;
	
	public Window(Size size){
		this.size = size;
		if (size == Size.FULL){
			this.setPreferredSize(new Dimension(Main.resX, Main.resY));
		}else
			this.setPreferredSize(new Dimension(Main.RES_X_NARROW, Main.resY));
			
		this.setLayout(DEFAULT_LAYOUT);
		this.setOpaque(false);
		this.setDoubleBuffered(false);
		if (size == Size.NORMAL){
			this.add(createSpacer(Main.resX-100, Main.resY/2 - 355));
		}else if (size == Size.SMALL)
			this.add(createSpacer(Main.resX-100, Main.resY/2 - 180));
	}
	
	public void returnValue(Object value){}
	public void suspend(){}
	public void exit(){
		suspend();
	}
	public void resume(){}
	
	public JComponent getRootComponent(){
		return this;
	}
	
	protected String getBackgroundFile(){
		if (size == Size.SMALL){
			return "data/menu_background_small.png";
		}else if (size == Size.NORMAL){
			return "data/menu_background_big.png";
		}else
			return null;
	}
	
	public final BufferedImage getBackgroundImage(){
		String newFile = getBackgroundFile();
		if (Objects.equals(backgroundImageFile, newFile)){
			return backgroundImage;
		}else{
			backgroundImageFile = newFile;
			
			if (backgroundImage != null)
				backgroundImage.flush();
			backgroundImage = null;
			if (newFile != null){
				try{
					backgroundImage = Main.convert(ImageIO.read(new File(newFile)));
				}catch(IOException e){}
			}
		}
		return backgroundImage;
	}
	
	public static void load(){
		try{
			dividerImg = Main.convert(ImageIO.read(new File("data/divider.png")));
		}catch (IOException ex){
			dividerImg = Main.getCompatibleImage(1, 1, false);
		}
		
		clickSound = new Sound(new File("data/click.wav"));
		clickSound.load();
	}
	
	public static JComponent createSpacer(int width, int height){
		JPanel spacer = new JPanel();
		spacer.setPreferredSize(new Dimension(width, height));
		spacer.setOpaque(false);
		return spacer;
	}
	
	public static int getTurnsPerFrame(int framesPerSec, double speed){
		return (int)max(1, round(Main.TPS*speed/framesPerSec));
	}
	public static int getFramesPerSec(int turnsPerFrame, double speed){
		return (int)round(Main.TPS*speed/turnsPerFrame);
	}
	
	protected class Title extends JComponent{
		String text;
		
		public Title(String text, int width, int height){
			this.setPreferredSize(new Dimension(width, height));
			this.setOpaque(false);
			this.text = text;
		}
		
		public void paint(Graphics graphics){
			Graphics2D g = (Graphics2D)graphics;
			g.setRenderingHints(Main.menuHints);
			g.setFont(TITLE_FONT);
			Utility.drawOutlinedText(g, text,
					getWidth()/2, getHeight()/2+TITLE_FONT.getSize()/4, PANEL_VERY_LIGHT, Color.BLACK);
		}
		
		public void setText(String text){
			this.text = text;
			repaint();
		}
	}
}
