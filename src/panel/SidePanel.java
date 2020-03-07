import javax.swing.*;
import javax.imageio.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;

public abstract class SidePanel extends JPanel{
	
	public static BufferedImage background;
	public static BufferedImage spacer;
	
	public SidePanel(){
		this.setLayout(Window.DEFAULT_LAYOUT);
		this.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH, Main.resY-2));
	}
	
	public void refresh(){}
	
	/*public void paintComponent(Graphics g){
		g.drawImage(background, 0, 0, null);
		//super.paint(g);
	}*/
	
	public static void load(){
		try{
			background = Main.convert(ImageIO.read(new File("data/panel_background.png")));
		}catch(IOException e){
			background = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		}
		
		try{
			spacer = Main.convert(ImageIO.read(new File("data/panel_spacer.png")));
		}catch(IOException e){
			spacer = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		}
	}
	
}
