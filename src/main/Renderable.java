import static java.lang.Math.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.util.*;
import java.util.concurrent.*;

//import javax.imageio.stream.*;
import org.imgscalr.*;

// Stores caches of transformed images for high-performance rendering

public class Renderable{
	public static final int MIN_CACHE_MEMORY = 300;
	private static final int MAX_NUM_SCALES = 30;
	private static final int MAX_QUEUE_SIZE = 20;
	private static final long START_CACHE_SIZE = 1000*1000*MIN_CACHE_MEMORY +
				(Runtime.getRuntime().maxMemory()-1000*1000*Main.MIN_MEMORY)*3/4;
	static final Queue<ImageNode> rotatedCache = new PriorityBlockingQueue<ImageNode>();  // Least recenty used removal of cached uscaled, rotated images
	static long maxCacheSize = START_CACHE_SIZE;
	static long cacheSize;
	static int currentAge;
	static PriorityBlockingQueue<ImageCoords> toLoad = new PriorityBlockingQueue<ImageCoords>(MAX_QUEUE_SIZE+1);
	static ArrayBlockingQueue<VolatileImage> toFlush = new ArrayBlockingQueue<VolatileImage>(30);
	static Thread loadThread;
	
	double[] scaledZoom;
	int numScaledZoom;
	VolatileImage[][] scaledImg; // Second level scaled and rotated images
	boolean[][] scaledQuality; // Whether or not 2nd-level cache image is temporary low-quality image
	ImageNode[] unscaled; // First cache level consists of unscaled images
	
	// Variables set at initialization
	public final int numRenders;
	int minSize;
	double angleIncrement;
	boolean queueQuality;
	public int size;
	boolean combine;
	int numOrigRenders;
	double minZoom;
	
	public Renderable(int minSize, int numRenders){
		this(minSize, numRenders, true);
	}
	
	public Renderable(int minSize, int numRenders, boolean queueQuality){
		this.numRenders = numRenders;
		this.minSize = minSize;
		this.queueQuality = queueQuality;
		angleIncrement = 360.0/numRenders;
	}
	
	// Return image at given angle and zoom level, either by retrieving from cache or generating it if not available
	public Image getImage(double zoom, double angle, boolean useTemp, int preloadDirection){
		if (zoom < minZoom)
			return null;
		
		int angleIndex = getAngleIndex(angle, numRenders);
		updateUnscaled(angleIndex); // Update cache priority
		
		if (zoom >= 1.0){
			// Unscaled
			if (unscaled[angleIndex] != null){
				queueSpeculativeQualityImage(zoom, angle, preloadDirection, -1);
				return unscaled[angleIndex].img;
			}
			if (useTemp){
				Image approx = makeApproxImage(zoom, angle);
				if (queueQuality)
					queueQualityImage(zoom, angle);
				return approx;
			}
			return makeQualityImage(zoom, angle);
		}else{
			// Scaled
			int zoomIndex = getZoomIndex(zoom);
			VolatileImage scaled = scaledImg[angleIndex][zoomIndex];
			if (scaled != null && !scaled.contentsLost()){
				queueSpeculativeQualityImage(zoom, angle, preloadDirection, zoomIndex);
				return scaled;
			}
			if (useTemp){
				VolatileImage approx = makeApproxImage(zoom, angle);
				scaledImg[angleIndex][zoomIndex] = approx;
				scaledQuality[angleIndex][zoomIndex] = false;
				if (queueQuality)
					queueQualityImage(zoom, angle);
				return approx;
			}
			VolatileImage qualityImg = (VolatileImage)makeQualityImage(zoom, angle);
			scaledImg[angleIndex][zoomIndex] = qualityImg;
			scaledQuality[angleIndex][zoomIndex] = true;
			return qualityImg;
		}
	}
	
	// Fast approximate image used temporarily if useTemp==true
	private VolatileImage makeApproxImage(double zoom, double angle){
		int angleIndex = getAngleIndex(angle, numRenders);
		
		ImageNode unscaledNode = unscaled[angleIndex];
		
		BufferedImage untransformedImg;
		int width, height;
		double rotation = 0;
		if (unscaledNode == null){
			int unrotatedAngleIndex = getAngleIndex(angle, numOrigRenders)*numRenders/numOrigRenders;
			untransformedImg = unscaled[unrotatedAngleIndex].img;
			rotation = angleIncrement*(angleIndex - unrotatedAngleIndex);
			//width = transformedWidth(untransformedImg, rotation, zoom);
			//height = transformedHeight(untransformedImg, rotation, zoom);
		}else{
			untransformedImg = unscaledNode.img;
			//width = (int)(0.5 + zoom*unscaledImg.getWidth());
			//height = (int)(0.5 + zoom*unscaledImg.getHeight());
		}
		int size = max(untransformedImg.getWidth(), untransformedImg.getHeight());
		width = (int)(0.5 + zoom*size);
		height = width;
		zoom = width/(double)size;
		//width = (int)(0.5 + zoom*untransformedImg.getWidth());
		//height = (int)(0.5 + zoom*untransformedImg.getHeight());
		
		VolatileImage img = Main.getCompatibleVolatileImage(width, height, true);
		Graphics2D g = img.createGraphics();
		g.setRenderingHints(Main.fastHints);
		g.setComposite(AlphaComposite.Clear);
		g.fillRect(0, 0, width, height);
		g.setComposite(AlphaComposite.Src);
		if (zoom < 1.0)
			g.scale(zoom, zoom);
		if (rotation != 0)
			g.rotate(toRadians(rotation), size/2, size/2);
		g.drawImage(untransformedImg, (size-untransformedImg.getWidth())/2, (size-untransformedImg.getHeight())/2, null);
		g.dispose();
		
		return img;
	}
	
	// High-quality slow image
	private Image makeQualityImage(double zoom, double angle){
		int angleIndex = getAngleIndex(angle, numRenders);
		
		//if (unrotatedAngleIndex1 != angleIndex){
		if (unscaled[angleIndex] == null){
			double exactOrigIndex = angleIndex*(double)numOrigRenders/numRenders;
			int origImgIndex1 = (int)round(exactOrigIndex);
			int origImgIndex2 = origImgIndex1 + (origImgIndex1 < exactOrigIndex ? 1 : -1);
			int unrotatedAngleIndex1 = origImgIndex1*numRenders/numOrigRenders;
			int unrotatedAngleIndex2 = origImgIndex2*numRenders/numOrigRenders;
			double rotation1 = (angleIndex-unrotatedAngleIndex1)*angleIncrement;
			double rotation2 = (angleIndex-unrotatedAngleIndex2)*angleIncrement;
			
			BufferedImage unrotated1 = unscaled[unrotatedAngleIndex1%unscaled.length].img;
			BufferedImage unrotated2 = unscaled[unrotatedAngleIndex2%unscaled.length].img;
			
			//int width = max(transformedWidth(unrotated1, rotation1, 1.0), transformedWidth(unrotated2, rotation2, 1.0));
			//int height = max(transformedHeight(unrotated1, rotation1, 1.0), transformedHeight(unrotated2, rotation2, 1.0));
			int width = max(unrotated1.getWidth(), unrotated2.getWidth());
			int height = max(unrotated1.getHeight(), unrotated2.getHeight());
			if (!combine){
				width = max(width, height);
				height = width;
			}
			//int size = max(unrotated1.getWidth(null), unrotated1.getHeight(null));
			
			BufferedImage unscaledImg = Main.getCompatibleImage(width, height, true);
			Graphics2D g = unscaledImg.createGraphics();
			AffineTransform originalTransform = g.getTransform();
			
			g.setComposite(AlphaComposite.Src);
			g.setRenderingHints(Main.inGameHints);
			g.rotate(toRadians(rotation1), width/2, height/2);
			g.drawImage(unrotated1, (width-unrotated1.getWidth())/2, (height-unrotated1.getHeight())/2, null);
			g.setTransform(originalTransform);
			
			if (combine){
				g.setComposite(AlphaComposite.SrcOver.derive(abs(angleIndex-unrotatedAngleIndex1)*(float)numOrigRenders/numRenders));
				g.rotate(toRadians(rotation2), width/2, height/2);
				g.drawImage(unrotated2, (width-unrotated2.getWidth())/2, (height-unrotated2.getHeight())/2, null);
			}
			
			g.dispose();
			
			addUnscaled(unscaledImg, angleIndex);
		}
		
		BufferedImage unscaledImg = unscaled[angleIndex].img;
		if (zoom >= 1.0)
			return unscaledImg;
		int width = transformedWidth(unscaledImg, 0.0, zoom);
		int height = transformedHeight(unscaledImg, 0.0, zoom);
		return Main.convertVolatile(Scalr.resize(unscaledImg, Main.scaleMethod, width, height));
	}
	
	// Make booleam map from image
	public boolean[][] getContactMap(double angle, double scale){
		Image image = this.getImage(scale, angle, false, 0);
		BufferedImage snapshot = null;
		Raster raster;
		if (image instanceof BufferedImage){
			raster = ((BufferedImage)image).getAlphaRaster();
		}else{
			snapshot = ((VolatileImage)image).getSnapshot();
			raster = snapshot.getAlphaRaster();
		}
		boolean[][] contactMap = new boolean[raster.getWidth()][raster.getHeight()];
		for (int x = 0; x < contactMap.length; x++){
			for (int y = 0; y < contactMap[x].length; y++)
				contactMap[x][y] = raster.getSample(x, y, 0) > 3;
		}
		if (snapshot != null)
			snapshot.flush();
		return contactMap;
	}
	
	// Convert from real zoom value to index
	private int getZoomIndex(double zoom){
		if (zoom >= 1.0)
			return -1;
		for (int i = 0; i < scaledZoom.length; i++){
			if (scaledZoom[i] == zoom)
				return i;
		}
		return setZoomIndex(zoom);
	}
	private synchronized int setZoomIndex(double zoom){
		for (int i = 0; i < scaledZoom.length; i++){
			if (scaledZoom[i] == zoom)
				return i;
		}
		int index = numScaledZoom%scaledZoom.length;
		numScaledZoom++;
		if (scaledZoom[index] != 0){
			for (int i = 0; i < numRenders; i++){
				scaledImg[i][index] = null;
				scaledQuality[i][index] = false;
			}
		}
		scaledZoom[index] = zoom;
		return index;
	}
	
	public double getRenderAngle(double angle){
		return getAngleIndex(angle, numRenders)*angleIncrement;
	}
	
	public static int getAngleIndex(double angle, int numAngles){
		double angleIncrement = 360.0/numAngles;
		int index = (int)round(angle/angleIncrement);
		if (index < 0){
			index += numAngles;
		}else if (index >= numAngles)
			index -= numAngles;
		return index;
	}
	
	private static int transformedWidth(BufferedImage img, double angle, double scale){
		angle = toRadians(angle);
		return (int)(0.5 + scale*(abs(cos(angle))*img.getWidth() + abs(sin(angle))*img.getHeight()));
		
	}
	private static int transformedHeight(BufferedImage img, double angle, double scale){
		angle = toRadians(angle);
		return (int)(0.5 + scale*(abs(cos(angle))*img.getHeight() + abs(sin(angle))*img.getWidth()));
	}
	
	// Add uscaled, rotated image to 1st-level cache
	private void addUnscaled(BufferedImage img, int angleIndex){
		ImageNode node = new ImageNode(img, angleIndex);
		unscaled[angleIndex] = node;
		cacheSize += node.size;
		rotatedCache.add(node);
		if (Game.freeMem < 40)
			maxCacheSize = max(MIN_CACHE_MEMORY*1000*1000, maxCacheSize*9/10);
		while (cacheSize > maxCacheSize)
			removeUnscaled(rotatedCache.peek());
		if (Game.freeMem < 40){
			Runtime.getRuntime().gc();
			java.lang.System.out.println("GC");
		}
	}
	
	// Update 1st-level cache priority of uscaled, rotated image
	private void updateUnscaled(int angleIndex){
		ImageNode node = unscaled[angleIndex];
		if (node != null){
			rotatedCache.remove(node);
			node.setPriority();
			rotatedCache.add(node);
		}
	}
	
	// Remove from 1st-level cache
	private void removeUnscaled(ImageNode node){
		unscaled[node.angle] = null;
		rotatedCache.remove(node);
		cacheSize -= node.size;
		node.img.flush();
	}
	
	private void queueQualityImage(double zoom, double angle){
		if (toLoad.size() < MAX_QUEUE_SIZE)
			toLoad.add(new ImageCoords(zoom, angle, false));
	}
	
	// Speculative addition of quality images to cache
	private void queueSpeculativeQualityImage(double zoom, double angle, int preloadDirection, int zoomIndex){
		if (preloadDirection == 0 || toLoad.size() > MAX_QUEUE_SIZE/4)
			return;
		angle += preloadDirection*angleIncrement;
		int angleIndex = getAngleIndex(angle, numRenders);
		if (zoom >= 1.0){
			if (unscaled[angleIndex] != null)
				return;
		}else{
			VolatileImage scaled = scaledImg[angleIndex][zoomIndex];
			if (scaled != null && !scaled.contentsLost() && scaledQuality[angleIndex][zoomIndex])
				return;
		}
		toLoad.add(new ImageCoords(zoom, angle, true));
	}
	
	// Read in original images from files
	public void load(BufferedImage[] inputImages, double scale){
		numOrigRenders = inputImages.length;
		
		unscaled = new ImageNode[numRenders];
		scaledZoom = new double[MAX_NUM_SCALES];
		numScaledZoom = 0;
		scaledImg = new VolatileImage[numRenders][MAX_NUM_SCALES];
		scaledQuality = new boolean[numRenders][MAX_NUM_SCALES];
		
		combine = inputImages.length > 1;
		
		int minSize = Integer.MAX_VALUE;
		size = 0;
		for (int inputIndex = 0; inputIndex < inputImages.length; inputIndex++){
			BufferedImage input = inputImages[inputIndex];
			
			BufferedImage untrimmed = Scalr.resize(input, Scalr.Method.ULTRA_QUALITY,
					(int)max(1, scale*input.getWidth()), (int)max(1, scale*input.getHeight()));
			Raster untrimmedAlpha = untrimmed.getAlphaRaster();
			
			// Shrink images to minimum bounding rectangle
			int width = untrimmed.getWidth();
			int dx = 1;
			while (width > 2){
				boolean contact = false;
				for (int y = 0; y < untrimmed.getHeight(); y++){
					contact = contact || untrimmedAlpha.getSample(untrimmed.getWidth()-dx, y, 0) > 1;
					contact = contact || untrimmedAlpha.getSample(dx-1, y, 0) > 1;
				}
				if (contact)
					break;
				width -= 2;
				dx += 1;
			}
			int height = untrimmed.getHeight();
			int dy = 1;
			while (height > 2){
				boolean contact = false;
				for (int x = 0; x < untrimmed.getWidth(); x++){
					contact = contact || untrimmedAlpha.getSample(x, untrimmed.getHeight()-dy, 0) > 2;
					contact = contact || untrimmedAlpha.getSample(x, dy-1, 0) > 2;
				}
				if (contact)
					break;
				height -= 2;
				dy += 1;
			}
			
			BufferedImage img = Main.getCompatibleImage(width, height, true);
			img.getGraphics().drawImage(untrimmed, width/2-untrimmed.getWidth()/2, height/2-untrimmed.getHeight()/2, null);
			untrimmed.flush();
			
			int index = inputIndex*numRenders/numOrigRenders;
			unscaled[index] = new ImageNode(img, index);
			cacheSize += unscaled[index].size;
			
			size = max(size, max(img.getHeight(), img.getWidth()));
			minSize = min(minSize, min(img.getHeight(), img.getWidth()));
			
			input.flush();
		}
		minZoom = 1.01*this.minSize/minSize;
	}
	
	// Load thread generates quality images as demanded
	public static void startLoadThread(){
		loadThread = new Thread("ImageLoadThread"){
			public void run(){
				while (true){
					try{
						ImageCoords coords = toLoad.take();
						int angleIndex = getAngleIndex(coords.angle, coords.renderable.numRenders);
						if (coords.zoom >= 1.0 && coords.renderable.unscaled[angleIndex] != null)
							continue;
						if (coords.zoom < 1.0){
							int zoomIndex = coords.renderable.getZoomIndex(coords.zoom);
							VolatileImage scaled = coords.renderable.scaledImg[angleIndex][zoomIndex];
							if (scaled != null && !scaled.contentsLost() && coords.renderable.scaledQuality[angleIndex][zoomIndex])
								continue;
							Image qualityImg = coords.renderable.makeQualityImage(coords.zoom, coords.angle);
							coords.renderable.scaledImg[angleIndex][zoomIndex] = (VolatileImage)qualityImg;
							coords.renderable.scaledQuality[angleIndex][zoomIndex] = true;
							if (scaled != null && toFlush.remainingCapacity() > 2)
								toFlush.add(scaled);
						}else
							coords.renderable.makeQualityImage(coords.zoom, coords.angle);
					}catch (InterruptedException e){
						e.printStackTrace();
					}
				}
			}
		};
		loadThread.start();
	}
	
	public static void flushTemp(){
		while (!toFlush.isEmpty())
			toFlush.poll().flush();
	}
	
	// Image specifications for load thread
	private class ImageCoords implements Comparable<ImageCoords>{
		final double zoom;
		final double angle;
		final Renderable renderable;
		final int priority;
		
		public ImageCoords(double zoom, double angle, boolean isSpeculative){
			this.zoom = zoom;
			this.angle = angle;
			renderable = Renderable.this;
			
			currentAge++;
			priority = isSpeculative ? currentAge+100 : currentAge;
		}
		
		public int compareTo(ImageCoords other){
			return (int)(priority-other.priority);
		}
	}
	
	// Node for 1st-level cache priority queue
	private class ImageNode implements Comparable<ImageNode>{
		final BufferedImage img;
		final int angle, size;
		int priority;
		
		public ImageNode(BufferedImage img, int angle){
			this.img = img;
			this.angle = angle%numRenders;
			
			size = img.getWidth()*img.getHeight()*4 + 300;
			setPriority();
		}
		
		void setPriority(){
			currentAge++;
			priority = currentAge;
		}
		
		public int compareTo(ImageNode other){
			return priority-other.priority;
		}
	}
	
}
