import static java.lang.Math.*;
import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

// The in-game window. This implements rendering in a separate thread that is synchronized with the main game thread

public final class GameWindow extends Window{
	public static final int MENU_WIDTH = 270;
	public static final int DIVIDER_WIDTH = 4;
	public static final int MENU_REFRESH_PER_SEC = 8;
	private static final int MAX_CHAT_LINES = 6, CHAT_LINE_SPACING = 20, CHAT_LIFETIME = 6*Main.TPS;
	
	private static final double FRAMERATE_ADJUST_RATE = 0.008;
	private static final double FRAMERATE_INCREASE_THRESH = 0.98;
	private static final double FRAMERATE_DECREASE_THRESH = 0.80;
	
	public static Font unitLabelFont;
	public static Font displayFont;
	public static Font chatFont, missionChatFont;
	
	private static Music music;
	private static Sound notification;
	
	public final int windowResX, windowResY;
	public final HumanPlayer player;
	public final int inscribeRadius;
	
	private ViewWindow window;
	private MenuPanel menu;
	private TutorialPanel tutorial;
	private JPanel rightPanel;
	private DefaultPanel defaultPanel;
	private SidePanel sideMenu;
	private boolean showOrders, showVision;
	private JLabel budgetLabel;
	private boolean isTactical;
	private int multiPosOrderDist;
	private double zoomRatio, frameZoomRatio, frameRenderZoomRatio, scrollSpeedZoomRatio;
	private double posX, posY, framePosX, framePosY;
	private double velX, velY;
	private List<Controllable> selected;
	private Controllable toSelect;
	private double zoomToPosX, zoomToPosY;
	private InputHandler inputHandler;
	private boolean frameStarted, frameReady;
	private boolean hasMenu;
	private ArrayDeque<ChatLine> chatLines;
	
	// State of game at start of frame is recorded in these variables so that the rendering
	// thread can work independently of the game thread
	private List<Controllable> frameVisibleControllables;
	private List<Sprite> frameVisibleGraphics;
	private List<SensorTarget> frameRadarVisibleSprites;
	private List<Target> frameTargets;
	private List<Controllable> frameControllables;
	private List<Controllable> frameSelected;
	private List<Beam> frameBeams;
	
	private int turnsPerFrame;
	private int minTurnsPerFrame, maxTurnsPerFrame;
	private int framesPerInterfaceRefresh, turnsUntilShow;
	private int frameCount;
	private double frameHitRate;
	private CountDownLatch renderLatch;
	private boolean menuUpdatePending;
	private boolean victory, defeat;
	private boolean drawGrid, menuDisabled;
	
	public GameWindow(HumanPlayer player, double gameSpeed, int randomSeed){
		super(Size.FULL);
		this.setLayout(null);
		this.setOpaque(true);
		this.player = player;
		
		windowResX = Main.resX - MENU_WIDTH - DIVIDER_WIDTH;
		windowResY = Main.resY;
		inscribeRadius = (int)hypot(windowResX/2, windowResY/2);
		zoomToPosX = zoomToPosY = -1;
		selected = new ArrayList<Controllable>();
		chatLines = new ArrayDeque<ChatLine>();
		showOrders = showVision = true;
		frameVisibleControllables = new ArrayList<Controllable>();
		frameVisibleGraphics = new ArrayList<Sprite>();
		frameRadarVisibleSprites = new ArrayList<SensorTarget>();
		frameTargets = new ArrayList<Target>();
		frameControllables = new ArrayList<Controllable>();
		frameSelected = new ArrayList<Controllable>();
		frameBeams = new ArrayList<Beam>();
		frameReady = false;
		frameStarted = false;
		isTactical = false;
		drawGrid = false;
		menuDisabled = false;
		minTurnsPerFrame = Window.getTurnsPerFrame(Main.options.framesPerSec, gameSpeed);
		maxTurnsPerFrame = minTurnsPerFrame+1;
		turnsPerFrame = minTurnsPerFrame;
		frameHitRate = 1.0;
		framesPerInterfaceRefresh = (int)round(Main.options.framesPerSec/(double)MENU_REFRESH_PER_SEC);
		renderLatch = new CountDownLatch(1);
		
		Unit.createMenu();
		Ship.createMenu();
		Missile.createMenu();
		Weapon.createMenu();
		Sensor.createMenu();
		Detector.createMenu();
		Cloak.createMenu();
		Scanner.createMenu();
		Booster.createMenu();
		
		window = new ViewWindow();
		
		rightPanel = new JPanel(NO_BORDER);
		rightPanel.setPreferredSize(new Dimension(MENU_WIDTH, Main.resY));
		defaultPanel = new DefaultPanel(player);
		
		menu = new MenuPanel();
		
		budgetLabel = new JLabel();
		budgetLabel.setPreferredSize(new Dimension(60, 18));
		budgetLabel.setHorizontalAlignment(JLabel.CENTER);
		budgetLabel.setFont(Main.getDefaultFont(14));
		budgetLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		
		inputHandler = new InputHandler(this, window);
		
		this.add(window);
		this.add(rightPanel);
		window.setBounds(0, 0, windowResX, windowResY);
		rightPanel.setBounds(windowResX+DIVIDER_WIDTH, 0, MENU_WIDTH, Main.resY);
		
		queueMenuUpdate();
		
		music.randomize(randomSeed, Main.game.mission == null ? null : Main.game.mission.musicName);
	}
	
	public void move(){
		posX += velX;
		posY += velY;
	}
	
	public void act(){
		inputHandler.processInput(selected);
		
		if (toSelect != null){
			if (player.controllables.contains(toSelect)){
				if (inputHandler.controlPressed(Control.FUNCTION1)){
					if (!selected.remove(toSelect))
						selected.add(toSelect);
				}else{
					selected.clear();
					selected.add(toSelect);
					queueMenuUpdate();
				}
			}
			toSelect = null;
		}
		
		for (ChatLine line : chatLines)
			line.age++;
		if (!chatLines.isEmpty() && chatLines.peek().age > chatLines.peek().maxAge)
			chatLines.poll();
		
		if (!isStrategic() && selected.size() == 1){
			Controllable controllable = selected.get(0);
			Sprite sprite = (Sprite)controllable;
			
			if (!Main.game.isSuspended()){
				if (inputHandler.controlPressed(Control.SCROLL_UP))
					controllable.accelForward();
				if (inputHandler.controlPressed(Control.SCROLL_LEFT))
					controllable.accelTurn(false);
				if (inputHandler.controlPressed(Control.SCROLL_RIGHT))
					controllable.accelTurn(true);
				if (inputHandler.controlPressed(Control.SCROLL_DOWN))
					controllable.stopTurn();
			}
			
			velX = sprite.getVelX();
			velY = sprite.getVelY();
		
			posX = sprite.getPosX()-windowResX/2;
			posY = sprite.getPosY()-windowResY/2;
		}else{
			Point mousePosition = Main.getMousePosition();
			Point middleButtonPos = inputHandler.getMiddleButtonHoldPos();
			if (middleButtonPos != null && mousePosition != null){
				posX -= (mousePosition.getX() - middleButtonPos.getX())/frameZoomRatio;
				posY -= (mousePosition.getY() - middleButtonPos.getY())/frameZoomRatio;
				inputHandler.setMiddleButtonHoldPos(mousePosition);
			}
			
			double scrollAmount = Main.options.scrollSpeed/scrollSpeedZoomRatio;
			double accelAmount = Main.options.accelRate/scrollSpeedZoomRatio;
			if (inputHandler.controlPressed(Control.FUNCTION1)){
				scrollAmount *= Main.options.cameraMoveMultiplier;
				accelAmount *= Main.options.cameraMoveMultiplier;
			}
			
			if (zoomToPosX != -1){
				zoomToPosX += velX;
				zoomToPosY += velY;
				posX += (zoomToPosX-posX)/5;
				posY += (zoomToPosY-posY)/5;
				if (abs(posX-zoomToPosX) < 10 && abs(posY-zoomToPosY) < 10)
					zoomToPosX = -1;
			}else{
				if (inputHandler.controlPressed(Control.SCROLL_UP))
					posY -= scrollAmount;
				if (inputHandler.controlPressed(Control.SCROLL_DOWN))
					posY += scrollAmount;
				if (inputHandler.controlPressed(Control.SCROLL_LEFT))
					posX -= scrollAmount;
				if (inputHandler.controlPressed(Control.SCROLL_RIGHT))
					posX += scrollAmount;
			}
			
			if (inputHandler.controlPressed(Control.ACCEL_UP))
				velY -= accelAmount;
			if (inputHandler.controlPressed(Control.ACCEL_DOWN))
				velY += accelAmount;
			if (inputHandler.controlPressed(Control.ACCEL_LEFT))
				velX -= accelAmount;
			if (inputHandler.controlPressed(Control.ACCEL_RIGHT))
				velX += accelAmount;
		}
	}
	
	public void paint(Graphics g){
		super.paint(g);
		
		g.drawImage(dividerImg, windowResX, 0, null);
	}
	
	// Trigger the next frame to show as soon as it's ready and the turnsUntilShow counter has reached zero.
	// If the frame has completed rendering on time, then begin rendering the next one. 
	public void renderFrame(){
		
		boolean startNewFrame;
		synchronized (GameWindow.this){
			turnsUntilShow--;
			if (turnsUntilShow <= 0 && frameStarted){
				if (frameReady){
					frameStarted = false;
				}else{
					if (Main.DEBUG)
						this.receiveMessage("frame missed " + turnsPerFrame + " " + Main.game.turn, 0, Main.TPS);
				}
				updateTurnsPerFrame(frameReady);
				turnsUntilShow = turnsPerFrame;
				renderLatch.countDown();
			}
			startNewFrame = !frameStarted;
			
			if (startNewFrame){
				frameReady = false;
				frameStarted = true;
			}
		}
		
		if (startNewFrame){
			frameCount++;
			
			recordFrameState();
			
			final boolean frameMenuUpdate = menuUpdatePending;
			menuUpdatePending = false;
			
			renderLatch.countDown();
			renderLatch = new CountDownLatch(1);
			
			// Begin new Swing task that will render the frame to the buffer, then wait for renderLatch to trigger
			// before showing it.
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					
					if (frameMenuUpdate)
						updateMenu();
					
					try{
						if (frameCount%framesPerInterfaceRefresh == 0)
							sideMenu.refresh();
						if (frameCount%2 == 0)
							Main.paintDirtyRegions();
						
						Graphics2D frameGraphics = Main.getFrameGraphics();
						if (frameGraphics == null)
							return;
						
						frameGraphics.setClip(0, 0, windowResX, windowResY);
						window.paintFrame(frameGraphics);
						
						frameGraphics.dispose();
					}catch(Exception ex){
						java.lang.System.err.println("Rendering Exception:");
						ex.printStackTrace();
						java.lang.System.err.println();
					}
					
					frameReady = true;
					try{
						renderLatch.await();
					}catch (InterruptedException e){}
					synchronized (GameWindow.this){
						if (frameReady)
							frameStarted = false;
					}
					
					Main.showBuffer();
					Renderable.flushTemp();
				}
			});
			
		}
	}
	
	public void queueMenuUpdate(){
		menuUpdatePending = true;
	}
	
	private void updateMenu(){
		rightPanel.removeAll();
		if (frameSelected.size() == 1){
			sideMenu = frameSelected.get(0).getMenu();
			if (hasMenu)
				frameSelected.get(0).restoreMenuState();
			hasMenu = true;
		}else{
			hasMenu = false;
			sideMenu = defaultPanel;
		}
		rightPanel.add(sideMenu);
		rightPanel.revalidate();
		rightPanel.repaint();
	}
	
	// Save off the game state so that the rendering thread can work seprarately from the game thread
	private void recordFrameState(){
		framePosX = posX;
		framePosY = posY;
		frameZoomRatio = zoomRatio;
		frameRenderZoomRatio = pow(zoomRatio, Main.options.renderSizeScaling);
		
		for (Arena.ForegroundObject object : Main.game.arena.foregroundObjects)
			object.recordPos();
		frameVisibleControllables.clear();
		frameVisibleControllables.addAll(player.visibleControllables);
		for (Controllable controllable : frameVisibleControllables)
			((Sprite)controllable).recordPos();
		frameVisibleGraphics.clear();
		frameVisibleGraphics.addAll(player.visibleGraphics);
		for (Sprite sprite : frameVisibleGraphics)
			sprite.recordPos();
		frameRadarVisibleSprites.clear();
		frameRadarVisibleSprites.addAll(player.getSensorTargets());
		for (SensorTarget target : frameRadarVisibleSprites)
			target.recordPos();
		frameTargets.clear();
		frameTargets.addAll(player.getTargets());
		for (Target target : frameTargets)
			target.recordPos();
		frameControllables.clear();
		frameControllables.addAll(player.controllables);
		frameBeams.clear();
		for (Player player : Main.game.players){
			for (Beam firingBeam : player.firingBeams){
				if (this.player.visibleControllables.contains(firingBeam.unit))
					frameBeams.add(firingBeam);
			}
		}
		frameSelected.clear();
		frameSelected.addAll(selected);
	}
	
	// Adjust how many game turns per thread to maintain a smooth frame rate
	private void updateTurnsPerFrame(boolean frameHit){
		frameHitRate = frameHitRate*(1 - FRAMERATE_ADJUST_RATE) + (frameHit ? 1.0 : 0.0)*FRAMERATE_ADJUST_RATE;
		if (frameHitRate > FRAMERATE_INCREASE_THRESH && turnsPerFrame > minTurnsPerFrame){
			turnsPerFrame--;
			frameHitRate = (FRAMERATE_INCREASE_THRESH + FRAMERATE_DECREASE_THRESH)/2;
		}else if (frameHitRate < FRAMERATE_DECREASE_THRESH && turnsPerFrame < maxTurnsPerFrame){
			turnsPerFrame++;
			frameHitRate = (FRAMERATE_INCREASE_THRESH + FRAMERATE_DECREASE_THRESH)/2;
		}
	}
	
	public void exit(){
		Main.setDispatcherEnabled(false);
		Main.setHighPerformance(false);
		Sound.stopAll();
	}
	public void resume(){
		Main.setDispatcherEnabled(true);
		Main.setHighPerformance(true);
	}
	public void suspend(){}
	
	protected String getBackgroundFile(){
		return null;
	}
	
	public void initialize(){
		posX = Main.game.arena.teamPos[player.position][0] - windowResX/2;
		posY = Main.game.arena.teamPos[player.position][1] - windowResY/2;
		velX = Main.game.arena.teamVel[player.position][0];
		velY = Main.game.arena.teamVel[player.position][1];
		
		music.setVolume(Main.options.musicVolume);
		music.play();
		updateBudget();
	}
	
	public void setMenuDisabled(boolean menuDisabled){
		this.menuDisabled = menuDisabled;
	}
	
	public void toggleMenu(){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				if (window.getComponentCount() == 0 && !menuDisabled){
					window.add(menu);
					window.revalidate();
					if (Main.game.isLocal())
						Main.game.setPause(true);
				}else{
					window.removeAll();
					window.revalidate();
					drawBackground((Graphics2D)getGraphics());
					if (Main.game.isLocal())
						Main.game.setPause(false);
				}
			}
		});
	}
	
	public int setZoomLevel(int level){
		level = max(0, min(Main.options.zoomLevels, level));
		zoomRatio = pow(Main.options.zoomRatio, level);
		scrollSpeedZoomRatio = pow(Main.options.moveZoomRatio, level);
		multiPosOrderDist = 160+(int)(20/zoomRatio);
		return level;
	}
	
	public void removeControllable(Controllable controllable){
		if (controllable.getPlayer() == player){
			if (selected.contains(controllable)){
				selected.remove(controllable);
				queueMenuUpdate();
			}
		}
		inputHandler.removeControllable(controllable);
	}
	
	public void triggerVictory(){
		victory = true;
	}
	public void triggerDefeat(){
		defeat = true;
	}
	
	public InputHandler getInputHandler(){
		return inputHandler;
	}
	
	public boolean isSelected(Controllable controllable){
		return selected.contains(controllable);
	}
	
	public int posXOnScreen(Sprite sprite){
		return posXOnScreen(sprite.getRenderPosX());
	}
	public int posYOnScreen(Sprite sprite){
		return posYOnScreen(sprite.getRenderPosY());
	}
	
	public int posXOnScreen(double gamePosX){
		return (int)((gamePosX-framePosX)*frameZoomRatio+(1-frameZoomRatio)*windowResX/2);
	}
	public int posYOnScreen(double gamePosY){
		return (int)((gamePosY-framePosY)*frameZoomRatio+(1-frameZoomRatio)*windowResY/2);
	}
	
	public int posXInGame(Point point){
		return (int)((point.getX()-(1-zoomRatio)*windowResX/2)/zoomRatio+framePosX);
	}
	public int posYInGame(Point point){
		return (int)((point.getY()-(1-zoomRatio)*windowResY/2)/zoomRatio+framePosY);
	}
	
	public boolean isInWindow(Point position){
		return position != null && isInWindow(position.x, position.y);
	}
	public boolean isInWindow(int posX, int posY){
		return posX < windowResX && posX >= 0 && posY < windowResY && posY >= 0;
	}
	
	public int getMousePosX(){
		Point point = Main.getMousePosition();
		return isInWindow(point) ? posXInGame(point) : 0;
	}
	public int getMousePosY(){
		Point point = Main.getMousePosition();
		return isInWindow(point) ? posYInGame(point) : 0;
	}
	
	public int getPosX(){
		return (int)framePosX;
	}
	public int getPosY(){
		return (int)framePosY;
	}
	
	public void setPosX(double posX){
		this.posX = posX;
	}
	public void setPosY(double posY){
		this.posY = posY;
	}
	
	public double getVelX(){
		return velX;
	}
	public double getVelY(){
		return velY;
	}
	
	public double getMultiPosOrderDist(){
		return multiPosOrderDist;
	}
	
	public double getZoom(){
		return frameZoomRatio;
	}
	
	public double getRenderZoom(){
		return frameRenderZoomRatio;
	}
	
	public Player getPlayer(){
		return player;
	}
	
	public boolean isStrategic(){
		return !isTactical;
	}
	
	public int getFrameCount(){
		return frameCount;
	}
	
	public void setStrategic(boolean strategic){
		isTactical = !strategic;
	}
	
	public void cancelZoomTo(){
		zoomToPosX = -1;
		zoomToPosY = -1;
	}
	
	public void zoomTo(double posX, double posY, double velX, double velY){
		zoomToPosX = posX-windowResX/2;
		zoomToPosY = posY-windowResY/2;
		this.velX = velX;
		this.velY = velY;
	}
	
	// Set camera to center of mass of the given objects
	public void matchCamera(List<Controllable> controllables){
		if (!controllables.isEmpty()){
			double totalPosX = 0, totalPosY = 0;
			double totalVelX = 0, totalVelY = 0;
			for (Controllable controllable : controllables){
				totalPosX += ((Sprite)controllable).getPosX();
				totalPosY += ((Sprite)controllable).getPosY();
				totalVelX += ((Sprite)controllable).getVelX();
				totalVelY += ((Sprite)controllable).getVelY();
			}
			
			zoomTo(totalPosX/controllables.size(), totalPosY/controllables.size(),
					totalVelX/controllables.size(), totalVelY/controllables.size());
		}
	}
	
	// Draw line on the edge of the screen pointing to something off-screen
	public void drawPointerLine(Graphics g, int posX, int posY, String label){
		if (posX < 0 || posX > windowResX || posY < 0 || posY > windowResY){
			double centerPosX = posX-windowResX/2;
			double centerPosY = posY-windowResY/2;
			
			int startX = (int)(centerPosX*(windowResY/2)/abs(0.5+centerPosY)+windowResX/2);
			startX = max(0, min(windowResX, startX));
			int startY = (int)(centerPosY*(windowResX/2)/abs(0.5+centerPosX)+windowResY/2);
			startY = max(0, min(windowResY, startY));
			
			double rateX = startX-windowResX/2;
			double rateY = startY-windowResY/2;
			double normInv = 1.0/sqrt(rateX*rateX + rateY*rateY);
			rateX *= normInv;
			rateY *= normInv;
			
			double offset = label == null ? 0 : 20;
			double length = Main.options.maxPointerLineLength - 0.1*pow(centerPosX*centerPosX+centerPosY*centerPosY, 0.3);
			length = max(length, offset+Main.options.maxPointerLineLength/4);
			
			g.drawLine(startX-(int)(offset*rateX), startY-(int)(offset*rateY),
					startX-(int)(length*rateX), startY-(int)(length*rateY));
			
			if (label != null){
				g.setFont(unitLabelFont);
				g.drawString(label, startX-(int)(12*rateX)-4*label.length(), startY-(int)(10*rateY)+4);
			}
		}
	}
	
	public void drawBackground(Graphics2D g){
		g.setClip(0, 0, Main.resX-MENU_WIDTH-DIVIDER_WIDTH, Main.resY);
		if (Main.game.mission == null){
			Arena.background.drawBackground(g, this);
		}else
			Main.game.mission.drawBackground(g, this);
	}
	
	public void updateBudget(){
		budgetLabel.setText("$"+player.getBudget());
	}
	
	public void setGridEnabled(boolean drawGrid){
		this.drawGrid = drawGrid;
	}
	
	public void receiveChat(String message, int team){
		receiveMessage(message, team, CHAT_LIFETIME);
	}
	
	public void receiveMessage(String message, int team, int lifetime){
		if (team == 0 || team == player.team){
			synchronized (chatLines){
				if (chatLines.isEmpty() || chatLines.peekLast().age > 10)
					notification.play();
				chatLines.offer(new ChatLine(message, lifetime));
				if (chatLines.size() > MAX_CHAT_LINES)
					chatLines.poll();
			}
		}
	}
	
	public void select(Controllable controllable){
		toSelect = controllable;
	}
	
	public boolean isOptimizedDrawingEnabled(){
		return true;
	}
	
	public static void load(){
		notification = new Sound(new File("data/notification.wav"));
		notification.load();
		music = new Music("data/music", "data/music/mission");
		SidePanel.load();
	}
	
	// This is the main game rendering window
	static final int BAR_WIDTH = 70, BAR_HEIGHT = 17;
	static final int BAR_YPOS = 3, BAR_SPACING = 6;
	static final int GRID_SPACING = 40;
	static final Color BAR_COLOR1_FRIENDLY = new Color(50, 200, 50, 255);
	static final Color BAR_COLOR1_HOSTILE = new Color(200, 50, 50, 255);
	static final Color BAR_COLOR2_FRIENDLY = new Color(100, 180, 100, 255);
	static final Color BAR_COLOR2_HOSTILE = new Color(180, 100, 100, 255);
	static final Color GRID_COLOR1 = new Color(0, 200, 0, 12);
	static final Color GRID_COLOR2 = new Color(0, 200, 0, 22);
	private class ViewWindow extends JComponent{
		Font barFont;
		
		public ViewWindow(){
			this.setPreferredSize(new Dimension(windowResX, windowResY));
			this.setOpaque(true);
			barFont = Main.getDefaultFont(14);
		}
		
		public void paint(Graphics g){
			drawBackground((Graphics2D)g);
		}
		
		public void paintFrame(Graphics graphics){
			Graphics2D g = (Graphics2D)graphics;
			g.setRenderingHints(Main.inGameHints);
			
			boolean isWarpGame = Main.game.arena.startBudget < 1.0 && player.totalBudget > 0;
			
			Point mousePosition = Main.getMousePosition();
			if (!isInWindow(mousePosition))
				mousePosition = null;
			
			if (!Main.game.isRunning()){
				g.setColor(Color.BLACK);
				g.fillRect(0, 0, windowResX, windowResY);
				return;
			}
			
			Main.game.arena.draw(g, GameWindow.this, isWarpGame);
			
			if (drawGrid){
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
				Random rand = new Random(Main.game.turn);
				
				int ngridX = windowResX/GRID_SPACING;
				int posX = (windowResX - ngridX*GRID_SPACING)/2;
				for (int x = 0; x < ngridX; x++){
					g.setColor(rand.nextBoolean() ? GRID_COLOR1 : GRID_COLOR2);
					g.drawLine(posX, 0, posX, windowResY);
					posX += GRID_SPACING;
				}
				
				int ngridY = windowResY/GRID_SPACING;
				int posY = (windowResY - ngridY*GRID_SPACING)/2;
				for (int y = 0; y < ngridY; y++){
					g.setColor(rand.nextBoolean() ? GRID_COLOR1 : GRID_COLOR2);
					g.drawLine(0, posY, windowResX, posY);
					posY += GRID_SPACING;
				}
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, Main.inGameHints.get(RenderingHints.KEY_ANTIALIASING));
			}
			
			for (Controllable controllable : frameControllables){
				if (inputHandler.controlPressed(Control.FUNCTION2) || frameSelected.contains(controllable) ||
						(showOrders && controllable instanceof Unit))
					controllable.drawOrders(g, GameWindow.this);
				if (showVision)
					controllable.drawVision(g, GameWindow.this);
			}
			
			if (Main.DEBUG){
				for (Player player : Main.game.players){
					if (player instanceof ComputerPlayer){
						for (Controllable controllable : player.controllables){
							if (controllable instanceof Unit)
								controllable.drawOrders(g, GameWindow.this);
						}
					}
				}
			}
			
			for (Controllable controllable : frameVisibleControllables){
				if (frameSelected.contains(controllable))
					controllable.drawHudBottom(g, GameWindow.this);
				((Sprite)controllable).draw(g, GameWindow.this);
			}
			
			for (Beam firingBeam : frameBeams)
				firingBeam.draw(g, GameWindow.this);
			
			for (Sprite sprite : frameVisibleGraphics){
				if (!(sprite instanceof Explosion))
					sprite.draw(g, GameWindow.this);
			}
			for (Sprite sprite : frameVisibleGraphics){
				if (sprite instanceof Explosion)
					sprite.draw(g, GameWindow.this);
			}
			
			for (Target target : frameTargets)
				target.draw(g, GameWindow.this);
			
			for (SensorTarget target : frameRadarVisibleSprites){
				if (!frameVisibleControllables.contains((Controllable)target.sprite))
					target.draw(g, GameWindow.this);
			}
			
			for (Controllable controllable : frameSelected)
				controllable.drawHudTop(g, GameWindow.this);
			
			Point leftClickStartPos = inputHandler.getleftClickStartPos();
			if (leftClickStartPos != null && mousePosition != null){
				g.setColor(Color.WHITE);
				g.drawRect(min(mousePosition.x, leftClickStartPos.x),
						min(mousePosition.y, leftClickStartPos.y),
						max(mousePosition.x, leftClickStartPos.x) - min(mousePosition.x, leftClickStartPos.x),
						max(mousePosition.y, leftClickStartPos.y) - min(mousePosition.y, leftClickStartPos.y));
			}
			
			Locatable rightClickStart = inputHandler.getRightClickStart();
			if (mousePosition != null && rightClickStart != null){
				int posX = posXOnScreen(rightClickStart.getPosX());
				int posY = posYOnScreen(rightClickStart.getPosY());
				if (hypot(posX-mousePosition.x, posY-mousePosition.y)/frameZoomRatio > multiPosOrderDist){
					g.setColor(Color.WHITE);
					g.drawLine(posX, posY, mousePosition.x, mousePosition.y);
				}
			}
			
			String text = null;
			if (victory){
				text = "VICTORY";
			}else if (defeat){
				text = "DEFEAT";
			}else if (Main.game.turnsToStart() > 0){
				text = String.valueOf(1+Main.game.turnsToStart()/Main.TPS);
			}
			if (text != null){
				g.setFont(displayFont);
				Utility.drawOutlinedText(g, text, windowResX/2, windowResY/2-200, Color.LIGHT_GRAY, Color.BLACK);
			}
			
			g.setColor(Color.WHITE);
			g.setFont(Main.game.mission == null ? chatFont : missionChatFont);
			
			int chatPosY = windowResY-50;
			int chatPosX = 50;
			if (inputHandler.getChat() != null)
				g.drawString((inputHandler.isChatToAll() ? "To All:" : "To Allies:")+inputHandler.getChat(), chatPosX, chatPosY);
			
			if (!chatLines.isEmpty()){
				synchronized (chatLines){
					chatPosY -= CHAT_LINE_SPACING*chatLines.size() + 10;
					for (ChatLine line : chatLines){
						int opacity = line.opacity();
						g.setColor(opacity >= 255 ? Color.WHITE : new Color(255, 255, 255, opacity));
						g.drawString(line.string, chatPosX, chatPosY);
						chatPosY += CHAT_LINE_SPACING;
					}
				}
			}
			
			int xStart = BAR_SPACING-1;
			for (int x = 1; x <= Main.game.numTeams; x++){
				g.setColor(Color.LIGHT_GRAY);
				g.fillRect(xStart, BAR_YPOS, BAR_WIDTH, BAR_HEIGHT);
				if (player.team == x){
					g.setColor(BAR_COLOR2_FRIENDLY);
				}else
					g.setColor(BAR_COLOR2_HOSTILE);
				g.fillRect(xStart+1, BAR_YPOS+1,
						(int)((BAR_WIDTH)*min(1, Main.game.getScore(x)/Main.game.arena.maxScore)), BAR_HEIGHT-1);
				if (player.team == x){
					g.setColor(BAR_COLOR1_FRIENDLY);
				}else
					g.setColor(BAR_COLOR1_HOSTILE);
				g.drawRect(xStart, BAR_YPOS, BAR_WIDTH, BAR_HEIGHT);
				
				g.setColor(Color.BLACK);
				g.setFont(barFont);
				g.drawString("+ " + String.valueOf((int)round(Main.game.getScoreIncrement(x)*Main.TPS)),
						xStart+BAR_WIDTH/2-13, BAR_YPOS+BAR_HEIGHT/2+6);
				
				xStart += BAR_SPACING+BAR_WIDTH;
			}
			
			if (isWarpGame){
				int posX = windowResX-55, posY = 15;
				g.setFont(barFont);
				g.setColor(Color.BLACK);
				String string = "$" + player.getBudget();
				g.drawString(string, posX-1, posY);
				g.drawString(string, posX+1, posY);
				g.drawString(string, posX, posY-1);
				g.drawString(string, posX, posY+1);
				g.setColor(Color.WHITE);
				g.drawString(string, posX, posY);
			}
			
			if (getComponentCount() > 0)
				super.paint(g);
		}
	}
	
	// Popup window when escape key is pressed
	static final int MENU_PANEL_WIDTH = 145, MENU_PANEL_HEIGHT = 162;
	private class MenuPanel extends JPanel{
		public MenuPanel(){
			Mission mission = Main.game.mission;
			
			int height = MENU_PANEL_HEIGHT;
			if (mission != null)
				height += 29;
			this.setPreferredSize(new Dimension(MENU_PANEL_WIDTH, height));
			this.setBounds(windowResX/2-MENU_PANEL_WIDTH/2, windowResY/2-height/2,
					MENU_PANEL_WIDTH, height);
			this.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 3));
			
			Dimension buttonSize = new Dimension(MENU_PANEL_WIDTH-15, 25);
			
			JLabel menuLabel = new JLabel();
			menuLabel.setPreferredSize(buttonSize);
			menuLabel.setHorizontalAlignment(JLabel.CENTER);
			menuLabel.setText(Main.game.isLocal() ? "PAUSED" : "MENU");
			
			JButton tutorialButton = new JButton("Help");
			tutorialButton.setPreferredSize(buttonSize);
			tutorialButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					if (tutorial == null)
						tutorial = new TutorialPanel();
					window.add(tutorial, 0);
					window.revalidate();
			}});
			
			JButton exitButton = new JButton("Exit Aether");
			exitButton.setPreferredSize(buttonSize);
			exitButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					Main.exit();
			}});
			
			JButton restartButton = null;
			if (mission != null){
				restartButton = new JButton("Restart Mission");
				restartButton.setPreferredSize(buttonSize);
				restartButton.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent e){
						Main.game.stop();
						Main.removeWindow();
						mission.start(mission.getPilot());
				}});
			}
			
			JButton leaveButton = new JButton(Main.game.mission == null ? "Leave Game" : mission.getEndString());
			leaveButton.setPreferredSize(buttonSize);
			leaveButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					Main.game.stop();
					Main.removeWindow();
			}});
			
			JButton resumeButton = new JButton("Resume");
			resumeButton.setPreferredSize(buttonSize);
			resumeButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					toggleMenu();
			}});
			
			this.add(menuLabel);
			this.add(exitButton);
			if (restartButton != null)
				this.add(restartButton);
			this.add(leaveButton);
			this.add(tutorialButton);
			this.add(resumeButton);
		}
	}
	
	static final int TUTORIAL_WIDTH = 550, TUTORIAL_PANEL_HEIGHT = 800;
	static final int TUTORIAL_HEIGHT = 1530;
	private class TutorialPanel extends JScrollPane{
		public TutorialPanel(){
			super(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			this.setPreferredSize(new Dimension(TUTORIAL_WIDTH, TUTORIAL_PANEL_HEIGHT));
			this.setBounds(max(MENU_PANEL_WIDTH, (windowResX-TUTORIAL_WIDTH)/2),
					(windowResY-TUTORIAL_PANEL_HEIGHT)/2, TUTORIAL_WIDTH, TUTORIAL_PANEL_HEIGHT);
			this.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 3));
			
			File file = new File("data/tutorial.txt");
			char[] buffer = new char[(int)file.length()];
			try{
				FileReader input = new FileReader(file);
				input.read(buffer);
				input.close();
			}catch (Exception e){}
			
			JLabel label = new JLabel();
			label.setFont(Main.getPlainFont(12));
			label.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
			label.setHorizontalAlignment(JLabel.LEFT);
			label.setPreferredSize(new Dimension(TUTORIAL_WIDTH-30, TUTORIAL_HEIGHT));
			label.setText(new String(buffer));
			this.setViewportView(label);
		}
	}
	
	static final int FADE_TIME = Main.TPS/3;
	private class ChatLine{
		public String string;
		public int age, maxAge;
		public ChatLine(String string, int maxAge){
			this.string = "> " + string;
			this.maxAge = maxAge;
			age = 0;
		}
		
		public int opacity(){
			int time = maxAge - age;
			return time > FADE_TIME ? 255 : max(0, time*255/FADE_TIME);
		}
	}
	
}
