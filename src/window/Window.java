import static java.lang.Math.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

public abstract class Window extends JPanel{
	public static final FlowLayout DEFAULT_LAYOUT = new FlowLayout(FlowLayout.CENTER, 1, 1);
	public static final FlowLayout NO_BORDER = new FlowLayout(FlowLayout.CENTER, 0, 0);
	//public static final Color BACKGROUND_COLOR = new Color(90, 90, 90);//new Color(135, 135, 135);
	public static final Color DEFAULT_PANEL_BACKGROUND = new Color(227, 227, 227);
	public static final Color COMPONENT_BACKGROUND = new Color(201, 203, 205);
	public static final Color PANEL_LIGHT = new Color(220, 220, 220);
	public static final Color PANEL_VERY_LIGHT = new Color(240, 240, 240);
	public static Color SELECTED_COLOR = new Color(70, 210, 100);
	public static final Border SELECTED_BORDER = BorderFactory.createEtchedBorder(SELECTED_COLOR, Color.GRAY);
	public static final Border UNSELECTED_BORDER = BorderFactory.createEtchedBorder();
	public static final Font TITLE_FONT = new Font("Courier", Font.BOLD, 24);
	public static final InputVerifier INT_VERIFIER = new InputVerifier(){
		public boolean verify(JComponent input){
			try{
				Integer.parseInt(((JTextField)input).getText());
			}catch (NumberFormatException e){
				return false;
			}
			return true;
	}};
	
	public Window(boolean narrow){
		if (narrow){
			this.setPreferredSize(new Dimension(Main.RES_X_NARROW, Main.RES_Y));
		}else
			this.setPreferredSize(new Dimension(Main.resX, Main.RES_Y));
		this.setLayout(DEFAULT_LAYOUT);
		this.setOpaque(false);
		this.setDoubleBuffered(false);
		//this.setBackground(BACKGROUND_COLOR);
	}
	
	public void returnValue(Object type){}
	public void suspend(){}
	public void exit(){
		suspend();
	}
	public void resume(){}
	
	public JComponent getRootComponent(){
		return this;
	}
	
	public static JComponent createSpacer(int width, int height){
		JPanel spacer = new JPanel();
		spacer.setPreferredSize(new Dimension(width, height));
		spacer.setOpaque(false);
		//spacer.setBackground(color);
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
			
			//g.setColor(BACKGROUND_COLOR);
			//g.fillRect(0, 0, this.getWidth(), this.getHeight());
			
			g.setFont(TITLE_FONT);
			int posX = getWidth()/2-g.getFontMetrics().stringWidth(text)/2;
			int posY = getHeight()/2+g.getFontMetrics().getHeight()/5;
			g.setColor(Color.BLACK);
			for (int x = -1; x <= 1; x++){
				for (int y = -1; y <= 1; y++)
					g.drawString(text, posX+x, posY+y);
			}
			g.setColor(PANEL_VERY_LIGHT);
			g.drawString(text, posX, posY);
		}
		
		public void setText(String text){
			this.text = text;
			repaint();
		}
	}
}
