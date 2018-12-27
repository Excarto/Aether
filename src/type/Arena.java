import static java.lang.Math.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import org.imgscalr.*;

public class Arena extends Type{
	final static int MAX_PLAYERS = 10;
	final static int OBJ_ICON_SIZE = 12;
	final static Color OBJ_COLOR_NEUTRAL = new Color(120, 100, 120, 255);
	final static Color OBJ_COLOR_FRIENDLY = new Color(40, 140, 80, 255);
	final static Color OBJ_COLOR_HOSTILE = new Color(150, 40, 50, 255);
	final static int OBJ_BAR_SIZE = 25;
	final static int PREVIEW_SCALE_POS = 12, PREVIEW_SCALE_ARROW_SIZE = 6;
	final static Color WARP_IN_COLOR = new Color(150, 150, 150, 40);
	final static String CLOUD_DIR = "data/backgrounds/clouds/";
	final static String PLANET_DIR = "data/backgrounds/planets/";
	final static String RING_DIR = "data/backgrounds/rings/";
	
	static final Random rand = new Random();
	private static Cloud[] clouds, renderClouds;
	
	public static BufferedImage starBig, starSmall;
	
	public final String arenaName;
	public final String description;
	public final BackgroundObject[] backgroundObjects;
	public final ForegroundObject[] foregroundObjects;
	public final Objective[] objectives;
	public final int[][] teamPositions;
	public final int maxScore;
	public final int arenaSize;
	public final double startBudget;
	public final int incomeTime;
	public final int defaultBudget;
	public final int decapMultiplier;
	public final double totalObjectiveValue;
	
	private Map<String,Renderable> renderables;
	private int backgroundSeed;
	private BufferedImage background;
	
	public Arena(String arenaName){
		super("arenas/" + arenaName);
		this.arenaName = arenaName;
		renderables = new HashMap<String,Renderable>();
		
		description = getString("description");
		
		int numForegroundObjects = 0;
		while (hasValue("foreground_object" + (numForegroundObjects+1) + "_image"))
			numForegroundObjects++;
		foregroundObjects = new ForegroundObject[numForegroundObjects];
		int numObjectives = 0;
		for (int x = 0; x < foregroundObjects.length; x++){
			String objType = getString("foreground_object" + (x+1) + "_obj_type");
			numObjectives++;
			if (objType == null){
				numObjectives--;
				foregroundObjects[x] = new ForegroundObject(x+1);
			}else if (objType.equals("transient")){
				foregroundObjects[x] = new Objective(x+1, false, true);
			}else if (objType.equals("permanant")){
				foregroundObjects[x] = new Objective(x+1, true, false);
			}else if (objType.equals("normal"))
				foregroundObjects[x] = new Objective(x+1, false, false);
		}
		
		objectives = new Objective[numObjectives];
		numObjectives = 0;
		for (ForegroundObject object : foregroundObjects){
			if (object instanceof Objective)
				objectives[numObjectives++] = (Objective)object;
		}
		double value = 0.0;
		for (Objective objective : objectives)
			value += objective.value;
		totalObjectiveValue = value;
		
		int numBackgroundObjects = 0;
		while (getString("background_object" + (numBackgroundObjects+1) + "_image") != null)
			numBackgroundObjects++;
		backgroundObjects = new BackgroundObject[numBackgroundObjects];
		for (int x = 0; x < backgroundObjects.length; x++)
			backgroundObjects[x] = new BackgroundObject(x+1);
		
		int numPlayers = 0;
		while (hasValue("player" + (numPlayers+1) + "_x_pos") && numPlayers < MAX_PLAYERS)
			numPlayers++;
		teamPositions = new int[numPlayers][2];
		for (int x = 0; x < teamPositions.length; x++){
			teamPositions[x][0] = getInt("player" + (x+1) + "_x_pos");
			teamPositions[x][1] = getInt("player" + (x+1) + "_y_pos");
		}
		
		defaultBudget = getInt("default_budget");
		maxScore = getInt("max_score");
		startBudget = getDouble("start_budget") <= 0 ? 1.0 : getDouble("start_budget");
		incomeTime = getInt("income_time")*Main.TPS;
		decapMultiplier = getInt("decap_multiplier") == 0 ? 1 : getInt("decap_multiplier");
		
		String[] cloudFiles = new File(CLOUD_DIR).list();
		Arrays.sort(cloudFiles);
		clouds = new Cloud[cloudFiles.length];
		for (int x = 0; x < clouds.length; x++)
			clouds[x] = new Cloud(CLOUD_DIR + cloudFiles[x]);
		renderClouds = new Cloud[2];
		
		int minY = 0, maxY = 0, minX = 0, maxX = 0;
		for (ForegroundObject object : foregroundObjects){
			minY = min(minY, (int)object.getPosY());
			maxY = max(maxY, (int)object.getPosY());
			minX = min(minX, (int)object.getPosX());
			maxX = max(maxX, (int)object.getPosX());
		}
		for (int[] position : teamPositions){
			minY = min(minY, position[1]);
			maxY = max(maxY, position[1]);
			minX = min(minX, position[0]);
			maxX = max(maxX, position[0]);
		}
		arenaSize = max(maxX-minX, maxY-minY);
	}
	
	public void move(){
		for (ForegroundObject object : foregroundObjects)
			object.move();
	}
	
	public void act(){
		for (ForegroundObject object : foregroundObjects)
			object.act();
	}
	
	public void initialize(int randomSeed){
		rand.setSeed(randomSeed);
		
		backgroundSeed = randomSeed;
		for (ForegroundObject object : foregroundObjects){
			object.place(getInt("foreground_object" + object.index + "_x_pos"),
					getInt("foreground_object" + object.index + "_y_pos"),
					getInt("foreground_object" + object.index + "_x_vel")/Main.TPS,
					getInt("foreground_object" + object.index + "_y_vel")/Main.TPS,
					0.0, 0.0);
			if (object instanceof Objective){
				((Objective)object).owner = 0;
				((Objective)object).capAmount = 0;
				((Objective)object).isCaptured = false;
			}
		}
		
		if (randomSeed != 0){
			if (renderClouds[0] != null){
				renderClouds[0].unloadImg();
				renderClouds[1].unloadImg();
			}
			do{
				renderClouds[0] = clouds[rand.nextInt(clouds.length)];
				renderClouds[1] = clouds[rand.nextInt(clouds.length)];
			}while (renderClouds[0] == renderClouds[1] ||
					max(renderClouds[0].distance, renderClouds[1].distance)/min(renderClouds[0].distance, renderClouds[1].distance) < 1.5);
			for (int x = 0; x < renderClouds.length; x++){
				renderClouds[x].loadImg();
				renderClouds[x].setPos(rand);
			}
			
			if (background != null){
				background.flush();
				background = null;
			}
		}
		//java.lang.System.out.println(renderClouds[0].file+" "+renderClouds[1].file);
	}
	
	private BufferedImage getImage(String name){
		String filename = directory + "/" + name + ".png";
		BufferedImage image = null;
		try{
			image = Main.convert(ImageIO.read(new File(filename)));
		}catch(IOException e){
			Main.crash(filename);
		}
		return image;
	}
	
	private Renderable getRenderable(String imageName){
		if (!renderables.containsKey(imageName))
			renderables.put(imageName, new Renderable(OBJ_ICON_SIZE, 1));
		return renderables.get(imageName);
	}
	
	public void draw(Graphics2D g, GameWindow window){
		//g.drawImage(getBackground(window), 0, 0, null);
		
		for (Arena.Cloud cloud : renderClouds){
			VolatileImage cloudImg = cloud.getImage();
			int dist = (int)(cloud.distance + 1.0/window.getZoom());
			g.drawImage(cloudImg,
					cloud.posX-window.getPosX()/dist+window.windowResX/2-cloudImg.getWidth()/2,
					cloud.posY-window.getPosY()/dist+GameWindow.WINDOW_RES_Y/2-cloudImg.getHeight()/2,
					null);
		}
		
		for (Arena.BackgroundObject object : backgroundObjects){
			g.drawImage(object.getImg(),
					object.posX-window.getPosX()/object.distance+window.windowResX/2-object.getImg().getWidth()/2,
					object.posY-window.getPosY()/object.distance+GameWindow.WINDOW_RES_Y/2-object.getImg().getHeight()/2,
					null);
		}
		
		for (ForegroundObject object : foregroundObjects)
			object.draw(g, window);
		
		if (startBudget < 1.0){
			g.setColor(WARP_IN_COLOR);
			int size = (int)(Ship.WARP_POS_RADIUS*window.getZoom());
			int[] position = teamPositions[window.player.position];
			g.fillOval(window.posXOnScreen(position[0])-size, window.posYOnScreen(position[1])-size, 2*size, 2*size);
		}
	}
	
	public void drawBackground(Graphics2D g, GameWindow window){
		if (background == null)
			background = generateBackground(backgroundSeed);
		g.drawImage(background, 0, 0, null);
		background.flush();
	}
	
	public void drawPreview(Graphics2D g, int size){
		initialize(0);
		double scale = size/(1.25*arenaSize);
		
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, size, size);
		
		for (ForegroundObject object : foregroundObjects){
			int posX = size/2 + (int)(scale*object.getPosX());
			int posY = size/2 + (int)(scale*object.getPosY());
			object.drawPreview(g, posX, posY);
		}
		
		for (int x = 0; x < teamPositions.length; x++){
			Font font = new Font("Arial", Font.BOLD, 15);
			FontMetrics metrics = g.getFontMetrics(font);
			String label = String.valueOf(x+1);
			int posX = size/2 + (int)(scale*teamPositions[x][0]);
			int posY = size/2 + (int)(scale*teamPositions[x][1]);
			g.setFont(font);
			g.setColor(Color.WHITE);
			g.drawString(label, posX-metrics.stringWidth(label)/2, posY+metrics.getHeight()/2);
		}
		
		Font font = new Font("Arial", Font.PLAIN, 12);
		FontMetrics metrics = g.getFontMetrics(font);
		String sizeLabel = String.valueOf(arenaSize);
		g.setFont(font);
		g.setColor(Color.LIGHT_GRAY);
		g.drawString(sizeLabel, size/2-metrics.stringWidth(sizeLabel)/2, size-PREVIEW_SCALE_POS+metrics.getHeight()/2-2);
		g.drawLine(0, size-PREVIEW_SCALE_POS, size/2-30, size-PREVIEW_SCALE_POS);
		g.drawLine(size, size-PREVIEW_SCALE_POS, size/2+30, size-PREVIEW_SCALE_POS);
		g.drawLine(0, size-PREVIEW_SCALE_POS, PREVIEW_SCALE_ARROW_SIZE, size-PREVIEW_SCALE_POS-PREVIEW_SCALE_ARROW_SIZE);
		g.drawLine(0, size-PREVIEW_SCALE_POS, PREVIEW_SCALE_ARROW_SIZE, size-PREVIEW_SCALE_POS+PREVIEW_SCALE_ARROW_SIZE);
		g.drawLine(size, size-PREVIEW_SCALE_POS, size-PREVIEW_SCALE_ARROW_SIZE, size-PREVIEW_SCALE_POS-PREVIEW_SCALE_ARROW_SIZE);
		g.drawLine(size, size-PREVIEW_SCALE_POS, size-PREVIEW_SCALE_ARROW_SIZE, size-PREVIEW_SCALE_POS+PREVIEW_SCALE_ARROW_SIZE);
	}
	
	public String toString(){
		return arenaName;
	}
	
	public void load(){
		for (String imageName : renderables.keySet())
			renderables.get(imageName).load(new BufferedImage[]{Arena.this.getImage(imageName)}, 1.0);
		renderables = null;
		for (BackgroundObject object : backgroundObjects)
			object.load();
	}
	
	private BufferedImage generateBackground(int randomSeed){
		rand.setSeed(randomSeed);
		
		BufferedImage background = new BufferedImage(
				Main.resX-GameWindow.MENU_WIDTH-GameWindow.DIVIDER_WIDTH, GameWindow.WINDOW_RES_Y, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = (Graphics2D)background.getGraphics();
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, background.getWidth(), background.getHeight());
		
		drawStars(g, background.getWidth(), background.getHeight());
		drawClouds(g, background.getWidth(), background.getHeight());
		drawPlanets(g, background.getWidth(), background.getHeight());
		
		g.dispose();
		return background;
	}
	
	private void drawStars(Graphics2D g, int width, int height){
		double[] colorMult = new double[3];
		int[] pixel = new int[4];
		BufferedImage coloredBig = new BufferedImage(
					starBig.getWidth(), starBig.getHeight(), BufferedImage.TYPE_INT_ARGB);
		BufferedImage coloredSmall = new BufferedImage(
					starSmall.getWidth(), starSmall.getHeight(), BufferedImage.TYPE_INT_ARGB);
		
		for (int i = 0; i < 7000; i++){
			colorMult[0] = 1.0-0.25*rand.nextDouble();
			colorMult[1] = 1.0-0.12*rand.nextDouble();
			colorMult[2] = 1.0-0.15*rand.nextDouble();
			
			double scale = pow(rand.nextDouble(), 45.0);
			int size = (int)(scale*min(starBig.getWidth(), starBig.getHeight()));
			int posX = (int)(width*rand.nextDouble());
			int posY = (int)(height*rand.nextDouble());
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
	
	private void drawClouds(Graphics2D g, int width, int height){
		for (int i = 0; i <= 2; i++){
			Cloud cloud = null;
			boolean cloudDuplicate;
			do{
				cloud = clouds[rand.nextInt(clouds.length)];
				cloudDuplicate = false;
				for (int x = 0; x < renderClouds.length; x++)
					cloudDuplicate = cloudDuplicate || renderClouds[x] == cloud;
			}while (cloudDuplicate);
			
			int cloudPosX, cloudPosY;
			do{
				cloudPosX = rand.nextInt(width);
				cloudPosY = rand.nextInt(height);
			}while (abs(cloudPosX - width/2) < width/6 || abs(cloudPosY - height/2) < height/6);
			
			g.setComposite(AlphaComposite.SrcOver.derive(0.32f));
			VolatileImage cloudImg = cloud.getImage();
			g.drawImage(cloudImg, cloudPosX-cloudImg.getWidth()/2, cloudPosY-cloudImg.getHeight()/2, null);
			g.setComposite(AlphaComposite.SrcOver);
			cloud.unloadImg();
		}
	}
	
	private void drawPlanets(Graphics2D g, int width, int height){
		String[] planets = new File("data/backgrounds/planets").list();
		Arrays.sort(planets);
		int numPlanets = rand.nextInt(5);
		if (numPlanets == 0 || numPlanets == 1 || numPlanets == 2){
			numPlanets = 1;
		}else if (numPlanets == 3){
			numPlanets = 2;
		}else
			numPlanets = 0;
		numPlanets = min(numPlanets, planets.length);
		
		if (numPlanets == 1 && rand.nextDouble() < 0.81){
			String planetName = planets[rand.nextInt(planets.length)];
			BufferedImage planetImg = null;
			try{
				planetImg = ImageIO.read(new File(PLANET_DIR+planetName));
			}catch(IOException e){
				Main.crash(PLANET_DIR+planetName);
			}
			
			String[] rings = new File(RING_DIR).list();
			Arrays.sort(rings);
			String ringName = rings[rand.nextInt(rings.length)];
			
			BufferedImage ringImg = null, shadowImg = null;
			int size = 0, planetPosX = 0, planetPosY = 0;
			try{
				Map<String,String> data = Main.readDataFile(RING_DIR + ringName + "/data.txt");
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
			boolean[] chosen = new boolean[planets.length];
			int[][] positions = new int[numPlanets][2];
			for (int x = 0; x < numPlanets; x++){
				int index;
				do{
					index = rand.nextInt(planets.length);
				}while (chosen[index]);
				chosen[index] = true;
				BufferedImage planetImg = null;
				try{
					planetImg = ImageIO.read(new File(PLANET_DIR+planets[index]));
				}catch(IOException e){
					Main.crash(PLANET_DIR+planets[index]);
				}
				
				double scale = /*0.09+0.91*/0.09+0.6*maxScale*pow(rand.nextDouble(), 0.65);
				maxScale *= 1.0 - scale;
				BufferedImage scaled = Scalr.resize(planetImg, Scalr.Method.ULTRA_QUALITY,
						(int)(scale*planetImg.getWidth()), (int)(scale*planetImg.getHeight()));
				
				int radius = max(scaled.getWidth(), scaled.getHeight())/2;
				int bound = radius/2;
				boolean intersects;
				do{
					positions[x][0] = rand.nextInt(Main.RES_X_NARROW-scaled.getWidth()+2*bound)-bound;
					positions[x][1] = rand.nextInt(Main.RES_Y-scaled.getHeight()+2*bound)-bound;
					intersects = false;
					for (int y = 0; y < x; y++)
						intersects = intersects || hypot(positions[x][0]-positions[y][0], positions[x][1]-positions[y][1]) < 1.3*radius;
				}while (intersects);
				g.drawImage(scaled, positions[x][0], positions[x][1], null);
			}
		}
	}
	
	public static void loadClouds(){
		for (Cloud cloud : clouds)
			cloud.load();
	}
	
	public class ForegroundObject extends Sprite{
		static final int PREVIEW_SIZE = 24;
		public final int index;
		public final BufferedImage previewImg;
		
		public ForegroundObject (int index){
			super(getRenderable(getString("foreground_object" + index + "_image")));
			this.index = index;
			BufferedImage image = Arena.this.getImage(getString("foreground_object" + index + "_image"));
			previewImg = Scalr.resize(image, Scalr.Method.QUALITY, PREVIEW_SIZE, PREVIEW_SIZE);
			image.flush();
		}
		
		public void draw(Graphics2D g, GameWindow window){
			int posX = (int)window.posXOnScreen(getPosX()), posY = (int)window.posYOnScreen(getPosY());
			g.setColor(getColor(window.getPlayer()));
			super.draw(g, window);
			window.drawPointerLine(g, posX, posY, null);
		}
		
		public void drawPreview(Graphics2D g, int posX, int posY){
			g.drawImage(previewImg, posX-ForegroundObject.PREVIEW_SIZE/2, posY-ForegroundObject.PREVIEW_SIZE/2, null);
		}
		
		public Image getImg(double zoom){
			return renderable.getImage(zoom, 0, false, 0);
		}
		
		public void act(){}
		public int getIconSize(){
			return OBJ_ICON_SIZE;
		}
		public double getVisionSize(){
			return Double.MAX_VALUE;
		}
		public double getRadarSize(){
			return 0.0;
		}
		
		public Color getColor(Player player){
			return Color.GRAY;
		}
	}
	
	public class Objective extends ForegroundObject{
		final boolean isPermanant, isTransient;
		final double value;
		final int capSize;
		
		int owner;
		int capAmount;
		boolean isCaptured;
		
		private int lastCapTime;
		private int capturingTeam;
		
		CaptureMsg capMsg;
		
		public Objective(int index, boolean isPermanant, boolean isTransient){
			super(index);
			value = getInt("foreground_object" + index + "_obj_value")/(double)Main.TPS;
			capSize = getInt("foreground_object" + index + "_obj_capture_size");
			this.isPermanant = isPermanant;
			this.isTransient = isTransient;
			isCaptured = false;
		}
		
		public void capture(Player player, int amount){
			lastCapTime = Main.game.turn;
			capturingTeam = player.team;
			
			if (player.team == owner){
				capAmount += amount;
				if (capAmount >= capSize){
					isCaptured = true;
					capAmount = capSize;
				}
			}else{
				capAmount -= decapMultiplier*amount;
				if (capAmount <= 0){
					owner = player.team;
					isCaptured = false;
					capAmount = 0;
				}
			}
		}
		
		public void draw(Graphics2D g, GameWindow window){
			super.draw(g, window);
			
			Image img = getImage(window.getRenderZoom(), window.renderTimeLeft());
			if (img != null){
				int posX = (int)window.posXOnScreen(getPosX()), posY = (int)window.posYOnScreen(getPosY());
				
				g.setColor(getColor(window.getPlayer()));
				int barPosY = posY+img.getHeight(null)/2-OBJ_BAR_SIZE+1+((OBJ_BAR_SIZE-1)-((OBJ_BAR_SIZE-1)*capAmount/capSize));
				g.fillRect(posX-img.getWidth(null)/2+1, barPosY, 4, (OBJ_BAR_SIZE-1)*capAmount/capSize);
				if (!isCaptured && capAmount > 0){
					g.setColor(owner == window.getPlayer().team ? OBJ_COLOR_FRIENDLY : OBJ_COLOR_HOSTILE);
					g.drawLine(posX-img.getWidth(null)/2+1, barPosY-1, posX-img.getWidth(null)/2+4, barPosY-1);
				}
				g.setColor(Color.LIGHT_GRAY);
				g.drawRect(posX-img.getWidth(null)/2, posY+img.getHeight(null)/2-OBJ_BAR_SIZE,
						5, OBJ_BAR_SIZE);
			}
		}
		
		public void drawPreview(Graphics2D g, int posX, int posY){
			super.drawPreview(g, posX, posY);
			g.setColor(Color.WHITE);
			g.setFont(new Font("Arial", Font.PLAIN, 10));
			g.drawString(String.valueOf((int)round(value*Main.TPS)), posX-PREVIEW_SIZE/5, posY+PREVIEW_SIZE);
		}
		
		public CaptureMsg getCapMsg(Player player, int amount){
			if (capMsg == null){
				capMsg = new CaptureMsg();
				capMsg.objective = this;
			}
			capMsg.player = player;
			capMsg.amount = amount;
			return capMsg;
		}
		
		public Color getColor(Player viewer){
			if (isCaptured){
				if (owner == viewer.team){
					return OBJ_COLOR_FRIENDLY;
				}else
					return OBJ_COLOR_HOSTILE;
			}else
				return OBJ_COLOR_NEUTRAL;
		}
		
		public int getCapturingTeam(){
			return Main.game.turn-lastCapTime < 2*Capture.TURNS_CAPTURE ? capturingTeam : -1;
		}
	}
	
	public class BackgroundObject{
		public final int posX, posY;
		public final int distance;
		public final int index;
		
		private BufferedImage img;
		
		public BackgroundObject (int index){
			this.index = index;
			posX = getInt("background_object" + index + "_x_pos");
			posY = getInt("background_object" + index + "_y_pos");
			distance = getInt("background_object" + index + "_distance");
		}
		
		public void load(){
			img = getImage(getString("background_object" + index + "_image"));
		}
		
		public BufferedImage getImg(){
			return img;
		}
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
				//img = Main.convert(ImageIO.read(new File(file)));
				img = Main.convertVolatile(ImageIO.read(new File(file)));
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
	
}
