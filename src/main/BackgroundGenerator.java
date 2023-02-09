import static java.lang.Math.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import org.imgscalr.*;

// Generates components of the background image: starfield, planets, cloud layers, and nebula.

public class BackgroundGenerator{
	
	final static String CLOUD_DIR = "data/backgrounds/clouds/";
	final static String PLANET_DIR = "data/backgrounds/planets/";
	final static String RING_DIR = "data/backgrounds/rings/";
	final static String NEBULA_DIR = "data/backgrounds/nebulae/";
	final static double MIN_CLOUD_OPACITY = 0.23, MAX_CLOUD_OPACITY = 0.30;
	final static double MIN_NEBULA_OPACITY = 0.65;
	final static int NUM_STARS = 7000;
	
	private final Random rand = new Random();
	private BufferedImage starBig, starSmall;
	
	private int backgroundSeed;
	private int nebulaIndex;
	private double nebulaOpacity;
	private double cloudOpacity;
	private Planet[] planets;
	
	private Cloud[] clouds;
	private Cloud[] foregroundClouds;
	
	public BackgroundGenerator(){
		foregroundClouds = new Cloud[0];
		
		String[] cloudFiles = new File(CLOUD_DIR).list();
		Arrays.sort(cloudFiles);
		clouds = new Cloud[cloudFiles.length];
		for (int x = 0; x < clouds.length; x++)
			clouds[x] = new Cloud(CLOUD_DIR + cloudFiles[x]);
	}
	
	public void initialize(int randomSeed){
		initialize(randomSeed, -1, -1.0, -1.0, null);
	}
	
	public void initialize(int randomSeed, int nebulaIndex, double nebulaOpacity, double cloudOpacity, Planet[] planets){
		backgroundSeed = randomSeed == 0 ? (int)(Integer.MAX_VALUE*random()) : randomSeed;
		this.nebulaIndex = nebulaIndex;
		this.nebulaOpacity = nebulaOpacity;
		this.planets = planets;
		
		for (Cloud cloud : foregroundClouds)
			cloud.unloadImg();
		
		rand.setSeed(backgroundSeed);
		
		if (cloudOpacity < 0 && nebulaOpacity < 0 && rand.nextDouble() < 0.10){
			cloudOpacity = 0.0;
			nebulaOpacity = 0.0;
		}
		
		if (cloudOpacity < 0)
			cloudOpacity = MIN_CLOUD_OPACITY + rand.nextDouble()*(MIN_CLOUD_OPACITY-MIN_CLOUD_OPACITY);
		
		this.cloudOpacity = cloudOpacity;
		
		if (cloudOpacity <= 0.0){
			foregroundClouds = new Cloud[0];
		}else{
			foregroundClouds = new Cloud[2];
			
			double minDist, maxDist;
			do{
				foregroundClouds[0] = clouds[rand.nextInt(clouds.length)];
				foregroundClouds[1] = clouds[rand.nextInt(clouds.length)];
				minDist = min(foregroundClouds[0].distance, foregroundClouds[1].distance);
				maxDist = max(foregroundClouds[0].distance, foregroundClouds[1].distance);
			}while (foregroundClouds[0] == foregroundClouds[1] || maxDist/minDist < 1.5);
			
			for (int x = 0; x < foregroundClouds.length; x++){
				foregroundClouds[x].loadImg();
				foregroundClouds[x].setPos(rand);
			}
		}	
	}
	
	public void drawForegroundClouds(Graphics2D g, GameWindow window){
		for (Cloud cloud : foregroundClouds){
			VolatileImage cloudImg = cloud.getImage();
			int dist = (int)(cloud.distance + 1.0/window.getZoom());
			g.drawImage(cloudImg,
					cloud.posX - window.getPosX()/dist + window.windowResX/2 - cloudImg.getWidth()/2,
					cloud.posY - window.getPosY()/dist + window.windowResY/2 - cloudImg.getHeight()/2,
					null);
		}
	}
	
	public void drawBackground(Graphics2D g, GameWindow window){
		rand.setSeed(backgroundSeed);
		if (Main.DEBUG)
			Main.print("BG seed= " + backgroundSeed);
		
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		//BufferedImage background = new BufferedImage(
		//		Main.resX-GameWindow.MENU_WIDTH-GameWindow.DIVIDER_WIDTH, Main.resY, BufferedImage.TYPE_INT_RGB);
		//Graphics2D g = (Graphics2D)background.getGraphics();
		int width = window.windowResX;
		int height = window.windowResY;
		
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, width, height);
		
		drawBackgroundStars(g, width, height);
		//drawBackgroundClouds(g, background.getWidth(), background.getHeight());
		if (rand.nextDouble() < 0.65){
			drawBackgroundNebula(g, width, height, nebulaIndex, nebulaOpacity);
			drawBackgroundPlanets(g, width, height, planets);
		}else{
			drawBackgroundPlanets(g, width, height, planets);
			drawBackgroundNebula(g, width, height, nebulaIndex, nebulaOpacity);
		}
		
		//g.dispose();
		//return background;
	}
	
	// Places starts uniformly randomly, plus a curved band of stars for "milky way" effect
	private void drawBackgroundStars(Graphics2D g, int width, int height){
		double[] colorMult = new double[3];
		int[] pixel = new int[4];
		BufferedImage coloredBig = new BufferedImage(
					starBig.getWidth(), starBig.getHeight(), BufferedImage.TYPE_INT_ARGB);
		BufferedImage coloredSmall = new BufferedImage(
					starSmall.getWidth(), starSmall.getHeight(), BufferedImage.TYPE_INT_ARGB);
		
		double bandPosX = width/10 + rand.nextInt(width*8/10);
		double bandPosY = height/10 + rand.nextInt(height*8/10);
		double bandAng = rand.nextDouble()*360.0;
		double bandStrength = 0.5 + 7.0*pow(rand.nextDouble(), 5.5);
		double bandWidth = 150 + rand.nextInt(150);
		double bandCurve = (rand.nextBoolean() ? 1 : -1)*(250 + rand.nextDouble()*400);
		double normalUnitX = -cos(toRadians(bandAng));
		double normalUnitY = -sin(toRadians(bandAng));
		
		int numStars = (int)(NUM_STARS*(1 + bandStrength));
		for (int i = 0; i < numStars; i++){
			
			// Stars are slightly colored
			colorMult[0] = 1.0-0.25*rand.nextDouble();
			colorMult[1] = 1.0-0.12*rand.nextDouble();
			colorMult[2] = 1.0-0.15*rand.nextDouble();
			
			double scale = pow(rand.nextDouble(), 50.0);
			int size = (int)(scale*min(starBig.getWidth(), starBig.getHeight()));
			int posX = (int)(width*rand.nextDouble());
			int posY = (int)(height*rand.nextDouble());
			
			if (rand.nextDouble() < bandStrength/(1+bandStrength)){
				double dX = bandPosX-posX, dY = bandPosY-posY;
				double dist = dX*normalUnitX + dY*normalUnitY;
				double span = -dX*normalUnitY + dY*normalUnitX;
				dist += pow(span/Main.RES_Y_TALL, 2)*bandCurve;
				double weight = exp(-pow(abs(dist)/bandWidth, 1.5));
				if (rand.nextDouble() > weight)
					continue;
			}
			
			if (size <= 1){
				int alpha = (int)(255*0.6*pow(rand.nextDouble(), 2.0));
				g.setColor(new Color((int)(255*colorMult[0]), (int)(255*colorMult[1]), (int)(255*colorMult[2]), alpha));
				g.drawLine(posX, posY, posX, posY);
			}else{
				BufferedImage original, colored;
				if (size > starSmall.getWidth()){
					original = starBig;
					colored = coloredBig;
				}else{
					original = starSmall;
					colored = coloredSmall;
					scale *= (double)starBig.getWidth()/starSmall.getWidth();
				}
				
				WritableRaster coloredRaster = colored.getRaster();
				WritableRaster raster = original.getRaster();
				for (int x = 0; x < raster.getWidth(); x++){
					for (int y = 0; y < raster.getHeight(); y++){
						raster.getPixel(x, y, pixel);
						for (int z = 0; z < 3; z++)
							pixel[z] = (int)(pixel[z]*colorMult[z]);
						coloredRaster.setPixel(x, y, pixel);
					}
				}
				
				BufferedImage scaled = Scalr.resize(colored, Scalr.Method.BALANCED,
						(int)max(1, scale*colored.getWidth()), (int)max(1, scale*colored.getHeight()));
				g.drawImage(scaled, posX, posY, null);
				scaled.flush();
			}
		}
	}
	
	private void drawBackgroundNebula(Graphics2D g, int width, int height, int index, double opacity){
		if (opacity == 0.0)
			return;
		
		String[] nebulae = new File(NEBULA_DIR).list();
		Arrays.sort(nebulae);
		
		if (index < 0)
			index = rand.nextInt(nebulae.length);
		String nebula = nebulae[index];
		BufferedImage nebulaImg = null;
		try{
			nebulaImg = ImageIO.read(new File(NEBULA_DIR+nebula));
		}catch(IOException e){
			Main.crash(NEBULA_DIR+nebula);
		}
		
		if (opacity < 0)
			opacity = MIN_NEBULA_OPACITY + (1 - MIN_NEBULA_OPACITY)*rand.nextDouble();
		
		if (Main.DEBUG)
			Main.print("BG nebula= " + index + " " + opacity);
		
		g.setComposite(AlphaComposite.SrcOver.derive((float)opacity));
		g.drawImage(nebulaImg, -(nebulaImg.getWidth()-width)/2, -(nebulaImg.getHeight()-height)/2, null);
		g.setComposite(Window.DEFAULT_COMPOSITE);
	}
	
	private void drawBackgroundPlanets(Graphics2D g, int width, int height, Planet[] planets){
		
		int numPlanets;
		if (planets != null){
			numPlanets = planets.length;
		}else{
			double planetRand = rand.nextDouble();
			if (planetRand < 0.75){
				numPlanets = 0;
			}else {
				planetRand = (planetRand - 0.7)/(1 - 0.7);
				if (planetRand < 0.75){
					numPlanets = 1;
				}else
					numPlanets = 2;
			}
		}
		
		//numPlanets = 1;//////////////////////////////////////////////////////////////
		if (numPlanets == 0)
			return;
		
		String[] availPlanets = new File(PLANET_DIR).list();
		Arrays.sort(availPlanets);
		numPlanets = min(numPlanets, availPlanets.length);
		
		boolean incRing;
		if (planets != null){
			incRing = planets[0].ringIndex >= 0;
		}else{
			incRing = numPlanets == 1 && rand.nextDouble() < 0.65;
		}
		
		//incRing = true;///////////////////////////////////////////////
		if (incRing){
			
			int planetIndex;
			if (planets == null){
				planetIndex = rand.nextInt(availPlanets.length);
			}else
				planetIndex = planets[0].index;
			String planetName = availPlanets[planetIndex];
			
			BufferedImage planetImg = null;
			try{
				planetImg = ImageIO.read(new File(PLANET_DIR+planetName));
			}catch(IOException e){
				Main.crash(PLANET_DIR+planetName);
			}
			
			String[] rings = new File(RING_DIR).list();
			Arrays.sort(rings);
			
			int ringIndex;
			if (planets == null){
				ringIndex = rand.nextInt(rings.length);
			}else
				ringIndex = planets[0].ringIndex;
			String ringName = rings[ringIndex];
			
			if (Main.DEBUG)
				Main.print("BG ring= " + ringName);
			
			BufferedImage ringImg = null, shadowImg = null;
			int size = 0, planetPosX = 0, planetPosY = 0;
			try{
				Map<String,String> data = Utility.readDataFile(RING_DIR + ringName + "/data.txt");
				ringImg = ImageIO.read(new File(RING_DIR + ringName + "/ring.png"));
				shadowImg = ImageIO.read(new File(RING_DIR + ringName + "/shadow.png"));
				size = Integer.parseInt(data.get("size"));
				planetPosX = Integer.parseInt(data.get("posx"));
				planetPosY = Integer.parseInt(data.get("posy"));
			}catch(Exception e){
				e.printStackTrace();
				Main.crash(RING_DIR+ringName);
			}
			
			BufferedImage scaledPlanetImg = Scalr.resize(planetImg, Scalr.Method.ULTRA_QUALITY, size, size);
			planetImg.flush();
			Graphics2D gPlanet = scaledPlanetImg.createGraphics();
			gPlanet.setComposite(AlphaComposite.SrcAtop);
			gPlanet.drawImage(shadowImg, -planetPosX, -planetPosY, null);
			gPlanet.dispose();
			
			int ringPosX = width/2 - ringImg.getWidth()/2;
			int ringPosY = height/2 - ringImg.getHeight()/2;
			g.drawImage(scaledPlanetImg, ringPosX+planetPosX, ringPosY+planetPosY, null);
			g.drawImage(ringImg, ringPosX, ringPosY, null);
		}else{
			
			double maxScale = 0.65;
			boolean[] chosen = new boolean[availPlanets.length];
			int[][] positions = new int[numPlanets][2];
			for (int x = 0; x < numPlanets; x++){
				int index;
				double scale;
				
				if (planets != null){
					index = planets[x].index;
					scale = planets[x].scale;
				}else{
					do{
						index = rand.nextInt(availPlanets.length);
					}while (chosen[index]);
					chosen[index] = true;
					scale = 0.07 + 0.6*maxScale*pow(rand.nextDouble(), 0.65);
					maxScale *= 1.0 - scale;
				}
				
				BufferedImage planetImg = null;
				try{
					planetImg = ImageIO.read(new File(PLANET_DIR+availPlanets[index]));
				}catch(IOException e){
					Main.crash(PLANET_DIR+availPlanets[index]);
				}
				
				BufferedImage scaled = Scalr.resize(planetImg, Scalr.Method.ULTRA_QUALITY,
						(int)(scale*planetImg.getWidth()), (int)(scale*planetImg.getHeight()));
				
				if (planets != null){
					positions[x][0] = width/2 + (int)(width*planets[x].posX)/2;
					positions[x][1] = height/2 - (int)(height*planets[x].posY)/2;
				}else{
					int radius = max(scaled.getWidth(), scaled.getHeight())/2;
					int bound = radius/2;
					boolean intersects;
					do{
						positions[x][0] = rand.nextInt(Main.RES_X_NARROW-scaled.getWidth()+2*bound)-bound;
						positions[x][1] = rand.nextInt(Main.resY-scaled.getHeight()+2*bound)-bound;
						intersects = false;
						for (int y = 0; y < x; y++)
							intersects = intersects || hypot(positions[x][0]-positions[y][0], positions[x][1]-positions[y][1]) < 1.3*radius;
					}while (intersects);
				}
				
				g.drawImage(scaled, positions[x][0], positions[x][1], null);
			}
		}
	}
	
	public void load(){
		try{
			starBig = ImageIO.read(new File("data/star_big.png"));
			starSmall = ImageIO.read(new File("data/star_small.png"));
		}catch (IOException ex){
			starBig = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			starSmall = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		}
		
		for (Cloud cloud : clouds)
			cloud.load();
	}
	
	private class Cloud{
		public int posX, posY;
		public int distance;
		
		private VolatileImage img;
		private final String file;
		
		public Cloud(String file){
			posX = 0;
			posY = 0;
			this.file = file;
		}
		
		public void load(){
			loadImg();
			distance = 800000/(img.getWidth() + img.getHeight());
			unloadImg();
		}
		
		public void setPos(Random random){
			posX = (int)(100*(2*random.nextDouble() - 1));
			posY = (int)(100*(2*random.nextDouble() - 1));
		}
		
		public VolatileImage getImage(){
			if (img == null || img.contentsLost())
				loadImg();
			return img;
		}
		
		public void loadImg(){
			try{
				BufferedImage orig = ImageIO.read(new File(file));
				//img = Main.convertVolatile(orig);
				img = Main.getCompatibleVolatileImage(orig.getWidth(), orig.getHeight(), true);
				Graphics2D g = img.createGraphics();
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
				g.fillRect(0, 0, img.getWidth(), img.getHeight());
				g.setComposite(AlphaComposite.SrcOver.derive((float)cloudOpacity));
				g.drawImage(orig, 0, 0, null);
				g.dispose();
			}catch(IOException e){
				e.printStackTrace();
				Main.crash(file);
			}
		}
		
		public void unloadImg(){
			img.flush();
			img = null;
		}
	}
	
	public class Planet{
		int index, ringIndex;
		double posX, posY;
		double scale;
		
		public Planet(int index, int ringIndex, double posX, double posY, double scale){
			this.index = index;
			this.ringIndex = ringIndex;
			this.posX = posX;
			this.posY = posY;
			this.scale = scale;
		}
	}
	
}
