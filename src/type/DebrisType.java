import static java.lang.Math.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class DebrisType{
	
	public final Renderable renderable;
	public final int size;
	public final double visionSize;
	
	private BufferedImage origImage;
	
	public DebrisType(String type){
		File file = new File("data/debris/" + type);
		try{
			origImage = ImageIO.read(file);
		}catch(IOException e){
			Main.crash(file.getPath());
		}
		
		BufferedImage temp = new BufferedImage(
				origImage.getWidth(), origImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
		temp.getGraphics().drawImage(origImage, 0, 0, null);
		int opaquePixels = 0;
		Raster raster = temp.getAlphaRaster();
		for (int x = 0; x < raster.getWidth(); x++){
			for (int y = 0; y < raster.getHeight(); y++)
				if (raster.getSample(x, y, 0) > 2)
					opaquePixels++;
		}
		size = (int)round(opaquePixels);
		visionSize = pow(size*Main.debrisVisionSize, 0.2);
		
		renderable = new Renderable(1,
				(int)(Main.debrisRenderAnglesMultiplier*Main.renderAnglesMultiplier*
						max(origImage.getWidth(), origImage.getHeight())));
	}
	
	public void load(){
		renderable.load(new BufferedImage[]{origImage}, 1.0);
		origImage = null;
	}
}
