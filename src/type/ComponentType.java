import java.awt.image.*;
import java.io.*;
import javax.imageio.ImageIO;
import org.imgscalr.*;

public abstract class ComponentType extends BuyType{
	static final int DETAIL_WIDTH = 120;
	
	public final BufferedImage icon;
	public final double powerUse;
	public final double engageEnergy;
	
	private BufferedImage detailImage;
	
	public abstract String powerCategory();
	
	public double getAveragePowerUse(){
		return powerUse;
	}
	
	public ComponentType(String type, String folder){
		super(type, folder);
		
		powerUse = getDouble("power_use")/Main.TPS;
		engageEnergy = getDouble("energy_to_engage");
		
		File file = new File("data/" + folder + "/" + type + "/icon.png");
		BufferedImage temp = null;
		try{
			temp = ImageIO.read(file);
		}catch(IOException e){
			Main.crash(file.getPath());
		}
		icon = temp;
	}
	
	public void load(){
		
	}
	
	public BufferedImage getDetailImage(){
		if (detailImage == null)
			detailImage = Scalr.resize(sideImg, Scalr.Method.QUALITY, DETAIL_WIDTH, sideImg.getHeight()*DETAIL_WIDTH/sideImg.getWidth());
		return detailImage;
	}
}
