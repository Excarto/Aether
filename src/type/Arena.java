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
	static Font previewFontSmall, previewFontLarge;
	
	public static BackgroundGenerator background = new BackgroundGenerator();
	
	public final String arenaName;
	public final String shortName;
	public final String description;
	public final BackgroundObject[] backgroundObjects;
	public final ForegroundObject[] foregroundObjects;
	public final Objective[] objectives;
	public final int[][] teamPos;
	public final double[][] teamVel;
	public final int maxScore;
	public final int arenaSize;
	public final double startBudget;
	public final int incomeTime;
	public final int defaultBudget;
	public final int decapMultiplier;
	public final double totalObjectiveValue;
	public final boolean missionOnly;
	public final boolean isDefault;
	
	private Map<String,Renderable> renderables;
	
	public Arena(String arenaName){
		super("arenas/" + arenaName);
		this.arenaName = arenaName;
		this.shortName = arenaName.replaceAll("\\s+","").replace("_","").toLowerCase();
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
		
		int numTeams = 0;
		while (hasValue("player" + (numTeams+1) + "_x_pos") && numTeams < MAX_PLAYERS)
			numTeams++;
		teamPos = new int[numTeams][2];
		teamVel = new double[numTeams][2];
		for (int x = 0; x < teamPos.length; x++){
			teamPos[x][0] = getInt("player" + (x+1) + "_x_pos");
			teamPos[x][1] = getInt("player" + (x+1) + "_y_pos");
			teamVel[x][0] = getInt("player" + (x+1) + "_x_vel")/Main.TPS;
			teamVel[x][1] = getInt("player" + (x+1) + "_y_vel")/Main.TPS;
		}
		
		defaultBudget = getInt("default_budget");
		maxScore = getInt("max_score");
		startBudget = !hasValue("start_budget") ? 1.0 : getDouble("start_budget");
		incomeTime = getInt("income_time")*Main.TPS;
		decapMultiplier = getInt("decap_multiplier") == 0 ? 1 : getInt("decap_multiplier");
		missionOnly = getBoolean("mission_only");
		isDefault = getBoolean("default_arena");
		
		int minY = 0, maxY = 0, minX = 0, maxX = 0;
		for (ForegroundObject object : foregroundObjects){
			minY = min(minY, (int)object.getPosY());
			maxY = max(maxY, (int)object.getPosY());
			minX = min(minX, (int)object.getPosX());
			maxX = max(maxX, (int)object.getPosX());
		}
		for (int[] position : teamPos){
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
	
	public void initialize(){
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
	
	public void draw(Graphics2D g, GameWindow window, boolean isWarpGame){
		
		background.drawForegroundClouds(g, window);
		
		for (Arena.BackgroundObject object : backgroundObjects){
			g.drawImage(object.getImg(),
					object.posX-window.getPosX()/object.distance+window.windowResX/2-object.getImg().getWidth()/2,
					object.posY-window.getPosY()/object.distance+window.windowResY/2-object.getImg().getHeight()/2,
					null);
		}
		
		for (ForegroundObject object : foregroundObjects)
			object.draw(g, window);
		
		if (isWarpGame){
			g.setColor(WARP_IN_COLOR);
			int size = (int)(Ship.WARP_POS_RADIUS*window.getZoom());
			int[] position = teamPos[window.player.position];
			g.fillOval(window.posXOnScreen(position[0])-size, window.posYOnScreen(position[1])-size, 2*size, 2*size);
		}
	}
	
	/*public void drawBackground(Graphics2D g, GameWindow window){
		//BufferedImage backgroundImg = background.generateBackground();
		//g.drawImage(backgroundImg, 0, 0, null);
		//backgroundImg.flush();
		 background.drawBackground(g);
	}*/
	
	public void drawPreview(Graphics2D g, int size){
		initialize();
		double scale = size/(1.25*arenaSize);
		
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, size, size);
		
		for (ForegroundObject object : foregroundObjects){
			int posX = size/2 + (int)(scale*object.getPosX());
			int posY = size/2 + (int)(scale*object.getPosY());
			object.drawPreview(g, posX, posY);
		}
		
		for (int x = 0; x < teamPos.length; x++){
			FontMetrics metrics = g.getFontMetrics(previewFontLarge);
			String label = String.valueOf(x+1);
			int posX = size/2 + (int)(scale*teamPos[x][0]);
			int posY = size/2 + (int)(scale*teamPos[x][1]);
			g.setFont(previewFontLarge);
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
			value = getDouble("foreground_object" + index + "_obj_value");
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
			
			Image img = getImage(window.getRenderZoom());
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
			g.setFont(previewFontSmall);
			g.drawString(String.valueOf((int)round(value)), posX-PREVIEW_SIZE/5, posY+PREVIEW_SIZE);
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
	
	
}
