import static java.lang.Math.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

// This is not actually a child of Type, since the only data read in is the image

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
		
		// Count how many pixels it has to compute the size of the debris chunk
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
		visionSize = pow(size*Main.config.debrisVisionSize, 0.2);
		
		int numRenders = (int)(Main.options.debrisRenderAnglesMultiplier*Main.options.renderAnglesMultiplier*
						max(origImage.getWidth(), origImage.getHeight()));
		renderable = new Renderable(1, numRenders, false);
	}
	
	public void load(){
		renderable.load(new BufferedImage[]{origImage}, 1.0);
		origImage = null;
	}
}
