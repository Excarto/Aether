import static java.lang.Math.*;
import java.util.concurrent.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.ImageIO;
import org.imgscalr.*;

// Expolosions are stored as one image per frame, with each frame being constructed at load time
// based on some number of input component images

public class ExplosionType extends Type{
	
	static ArrayBlockingQueue<ImageCoords> toLoad = new ArrayBlockingQueue<ImageCoords>(20);
	static Thread loadThread;
	
	private final BufferedImage[] baseImage;
	private final VolatileImage[] img;
	
	private final int numComponents;
	private final double[][] imgOpacity;
	private final double[][] imgScale;
	private final double[] currentScale;
	private final int[] componentDuration;
	
	public final String type;
	public final double visionSize;
	public final int duration;
	
	public int preloadFrames;
	public double size;
	
	public ExplosionType(String type){
		super("explosions/" + type);
		this.type = type;
		
		int count = 0;
		while (getDouble("max_scale" + (count+1)) > 0)
			count++;
		numComponents = count;
		
		int duration = 0;
		componentDuration = new int[numComponents];
		for (int component = 0; component < numComponents; component++){
			componentDuration[component] = 1 + (int)(getDouble("duration" + (component+1))*Main.options.framesPerSec);
			duration = max(duration, componentDuration[component]);
		}
		this.duration = duration;
		
		visionSize = getDouble("vision_size");
		
		img = new VolatileImage[duration];
		baseImage = new BufferedImage[duration];
		
		imgScale = new double[numComponents][duration];
		imgOpacity = new double[numComponents][duration];
		currentScale = new double[duration];
		
		for (int component = 0; component < numComponents; component++){
			double timePow = getDouble("time_power" + (component+1));
			
			double growthTime = getDouble("growth_time" + (component+1));
			double growDuration = componentDuration[component]*growthTime;
			double shrinkDuration = (componentDuration[component]-1)*(1.0-growthTime);
			
			double maxScale = getDouble("max_scale" + (component+1));
			double start = getDouble("start_scale" + (component+1))/maxScale;
			double end = getDouble("end_scale" + (component+1))/maxScale;
			for (int time = 0; time < (int)growDuration; time++)
				imgScale[component][time] = maxScale*(start+(1.0-start)*(1-pow((growDuration-time)/growDuration, timePow)));
			if (imgScale[component].length > (int)growDuration)
				imgScale[component][(int)growDuration] = imgScale[component][(int)growDuration - 1];
			for (int time = (int)ceil(growDuration); time < componentDuration[component]; time++)
				imgScale[component][time] = maxScale*(1.0-(1.0-end)*pow((time-growDuration)/shrinkDuration, timePow));
			
			double max = getDouble("max_opacity" + (component+1));
			start = getDouble("start_opacity" + (component+1));
			end = getDouble("end_opacity" + (component+1));
			for (int time = 0; time < growDuration; time++)
				imgOpacity[component][time] = start+(max-start)*(1-pow((growDuration-time)/growDuration, timePow));
			if (imgOpacity[component].length > (int)growDuration)
				imgOpacity[component][(int)growDuration] = imgOpacity[component][(int)growDuration - 1];
			for (int time = (int)ceil(growDuration); time < componentDuration[component]; time++)
				imgOpacity[component][time] = max-(max-end)*pow((time-growDuration)/shrinkDuration, timePow);
		}
		
	}
	
	// The baseImage array is built out of some number of component images. For each frame, the
	// scale and opacity of each component is determined by values read in from text file
	public void load(){
		
		BufferedImage[] origImage = new BufferedImage[numComponents];
		for (int component = 0; component < numComponents; component++){
			File file = new File("data/explosions/" + type + "/image" + (component+1) + ".png");
			try{
				origImage[component] = ImageIO.read(file);
			}catch(IOException e){
				Main.crash(file.getPath());
			}
		}
		
		// Compute explosion size and initialize baseImage array
		size = 0;
		for (int frame = 0; frame < duration; frame++){
			int frameSize = 0;
			for (int component = 0; component < numComponents; component++){
				int imageSize = max(origImage[component].getWidth(), origImage[component].getHeight());
				frameSize = max(frameSize, (int)(imgScale[component][frame]*imageSize));
			}
			size = max(size, frameSize);
			
			if (frameSize > 1)
				baseImage[frame] = Main.getCompatibleImage(frameSize, frameSize, true);
		}
		preloadFrames = 1 + (int)(size/150);
		
		for (int component = 0; component < numComponents; component++){
			
			for (int frame = 0; frame < duration; frame++){
				double scale = imgScale[component][frame];
				
				if ((int)(origImage[component].getWidth()*scale) > 1 && (int)(origImage[component].getHeight()*scale) > 1){
					
					BufferedImage img = Scalr.resize(origImage[component], Main.scaleMethod,
							(int)(origImage[component].getWidth()*scale), (int)(origImage[component].getHeight()*scale));
					
					int width = img.getWidth(), height = img.getHeight();
					double timeFrac = min(1.0, max(0.0, 1.3*(frame/(double)componentDuration[component] - 0.2)));
					
					for (int x = 0; x < width; x++){
						for (int y = 0; y < height; y++){
							int color = img.getRGB(x, y);
							
							double alphaMult = 1.0;
							if (componentDuration[component] > 6){
								alphaMult = ((color >> 24) & 0xff)/256.0;
								alphaMult = timeFrac*alphaMult*alphaMult*alphaMult*alphaMult + 1.0 - timeFrac;
							}
							alphaMult *= imgOpacity[component][frame];
							
							int newColor = 0;
							for (int band = 0; band <= 3; band++){
								int val = (color >> (8*band)) & 0xFF;
								int newVal = val;
								if (img.isAlphaPremultiplied() || band == 3)
									newVal = min(255, (int)round(val*alphaMult));
								newColor |= newVal << (8*band);
							}
							
				            img.setRGB(x, y, newColor);
						}
					}
					
					Graphics g = baseImage[frame].getGraphics();
					g.drawImage(img, baseImage[frame].getWidth()/2 - img.getWidth()/2,
							baseImage[frame].getHeight()/2 - img.getHeight()/2, null);
					g.dispose();
					
					img.flush();
				}
			} // End loop over frames
		} // End loop over components
		
		for (int component = 0; component < numComponents; component++)
			origImage[component].flush();
	}
	
	public int getNumFrames(){
		return img.length;
	}
	
	public Image getImg(double zoom, int frame){
		frame = min(frame, img.length-1);
		
		if (zoom >= 1.0)
			return baseImage[frame];
		
		VolatileImage existingImg = img[frame];
		if (zoom == currentScale[frame] && existingImg != null && !existingImg.contentsLost())
			return existingImg;
		
		loadImg(zoom, frame);
		
		return img[frame];
	}
	
	private synchronized void loadImg(double zoom, int frame){
		VolatileImage oldImg = img[frame];
		if (zoom == currentScale[frame] && oldImg != null && !oldImg.contentsLost())
			return;
		
		if (oldImg != null){
			oldImg.flush();
			img[frame] = null;
		}
		
		if (baseImage[frame] != null){
			int width = (int)(baseImage[frame].getWidth()*zoom);
			int height = (int)(baseImage[frame].getHeight()*zoom);
			if (height > 1 && width > 1){
				//img[frame] = Main.convertVolatile(Scalr.resize(baseImage[frame], Scalr.Method.SPEED, width, height));
				VolatileImage newImg = Main.getCompatibleVolatileImage(width, height, true);
				Graphics2D g = newImg.createGraphics();
				g.setComposite(AlphaComposite.Src);
				g.scale(zoom, zoom);
				g.drawImage(baseImage[frame], 0, 0, null);
				g.dispose();
				img[frame] = newImg;
			}
		}
		
		currentScale[frame] = zoom;
	}
	
	public void queueLoadImg(int frame){
		if (toLoad.remainingCapacity() > 1)
			toLoad.add(new ImageCoords(frame));
	}
	
	public static void startLoadThread(){
		loadThread = new Thread(){
			public void run(){
				while (true){
					try{
						ImageCoords coords = toLoad.take();
						Window window = Main.getCurrentWindow();
						if (window instanceof GameWindow)
							coords.type.loadImg(((GameWindow)window).getRenderZoom(), coords.frame);
					}catch (InterruptedException e){
						e.printStackTrace();
					}
				}
			}
		};
		loadThread.start();
	}
	
	private class ImageCoords{
		//final double zoom;
		final int frame;
		final ExplosionType type;
		public ImageCoords(int frame){
			//this.zoom = zoom;
			this.frame = frame;
			type = ExplosionType.this;
		}
	}
	
}
