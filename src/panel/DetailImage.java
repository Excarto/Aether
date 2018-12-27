import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

public class DetailImage extends JComponent{
	private BufferedImage img;
	
	public DetailImage(){
		this.setPreferredSize(new Dimension(120, 60));
	}
	
	public void select(ComponentType type){
		if (type == null){
			img = null;
		}else
			img = type.getDetailImage();//Scalr.resize(type.sideImg, Scalr.Method.BALANCED, this.getPreferredSize().width, this.getPreferredSize().height);
		this.repaint();
	}
	
	public void paint(Graphics g){
		if (img != null){
			g.drawImage(img, 0, 0, null);
		}else{
			
		}
	}
}