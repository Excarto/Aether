import static java.lang.Math.*;

import javax.swing.*;
import javax.imageio.*;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public final class GameWindow extends Window{
	public static final int MENU_WIDTH = 270, WINDOW_RES_Y = Main.RES_Y;
	public static final int DIVIDER_WIDTH = 4;
	public static final int MENU_REFRESH_PER_SEC = 8;
	public final static Font UNIT_LABEL_FONT = new Font("Arial", Font.PLAIN, 12);
	private static final Font DISPLAY_FONT = new Font("Arial", Font.BOLD, 40), CHAT_FONT = new Font("Arial", Font.PLAIN, 15);
	private static final int MAX_CHAT_LINES = 6, CHAT_LINE_SPACING = 20, CHAT_LIFETIME = 6*Main.TPS;
	
	private static Music music;
	public static double framesPerTurn;
	public static int turnsPerFrame;
	
	public final int windowResX;
	public final HumanPlayer player;
	public final int inscribeRadius;
	
	//private GLG2DCanvas GLG2DWrapper;
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
	private Queue<ChatLine> chatLines;
	private List<Controllable> frameVisibleControllables;
	private List<Sprite> frameVisibleGraphics;
	private List<SensorTarget> frameRadarVisibleSprites;
	private List<Target> frameTargets;
	private List<Controllable> frameControllables;
	private List<Controllable> frameSelected;
	private List<Beam> frameBeams;
	private int framesPerInterfaceRefresh, turnsUntilShow;
	private int frameCount;
	private CountDownLatch renderLatch;
	private boolean menuUpdatePending;
	private boolean victory, defeat;
	
	public GameWindow(HumanPlayer player, double gameSpeed, int randomSeed){
		super(false);
		this.setLayout(null);
		this.setOpaque(true);
		this.player = player;
		
		windowResX = Main.resX - MENU_WIDTH - DIVIDER_WIDTH;
		inscribeRadius = (int)hypot(windowResX/2, WINDOW_RES_Y/2);
		zoomToPosX = zoomToPosY = -1;
		selected = new ArrayList<Controllable>();
		chatLines = new ArrayDeque<ChatLine>();
		showOrders = showVision = true;
		frameVisibleControllables = new ArrayList<Controllable>();
		frameVisibleGraphics = new ArrayList<Sprite>();
		frameRadarVisibleSprites = new ArrayList<SensorTarget>();
		frameTargets = new ArrayList<Target>();
		frameControllables = new ArrayList<>();
		frameSelected = new ArrayList<Controllable>();
		frameBeams = new ArrayList<Beam>();
		frameReady = false;
		frameStarted = false;
		isTactical = false;
		turnsPerFrame = Window.getTurnsPerFrame(Main.framesPerSec, gameSpeed);
		framesPerTurn = 1.0/turnsPerFrame;
		framesPerInterfaceRefresh = (int)round(Main.framesPerSec/(double)MENU_REFRESH_PER_SEC);
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
		rightPanel.setPreferredSize(new Dimension(MENU_WIDTH, Main.RES_Y));
		defaultPanel = new DefaultPanel(player);
		
		menu = new MenuPanel();
		
		budgetLabel = new JLabel();
		budgetLabel.setPreferredSize(new Dimension(60, 18));
		budgetLabel.setHorizontalAlignment(JLabel.CENTER);
		budgetLabel.setFont(new Font("Arial", Font.BOLD, 14));
		budgetLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		
		inputHandler = new InputHandler(this, window);
		
		this.add(window);
		this.add(rightPanel);
		window.setBounds(0, 0, windowResX, WINDOW_RES_Y);
		rightPanel.setBounds(windowResX + DIVIDER_WIDTH, 0, MENU_WIDTH, Main.RES_Y);
		
		queueMenuUpdate();
		
		music.randomize(randomSeed);
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
			posY = sprite.getPosY()-WINDOW_RES_Y/2;
		}else{
			Point mousePosition = Main.getMousePosition();
			Point middleButtonPos = inputHandler.getMiddleButtonHoldPos();
			if (middleButtonPos != null && mousePosition != null){
				posX -= (mousePosition.getX() - middleButtonPos.getX())/frameZoomRatio;
				posY -= (mousePosition.getY() - middleButtonPos.getY())/frameZoomRatio;
				inputHandler.setMiddleButtonHoldPos(mousePosition);
			}
			
			double scrollAmount = Main.scrollSpeed/scrollSpeedZoomRatio;
			double accelAmount = Main.accelRate/scrollSpeedZoomRatio;
			if (inputHandler.controlPressed(Control.FUNCTION1)){
				scrollAmount *= Main.cameraMoveMultiplier;
				accelAmount *= Main.cameraMoveMultiplier;
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
		
		try{
			BufferedImage border = ImageIO.read(new File("data/divider.png"));
			g.drawImage(border, windowResX, 0, null);
			border.flush();
		}catch (IOException e){}
	}
	
	//int count; long sum; long startTime; long firstFrameTime; int firstFrame; int framesMissed;
	public void renderFrame(){
		//currentMilis = java.lang.System.nanoTime()/1000;
		
		boolean startNewFrame;
		synchronized (GameWindow.this){
			turnsUntilShow--;
			if (turnsUntilShow <= 0 && frameStarted){
				if (frameReady){
					frameStarted = false;
				}//else{
				//	framesMissed++;
				//	this.receiveChat("frame missed "+(100*1000*framesMissed/frameCount)/1000.0+"%", 0);
				//}
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
			//milisShow = java.lang.System.nanoTime()/1000 + turnsUntilShow*1000/Main.TPS;
			
			recordFrameState();
			
			final boolean frameMenuUpdate = menuUpdatePending;
			menuUpdatePending = false;
			
			renderLatch.countDown();
			renderLatch = new CountDownLatch(1);
			
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					//startTime = java.lang.System.nanoTime();
					
					if (frameMenuUpdate)
						updateMenu();
					
					try{
						if (frameCount%framesPerInterfaceRefresh == 0)
							sideMenu.refresh();
						if (frameCount%2 == 0)
							Main.paintDirtyRegions();
						
						Graphics frameGraphics = Main.getFrameGraphics();
						if (frameGraphics == null)
							return;
						
						frameGraphics.setClip(0, 0, windowResX, WINDOW_RES_Y);
						window.paintFrame(frameGraphics);
						
						frameGraphics.dispose();
					}catch(Exception ex){
						java.lang.System.err.println("Rendering Exception:");
						ex.printStackTrace();
						java.lang.System.err.println();
					}
					
					/*long time = (java.lang.System.nanoTime()-startTime);
					count++;
					sum += time;
					if (frameCount%101 == 0){
						if (firstFrameTime == 0){
							firstFrame = frameCount;
							firstFrameTime = java.lang.System.nanoTime();
						}
						java.lang.System.out.println(sum*1.0e-6/count
								+" "+(frameCount-firstFrame)*1.0e9/(java.lang.System.nanoTime()-firstFrameTime));
						sum = 0;
						count = 0;
					}*/
					
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
	
	private void recordFrameState(){
		framePosX = posX;
		framePosY = posY;
		frameZoomRatio = zoomRatio;
		frameRenderZoomRatio = pow(zoomRatio, Main.renderSizeScaling);
		
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
	
	public static void load(){
		//File[] musicFiles = new File("data/music/").listFiles();
		//music = new Sound(musicFiles[(int)(random()*musicFiles.length)]);
		//music.setFollowSound(music);
		//music.load();
		music = new Music("data/music");
		
		SidePanel.load();
	}
	
	public void initialize(){
		posX = Main.game.arena.teamPositions[player.position][0] - windowResX/2;
		posY = Main.game.arena.teamPositions[player.position][1] - WINDOW_RES_Y/2;
		
		music.setVolume(Main.musicVolume);
		music.play();
		updateBudget();
	}
	
	public void toggleMenu(){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				if (window.getComponentCount() == 0){
					window.add(menu);
					window.revalidate();
					if (Main.game.isLocal())
						Main.game.setPause(true);
				}else{
					window.removeAll();
					window.revalidate();
					Main.game.arena.drawBackground((Graphics2D)getGraphics(), GameWindow.this);
					if (Main.game.isLocal())
						Main.game.setPause(false);
				}
			}
		});
	}
	
	public int setZoomLevel(int level){
		level = max(0, min(Main.zoomLevels, level));
		zoomRatio = pow(Main.zoomRatio, level);
		scrollSpeedZoomRatio = pow(Main.moveZoomRatio, level);
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
		return (int)((gamePosY-framePosY)*frameZoomRatio+(1-frameZoomRatio)*WINDOW_RES_Y/2);
	}
	
	public int posXInGame(Point point){
		return (int)((point.getX()-(1-zoomRatio)*windowResX/2)/zoomRatio+framePosX);
	}
	public int posYInGame(Point point){
		return (int)((point.getY()-(1-zoomRatio)*WINDOW_RES_Y/2)/zoomRatio+framePosY);
	}
	
	public boolean isInWindow(Point position){
		return position != null && position.x < windowResX && position.x >= 0 &&
				position.y < WINDOW_RES_Y && position.y >= 0;
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
	
	public double renderTimeLeft(){
		//return (milisShow - currentMilis)*framePerMilis;
		return turnsUntilShow*framesPerTurn;
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
		zoomToPosY = posY-WINDOW_RES_Y/2;
		this.velX = velX;
		this.velY = velY;
	}
	
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
	
	public void drawPointerLine(Graphics g, int posX, int posY, String label){
		if (posX < 0 || posX > windowResX || posY < 0 || posY > WINDOW_RES_Y){
			double centerPosX = posX-windowResX/2;
			double centerPosY = posY-WINDOW_RES_Y/2;
			
			int startX = (int)(centerPosX*(WINDOW_RES_Y/2)/abs(0.5+centerPosY)+windowResX/2);
			startX = max(0, min(windowResX, startX));
			int startY = (int)(centerPosY*(windowResX/2)/abs(0.5+centerPosX)+WINDOW_RES_Y/2);
			startY = max(0, min(WINDOW_RES_Y, startY));
			
			double rateX = startX-windowResX/2;
			double rateY = startY-WINDOW_RES_Y/2;
			double norm = sqrt(rateX*rateX + rateY*rateY);
			rateX /= norm;
			rateY /= norm;
			
			double length = Main.maxPointerLineLength-pow(centerPosX*centerPosX+centerPosY*centerPosY, 0.3)/8;
			length = max(length, Main.maxPointerLineLength/4);
			
			double offset = label == null ? 0 : 19;
			
			g.drawLine(startX-(int)(offset*rateX), startY-(int)(offset*rateY),
					startX-(int)(length*rateX), startY-(int)(length*rateY));
			
			if (label != null){
				g.setFont(UNIT_LABEL_FONT);
				g.drawString(label, startX-(int)(10*rateX)-4*label.length(), startY-(int)(8*rateY)+4);
			}
		}
	}
	
	public void updateBudget(){
		budgetLabel.setText("$"+player.getBudget());
	}
	
	public void receiveChat(String message, int team){
		receiveMessage(message, team, CHAT_LIFETIME);
	}
	
	public void receiveMessage(String message, int team, int lifetime){
		if (team == 0 || team == player.team){
			synchronized (chatLines){
				chatLines.offer(new ChatLine(message, lifetime));
				if (chatLines.size() > MAX_CHAT_LINES)
					chatLines.poll();
			}
		}
	}
	
	public void select(Controllable controllable){
		toSelect = controllable;
	}
	
	static final int BAR_WIDTH = 70, BAR_HEIGHT = 17;
	static final int BAR_YPOS = 3, BAR_SPACING = 6;
	static final Color BAR_COLOR1_FRIENDLY = new Color(50, 200, 50, 255);
	static final Color BAR_COLOR1_HOSTILE = new Color(200, 50, 50, 255);
	static final Color BAR_COLOR2_FRIENDLY = new Color(100, 180, 100, 255);
	static final Color BAR_COLOR2_HOSTILE = new Color(180, 100, 100, 255);
	static final Font BAR_FONT = new Font("Arial", Font.BOLD, 14);
	private class ViewWindow extends JComponent{
		public ViewWindow(){
			this.setPreferredSize(new Dimension(windowResX, WINDOW_RES_Y));
			this.setOpaque(true);
		}
		
		public void paint(Graphics g){
			Main.game.arena.drawBackground((Graphics2D)g, GameWindow.this);
		}
		
		public void paintFrame(Graphics graphics){
			Graphics2D g = (Graphics2D)graphics;
			g.setRenderingHints(Main.inGameHints);
			Point mousePosition = Main.getMousePosition();
			if (!isInWindow(mousePosition))
				mousePosition = null;
			
			if (!Main.game.isRunning()){
				g.setColor(Color.BLACK);
				g.fillRect(0, 0, windowResX, WINDOW_RES_Y);
				return;
			}
			
			Main.game.arena.draw(g, GameWindow.this);
			
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
				if (!frameVisibleControllables.contains(target.sprite))
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
				g.setFont(DISPLAY_FONT);
				int posX = windowResX/2-g.getFontMetrics().stringWidth(text)/2;
				int posY = WINDOW_RES_Y/2-200;
				g.setColor(Color.BLACK);
				for (int x = -2; x <= 2; x++){
					for (int y = -2; y <= 2; y++)
						g.drawString(text, posX+x, posY+y);
				}
				g.setColor(Color.LIGHT_GRAY);
				g.drawString(text, posX, posY);
			}
			
			g.setColor(Color.WHITE);
			g.setFont(CHAT_FONT);
			int chatPosY = WINDOW_RES_Y-50;
			int chatPosX = 50;
			if (inputHandler.getChat() != null)
				g.drawString((inputHandler.isChatToAll() ? "To All:" : "To Allies:")+inputHandler.getChat(), chatPosX, chatPosY);
			
			if (!chatLines.isEmpty()){
				synchronized (chatLines){
					chatPosY -= CHAT_LINE_SPACING*chatLines.size() + 10;
					for (ChatLine line : chatLines){
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
				g.setFont(BAR_FONT);
				g.drawString("+ " + String.valueOf((int)round(Main.game.getScoreIncrement(x)*Main.TPS)),
						xStart+BAR_WIDTH/2-12, BAR_YPOS+BAR_HEIGHT/2+6);
				
				xStart += BAR_SPACING+BAR_WIDTH;
			}
			
			if (Main.game.arena.startBudget < 1.0){
				int posX = windowResX-55, posY = 15;
				g.setFont(BAR_FONT);
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
	
	static final int MENU_PANEL_WIDTH = 140, MENU_PANEL_HEIGHT = 162;
	private class MenuPanel extends JPanel{
		public MenuPanel(){
			this.setPreferredSize(new Dimension(MENU_PANEL_WIDTH, MENU_PANEL_HEIGHT));
			this.setBounds(windowResX/2-MENU_PANEL_WIDTH/2, WINDOW_RES_Y/2-MENU_PANEL_HEIGHT/2,
					MENU_PANEL_WIDTH, MENU_PANEL_HEIGHT);
			this.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 3));
			
			Dimension buttonSize = new Dimension(MENU_PANEL_WIDTH-20, 25);
			
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
			
			JButton leaveButton = new JButton("Leave Game");
			leaveButton.setPreferredSize(buttonSize);
			leaveButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					Main.game.stop();
					Main.removeWindow();
			}});
			
			JButton exitButton = new JButton("Exit Aether");
			exitButton.setPreferredSize(buttonSize);
			exitButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					Main.exit();
			}});
			
			JButton resumeButton = new JButton("Resume");
			resumeButton.setPreferredSize(buttonSize);
			resumeButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					toggleMenu();
			}});
			
			this.add(menuLabel);
			this.add(exitButton);
			this.add(leaveButton);
			this.add(tutorialButton);
			this.add(resumeButton);
		}
	}
	
	static final int TUTORIAL_WIDTH = 550, TUTORIAL_HEIGHT = 700;
	private class TutorialPanel extends JScrollPane{
		public TutorialPanel(){
			super(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			this.setPreferredSize(new Dimension(TUTORIAL_WIDTH, TUTORIAL_HEIGHT));
			this.setBounds(max(MENU_PANEL_WIDTH, (windowResX-TUTORIAL_WIDTH)/2),
					(WINDOW_RES_Y-TUTORIAL_HEIGHT)/2, TUTORIAL_WIDTH, TUTORIAL_HEIGHT);
			this.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 3));
			
			File file = new File("data/tutorial.txt");
			char[] buffer = new char[(int)file.length()];
			try{
				FileReader input = new FileReader(file);
				input.read(buffer);
				input.close();
			}catch (Exception e){}
			
			JLabel label = new JLabel();
			label.setFont(new Font("Arial", Font.PLAIN, 12));
			label.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
			label.setHorizontalAlignment(JLabel.LEFT);
			label.setPreferredSize(new Dimension(TUTORIAL_WIDTH-30, 1160));
			label.setText(new String(buffer));
			this.setViewportView(label);
		}
	}
	
	private class ChatLine{
		public String string;
		public int age, maxAge;
		public ChatLine(String string, int maxAge){
			this.string = string;
			this.maxAge = maxAge;
		}
	}
	
}
