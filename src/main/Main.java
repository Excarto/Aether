import static java.lang.Math.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.*;
import javax.swing.*;
import javax.swing.plaf.*;
import org.imgscalr.*;
import com.pagosoft.plaf.*;
import com.pagosoft.plaf.themes.*;

public final class Main{
	public static final int VERSION = 40, SUBVERSION = 0;
	public static final int RES_X_NARROW = 1024, RES_Y_SHORT = 900, RES_Y_TALL = 1080;
	public static final int BIT_DEPTH = 32;
	public static final int TPS = 108;
	public static final int MIN_MEMORY = 900+Renderable.MIN_CACHE_MEMORY;
	public static final boolean DEBUG = false;
	
	static final String OS_NAME = java.lang.System.getProperty("os.name").toLowerCase();
	static final boolean IS_WINDOWS = OS_NAME.contains("win");
	static final boolean IS_MAC = OS_NAME.contains("mac");
	static final boolean IS_NIX = OS_NAME.contains("nix") || OS_NAME.contains("nux") || OS_NAME.contains("aix");
	
	public static Options options; // User-configurable options
	public static Configuration config; // Non-unser configurable options read in from file
	public static int resX, resY;
	public static String saveDir;
	
	// Types loaded in from data files
	public static ShipType[] shipTypes;
	public static CraftType[] craftTypes;
	public static WeaponType[] weaponTypes;
	public static SystemType[] systemTypes;
	public static BuyType[] buyTypes;
	public static DebrisType[] debrisTypes;
	public static ExplosionType[] explosionTypes;
	public static Arena[] arenas;
	public static Mission[] missions;
	public static double[] ammoMass;
	public static String[] names;
	
	public static String dataHash; // used to verify data compatibility between multiplayer clients
	private static StringBuilder dataString; // Used to construct dataHash
	public static Game game; // Represents a single game round instance
	public static Server server; // Used when hosting a multiplayer match
	public static Map<Integer, Control> controlVals; // Keyboard controls
	public static DisplayMode[] displayModes;
	public static RenderingHints menuHints, inGameHints, fastHints;
	public static Scalr.Method scaleMethod; // Image scaling method, represents tradeoff between quality and performance
	public static Sound errorSound;
	public static UPnPHandler UPnPHandler;
	public static Font defaultFont;
	public static boolean isExiting, isStarting; // Global program starting or exiting flags
	
	// Swing components for global program GUI
	private static JComponent windowPanel;
	private static JFrame frame;
	private static Stack<Window> windowStack;
	
	private static KeyboardFocusManager manager;
	private static KeyEventDispatcher dispatcher;
	
	private static LinkedList<double[]> ammoMap; // List of ammo types and associated mass
	
	private static GraphicsConfiguration graphicsConfiguration;
	private static BufferStrategy bufferStrategy;
	private static Graphics bufferGraphics;
	private static BufferedImage backgroundBuffer;
	private static RepaintManager normalManager, manualManager;
	
	public Main(){
		isStarting = true;
		isExiting = false;
		
		readDirs();
		
		options = new Options(saveDir + "/options.txt");
		
		if (options.useHardwareAccel){
			if (IS_WINDOWS){
				java.lang.System.setProperty("sun.java2d.d3d","True");
				java.lang.System.setProperty("sun.java2d.ddscale","true");
			}else
				java.lang.System.setProperty("sun.java2d.opengl","True");
		}else{
			java.lang.System.setProperty("sun.java2d.d3d","false");
			java.lang.System.setProperty("sun.java2d.opengl","false");
		}
		
		setDefaultFont();
		setTheme();
		
		if (Runtime.getRuntime().maxMemory()*11/10 < MIN_MEMORY*1000*1000){
			crash("At least "+MIN_MEMORY+"MB of memory required."+
					"Use the Java command line option '-Xmx"+MIN_MEMORY+"m' or greater");
		}
		
		frame = new JFrame("Aether"){
			public Graphics getGraphics(){
				if (backgroundBuffer == null || bufferGraphics != null){
					return super.getGraphics(); // Use normal graphics for menus
				}else
					return backgroundBuffer.getGraphics(); // Using high-performance repainting strategy
			}
		};
		frame.setUndecorated(options.borderless);//fullscreen);
		frame.setFocusTraversalKeysEnabled(false);
		
		windowStack = new Stack<Window>();
		
		windowPanel = new JPanel(){
			public void paintComponent(Graphics g){
				BufferedImage background = getCurrentWindow().getBackgroundImage();
				if (background != null){
					g.drawImage(background,
							-(background.getWidth() - options.resX)/2,
							-(background.getHeight() - options.resY)/2,
							null);
				}
			}
		};
		windowPanel.setDoubleBuffered(false);
		windowPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
		windowPanel.setFocusTraversalKeysEnabled(false);
		
		findDisplayModes();
		setDisplayMode();
		setRenderQuality();
		
		BufferedImage icon;
		try{
			icon = ImageIO.read(new File("data/icon.png"));
		}catch(IOException e){
			icon = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		}
		frame.setIconImage(icon);
		frame.setResizable(options.fullscreen);
		frame.addWindowListener(new WindowListener(){
			public void windowClosed(WindowEvent e){
				exit();
			}
			public void windowClosing(WindowEvent e){
				exit();
			}
			public void windowActivated(WindowEvent e){
				Sound.setPause(false);
			}
			public void windowDeactivated(WindowEvent e){
				Sound.setPause(true);
			}
			public void windowDeiconified(WindowEvent e){
				//Sound.setPause(false);
				new Thread("MaximizeRepaintThread"){
					public void run(){
						try{
							Thread.sleep(500);
						}catch (InterruptedException ex){}
						SwingUtilities.invokeLater(new Runnable(){
							public void run(){
								frame.repaint();
						}});
				}}.start();
			}
			public void windowIconified(WindowEvent e){
				//Sound.setPause(true);
			}
			public void windowOpened(WindowEvent e){}
		});
		if (IS_NIX){
			frame.addFocusListener(new FocusListener(){
		        public void focusGained(FocusEvent e){
		            frame.setAlwaysOnTop(true);
		        }
		        public void focusLost(FocusEvent e){
		            frame.setAlwaysOnTop(false);
		        }
		    });
		}
		
		dataString = new StringBuilder(1024);
		
		config = new Configuration("data/config.txt");
		readControls();
		readTypes();
		readNames();
		
		missions = new Mission[]{new MissionZero(), new MissionOne(), new MissionTwo(), new MissionThree(),
				new MissionFour(), new MissionFive(), new MissionSix(), new MissionSeven(), new MissionEight()};
		
		dataHash = Utility.getHash(dataString.toString());
		dataString = null;
		
		errorSound = new Sound(new File("data/error.wav"));
		
		UPnPHandler = new UPnPHandler();
		
		manualManager = new ManualRepaintManager();
		
		manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		dispatcher = new KeyEventDispatcher(){
			public boolean dispatchKeyEvent(KeyEvent e){
				manager.redispatchEvent(windowStack.peek(), e);
				return true;
		}};
		
		Sound.initialize(options.audioChannels, options.masterVolume);
		
		frame.setContentPane(windowPanel);
		frame.pack();
		frame.setLocationRelativeTo(null);
		
		Menu.load();
		MainMenu menu = new MainMenu();
		addWindow(menu);
		
		frame.setVisible(true);
		Window.load();
		
		// Delay before starting as a hack to avoid some concurrency bug with Swing
		new Thread("StartDelayThread"){
			public void run(){
				try{
					Thread.sleep(IS_WINDOWS ? 1000 : 2000);
				}catch (Exception e){}
				
				isStarting = false;
				LoadWindow.start();
				menu.start();
				Renderable.startLoadThread();
				ExplosionType.startLoadThread();
			}
		}.start();
	}
	
	private static void setDefaultFont(){
		//JLabel testLabel = new JLabel();
		//Font defaultFont;
		//int fontSize = 15;
		//do{
		//	fontSize--;
		//	defaultFont = new Font("Arial", Font.BOLD, fontSize);
		//}while (testLabel.getFontMetrics(defaultFont).stringWidth("AbCdEfGhIjKlMnOpQrStUvWxYz") > 180); //170
		try{
			GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(
					Font.createFont(Font.TRUETYPE_FONT, new FileInputStream("data/font.ttf")));
		}catch (FontFormatException | IOException e){
			e.printStackTrace();
		}
		defaultFont = getDefaultFont(11);
		for (String componentName : new String[]{
				"Button", "ToggleButton", "RadioButton", "CheckBox", "ColorChooser",
				"ComboBox", "Label", "List", "RadioButtonMenuItem", "CheckBoxMenuItem",
				"Menu", "ScrollPane", "TabbedPane", "TextField", "TextArea", "ToolTip"})
			UIManager.put(componentName + ".font", defaultFont);
		Target.unitLabelFont = getDefaultFont(11);
		Unit.statusFont = getDefaultFont(8);
		Unit.iconFont = getDefaultFont(10);
		Arena.previewFontLarge = getDefaultFont(15);
		Arena.previewFontSmall = getDefaultFont(10);
		GameWindow.unitLabelFont = getDefaultFont(12);
		GameWindow.displayFont = getDefaultFont(44);
		GameWindow.chatFont = getDefaultFont(16);
		GameWindow.missionChatFont = getDefaultFont(16);
	}
	
	public static Font getDefaultFont(int size){
		return new Font("Tahoma", Font.BOLD, size);
	}
	public static Font getPlainFont(int size){
		return new Font("Tahoma", Font.PLAIN, size);
	}
	
	// Select best display mode for each compatible resolution
	private static void findDisplayModes(){
		GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		
		Map<DisplayMode,Integer> modeFitness = new HashMap<DisplayMode,Integer>();
		for (DisplayMode mode : device.getDisplayModes()){
			if (mode.getHeight() >= RES_Y_SHORT && mode.getHeight() <= RES_Y_TALL && mode.getWidth() >= RES_X_NARROW){
				int fitness = (mode.getBitDepth() == BIT_DEPTH ? 2 : 0) +
						(mode.getBitDepth() == DisplayMode.BIT_DEPTH_MULTI ? 1 : 0) +
						(mode.getRefreshRate() == device.getDisplayMode().getRefreshRate() ? 3 : 0);
				DisplayMode sameSizeMode = null;
				for (DisplayMode existingMode : modeFitness.keySet()){
					if (existingMode.getWidth() == mode.getWidth() && existingMode.getHeight() == mode.getHeight())
						sameSizeMode = existingMode;
				}
				if (sameSizeMode == null){
					modeFitness.put(mode, fitness);
				}else if (modeFitness.get(sameSizeMode) < fitness){
					modeFitness.remove(sameSizeMode);
					modeFitness.put(mode, fitness);
				}
			}
		}
		
		displayModes = new DisplayMode[modeFitness.size()];
		modeFitness.keySet().toArray(displayModes);
	}
	
	public static void setDisplayMode(){
		try{
			GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
			graphicsConfiguration = device.getDefaultConfiguration();
			Scalr.graphicsConfiguration = graphicsConfiguration;
			
			DisplayMode selectedMode = displayModes[0];
			for (DisplayMode mode : displayModes){
				if (mode.getWidth() == options.resX && mode.getHeight() == options.resY){
					selectedMode = mode;
					break;
				}
				// Default to highest resolution available if selected option not found
				if (mode.getWidth() > selectedMode.getWidth() || mode.getHeight() > selectedMode.getHeight())
					selectedMode = mode;
			}
			
			if (options.fullscreen){
				device.setFullScreenWindow(frame);
				device.setDisplayMode(selectedMode);
			}
			
			resX = selectedMode.getWidth();
			resY = selectedMode.getHeight();
		}catch(Exception ex){
			crash("Video mode not supported: "+ex.getMessage());
		}
		
		windowPanel.setPreferredSize(new Dimension(resX, resY));
	}
	
	// Set various AWT hints depending on selected options
	public static void setRenderQuality(){
		menuHints = new RenderingHints(null);
		menuHints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		inGameHints = new RenderingHints(null);
		inGameHints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		if (options.useAntiAliasing){
			menuHints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			menuHints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			inGameHints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			inGameHints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}else{
			menuHints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			menuHints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
			inGameHints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			inGameHints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		}
		if (options.scalingQuality <= 1){
			scaleMethod = Scalr.Method.SPEED;
		}else if (options.scalingQuality == 2){
			scaleMethod = Scalr.Method.BALANCED;
		}else if (options.scalingQuality >= 3)
			scaleMethod = Scalr.Method.QUALITY;
		fastHints = new RenderingHints(null);
		fastHints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		fastHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
		fastHints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		fastHints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
	}
	
	// Make sure directories exist
	private static void readDirs(){
		File file = new File("data/directory.txt");
		try{
			BufferedReader input = new BufferedReader(new FileReader(file));
			saveDir = input.readLine();
			input.close();
		}catch (Exception ex){
			ex.printStackTrace();
			crash(file.getPath());
		}
	}
	
	public static void readControls(){
		try{
			Map<String, String> data = Utility.readDataFile(saveDir + "/controls.txt");
			controlVals = new HashMap<Integer, Control>(32);
			for (String string : data.keySet())
				controlVals.put(Integer.valueOf(data.get(string)), Control.valueOf(string.toUpperCase()));
		}catch (Exception ex){
			crash(saveDir + "/controls.txt");
		}
	}
	
	private static void readTypes(){
		try{
			String[] availibleExplosionTypes = new File("data/explosions").list();
			Arrays.sort(availibleExplosionTypes);
			explosionTypes = new ExplosionType[availibleExplosionTypes.length];
			for (int x = 0; x < availibleExplosionTypes.length; x++)
				explosionTypes[x] = new ExplosionType(availibleExplosionTypes[x]);
			
			ammoMap = new LinkedList<double[]>();
			String[] availibleWeaponTypes = new File("data/weapons").list();
			Arrays.sort(availibleWeaponTypes);
			weaponTypes = new WeaponType[availibleWeaponTypes.length];
			for (int x = 0; x < availibleWeaponTypes.length; x++){
				String type = Utility.readDataFile("data/weapons/" + availibleWeaponTypes[x] + "/data.txt").get("type");
				if (type.equals("missile")){
					weaponTypes[x] = new MissileType(availibleWeaponTypes[x]);
				}else if (type.equals("gun")){
					weaponTypes[x] = new GunType(availibleWeaponTypes[x]);
				}else if (type.equals("beam"))
					weaponTypes[x] = new BeamType(availibleWeaponTypes[x]);
			}
			ammoMass = new double[ammoMap.size()];
			for (int x = 0; x < ammoMass.length; x++)
				ammoMass[x] = ammoMap.get(x)[1];
			ammoMap = null;
			
			String[] availibleCraftTypes = new File("data/crafts").list();
			Arrays.sort(availibleCraftTypes);
			craftTypes = new CraftType[availibleCraftTypes.length];
			for (int x = 0; x < availibleCraftTypes.length; x++)
				craftTypes[x] = new CraftType(availibleCraftTypes[x]);
			
			String[] availibleShipTypes = new File("data/ships").list();
			Arrays.sort(availibleShipTypes);
			shipTypes = new ShipType[availibleShipTypes.length];
			for (int x = 0; x < availibleShipTypes.length; x++)
				shipTypes[x] = new ShipType(availibleShipTypes[x]);
			
			String[] availibleSystemTypes = new File("data/systems").list();
			Arrays.sort(availibleSystemTypes);
			systemTypes = new SystemType[availibleSystemTypes.length];
			for (int x = 0; x < availibleSystemTypes.length; x++){
				String type = Utility.readDataFile("data/systems/" + availibleSystemTypes[x] + "/data.txt").get("type");
				if (type.equals("sensor")){
					systemTypes[x] = new SensorType(availibleSystemTypes[x]);
				}else if (type.equals("detector")){
					systemTypes[x] = new DetectorType(availibleSystemTypes[x]);
				}else if (type.equals("cloak")){
					systemTypes[x] = new CloakType(availibleSystemTypes[x]);
				}else if (type.equals("scanner")){
					systemTypes[x] = new ScannerType(availibleSystemTypes[x]);
				}else if (type.equals("booster"))
					systemTypes[x] = new BoosterType(availibleSystemTypes[x]);
			}
			
			String[] availibleDebrisTypes = new File("data/debris").list();
			Arrays.sort(availibleDebrisTypes);
			debrisTypes = new DebrisType[availibleDebrisTypes.length];
			for (int x = 0; x < availibleDebrisTypes.length; x++)
				debrisTypes[x] = new DebrisType(availibleDebrisTypes[x]);
			for (int x = 0; x < debrisTypes.length; x++){
				int smallest = x;
				for (int y = x+1; y < debrisTypes.length; y++)
					if (debrisTypes[y].size < debrisTypes[smallest].size)
						smallest = y;
				DebrisType buff = debrisTypes[smallest];
				debrisTypes[smallest] = debrisTypes[x];
				debrisTypes[x] = buff;
			}
			
			String[] availibleArenas = new File("data/arenas").list();
			Arrays.sort(availibleArenas);
			arenas = new Arena[availibleArenas.length];
			for (int x = 0; x < availibleArenas.length; x++)
				arenas[x] = new Arena(availibleArenas[x]);
			
			buyTypes = new BuyType[shipTypes.length+craftTypes.length+weaponTypes.length+systemTypes.length];
			int count = 0;
			for (BuyType type : shipTypes)
				buyTypes[count++] = type;
			for (BuyType type : craftTypes)
				buyTypes[count++] = type;
			for (BuyType type : weaponTypes)
				buyTypes[count++] = type;
			for (BuyType type : systemTypes)
				buyTypes[count++] = type;
		}catch (Exception ex){
			ex.printStackTrace();
			crash("Error reading game data");
		}
	}
	
	// Default player names
	private static void readNames(){
		File namesFile = new File("data/names.txt");
		try{
			Stack<String> orderedNames = new Stack<String>();
			BufferedReader input = new BufferedReader(new FileReader(namesFile));
			String name;
			while ((name = input.readLine()) != null)
				orderedNames.add(name);
			input.close();
			names = new String[orderedNames.size()];
			int index;
			while (!orderedNames.isEmpty()){
				do{
					index = (int)(random()*names.length);
				}while(names[index] != null);
				names[index] = orderedNames.pop();
			}
		}catch(Exception e){
			crash(namesFile.getPath());
		}
	}
	
	// Configure PgsLookAndFeel library
	private static void setTheme(){
		PlafOptions.setCurrentTheme(new SilverTheme(){
			public void addCustomEntriesToTable(UIDefaults table){
				super.addCustomEntriesToTable(table);
				table.put("glow", new ColorUIResource(100, 160, 100));
			}
		});
		PlafOptions.setAsLookAndFeel();
		PlafOptions.updateAllUIs();
		
		ArrayList<Object> keyList = new ArrayList<Object>();
		for (Object key : UIManager.getLookAndFeelDefaults().keySet())
			keyList.add(key);
		UIDefaults defaults = UIManager.getDefaults();
		for (Object key : keyList){
			if (Window.DEFAULT_PANEL_BACKGROUND.equals(defaults.get(key)))
				defaults.put(key, Window.COMPONENT_BACKGROUND);
		}
		
		defaults.put("List.background", Window.PANEL_VERY_LIGHT);
		defaults.put("Button.gradientStart", new Color(240, 240, 240));
		defaults.put("Button.gradientEnd", new Color(190, 200, 230));
		defaults.put("ComboBox.disabledBackground", Window.PANEL_VERY_LIGHT);
	}
	
	public static ExplosionType getExplosionType(String type){
		for (int x = 0; x < explosionTypes.length; x++){
			if (explosionTypes[x].type.equals(type))
				return explosionTypes[x];
		}
		return null;
	}
	
	public static int mapAmmo(int ammoType, double mass){
		for (int x = 0; x < ammoMap.size(); x++)
			if ((int)ammoMap.get(x)[0] == ammoType){
				ammoMap.get(x)[1] = max(ammoMap.get(x)[1], mass);
				return x;
			}
		
		ammoMap.add(new double[]{ammoType, mass});
		return ammoMap.size()-1;
	}
	
	public static void appendData(Map<String,String> data, String label){
		dataString.append(label+"[");
		for (String key : data.keySet())
			dataString.append(key+"="+data.get(key)+",");
		dataString.append("]");
	}
	
	// Put new GUI window onto stack
	public static void addWindow(Window window){
		windowPanel.removeAll();
		windowPanel.add(window.getRootComponent());
		windowStack.add(window);
		if (windowStack.size() >= 2)
			windowStack.get(windowStack.size()-2).suspend();
		//windowPanel.revalidate();
		frame.revalidate();
		frame.repaint();
	}
	
	// Pop top GUI window from stack
	public static void removeWindow(){
		windowStack.peek().exit();
		windowStack.pop();
		Window window = windowStack.peek();
		windowPanel.removeAll();
		windowPanel.add(window.getRootComponent());
		windowPanel.revalidate();
		//frame.revalidate();
		window.resume();
		frame.repaint();
	}
	
	public static void goToMainMenu(){
		while (windowStack.size() > 1)
			removeWindow();
	}
	
	public static Window getCurrentWindow(){
		if (!windowStack.isEmpty())
			return windowStack.peek();
		return null;
	}
	
	public static Graphics2D getFrameGraphics(){
		if (bufferGraphics == null && bufferStrategy != null){
			bufferGraphics = bufferStrategy.getDrawGraphics();
			bufferGraphics.drawImage(backgroundBuffer, 0, 0, null);
		}
		return (Graphics2D)bufferGraphics;
	}
	
	public static void paintDirtyRegions(){
		manualManager.paintDirtyRegions();
	}
	
	public static void showBuffer(){
		if (bufferStrategy != null && bufferGraphics != null){
			bufferGraphics.dispose();
			bufferStrategy.show();
			bufferGraphics = null;
		}
	}
	
	// Select high-performance mode using manualManager for in-game, or normal AWT repaint manager for in-menu
	public static void setHighPerformance(boolean hp){
		setIgnoreRepaint(frame, hp);
		if (hp){
			normalManager = RepaintManager.currentManager(windowPanel);
			RepaintManager.setCurrentManager(manualManager);
			if (bufferStrategy == null){
				frame.createBufferStrategy(2);
				bufferStrategy = frame.getBufferStrategy();
			}
			if (backgroundBuffer == null)
				backgroundBuffer = getCompatibleImage(resX, resY, false);
			Sprite.initBuffer();
		}else{
			RepaintManager.setCurrentManager(normalManager);
			if (bufferStrategy != null){
				bufferStrategy.dispose();
				bufferStrategy = null;
			}
			if (bufferGraphics != null){
				bufferGraphics.dispose();
				bufferGraphics = null;
			}
			if (backgroundBuffer != null){
				backgroundBuffer.flush();
				backgroundBuffer = null;
			}
		}
	}
	
	// Recursively set all children to ignore repaint. Needed for high-performance mode
	private static void setIgnoreRepaint(Container container, boolean ignore){
		container.setIgnoreRepaint(ignore);
		java.awt.Component[] components = container.getComponents();
		for (java.awt.Component component : components){
	        if (component instanceof Container){
	        	setIgnoreRepaint((Container)component, ignore);
	        }else
	        	component.setIgnoreRepaint(ignore);
	    }
	}
	
	public static void setDispatcherEnabled(boolean enabled){
		if (enabled){
			manager.addKeyEventDispatcher(dispatcher);
		}else
			manager.removeKeyEventDispatcher(dispatcher);
	}
	
	public static Point getMousePosition(){
		//if (borderless){
		//	return MouseInfo.getPointerInfo().getLocation();
		//}else
		return windowPanel.getMousePosition();
	}
	
	public static void setMousePosition(int posX, int posY){
		Point pos = windowPanel.getLocationOnScreen();
		if (pos != null){
			try{
				new Robot().mouseMove(pos.x + posX, pos.y + posY);
			}catch (AWTException ex){}
		}
	}
	
	public static BufferedImage getCompatibleImage(int width, int height, boolean transparent){
		if (graphicsConfiguration == null){
			return new BufferedImage(
					width, height, transparent ? BufferedImage.TYPE_4BYTE_ABGR_PRE : BufferedImage.TYPE_INT_RGB);
		}else{
			return graphicsConfiguration.createCompatibleImage(
					width, height, transparent ? Transparency.TRANSLUCENT : Transparency.OPAQUE);
		}
	}
	
	public static VolatileImage getCompatibleVolatileImage(int width, int height, boolean transparent){
		return graphicsConfiguration.createCompatibleVolatileImage(width, height,
				transparent ? Transparency.TRANSLUCENT : Transparency.OPAQUE);
	}
	
	// Get version of image with hardware acceleration enabled
	public static BufferedImage convert(BufferedImage image){
		if (graphicsConfiguration == null)
			return image;
		BufferedImage compatibleImage = graphicsConfiguration.createCompatibleImage(
				image.getWidth(), image.getHeight(), Transparency.TRANSLUCENT);
		Graphics2D g = compatibleImage.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		image.flush();
		return compatibleImage;
	}
	
	// Get version of image with hardware acceleration enabled
	public static VolatileImage convertVolatile(BufferedImage image){
		VolatileImage compatibleImage = graphicsConfiguration.createCompatibleVolatileImage(
				image.getWidth(), image.getHeight(), Transparency.TRANSLUCENT);
		Graphics2D g = compatibleImage.createGraphics();
		g.setComposite(AlphaComposite.Src);
		g.drawImage(image, 0, 0, null);
		g.dispose();
		image.flush();
		return compatibleImage;
	}
	
	public static boolean isAccelerated(BufferedImage image){
		return image.getCapabilities(graphicsConfiguration).isAccelerated();
	}
	
	public static int getStringWidth(String string, Font font){
		return windowPanel.getFontMetrics(font).stringWidth(string);
	}
	
	public static void exit(){
		if (!isExiting){
			isExiting = true;
			Sound.deinitialize();
			Main.UPnPHandler.close();
			java.lang.System.exit(0);
		}
	}
	
	public static void crash(String message){
		isExiting = true;
		java.lang.System.err.println("Error:");
		java.lang.System.err.println(message);
		java.lang.System.exit(1);
	}
	
	public static void print(Object obj){
		String str = obj == null ? "null" : obj.toString();
		Window window = getCurrentWindow();
		if (window instanceof GameWindow)
			((GameWindow)window).receiveChat(str, 0);
		java.lang.System.out.println("****  " + str);
	}
	
	public static void startGame(final Game game, final int backgroundSeed, final int gameSeed){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				Main.game = game;
				
				for (Player player : game.players){
					if (player instanceof HumanPlayer){
						GameWindow window = new GameWindow((HumanPlayer)player, game.gameSpeed, gameSeed);
						((HumanPlayer)player).setWindow(window);
						addWindow(window);
					}
				}
				
				// Add and wait on load indow even when loading is complete
				addWindow(new LoadWindow());
				
				new Thread("StartGameThread"){
					public void run(){
						LoadWindow.awaitLoad();
						game.start(backgroundSeed, gameSeed);
						SwingUtilities.invokeLater(new Runnable(){
							public void run(){
								removeWindow();
						}});
						try{
							Thread.sleep(1000);
						}catch (InterruptedException ex){}
						SwingUtilities.invokeLater(new Runnable(){
							public void run(){
								getCurrentWindow().revalidate();
						}});
				}}.start();
		}});
	}
	
	public static void main(String[] args){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				new Main();
		}});
	}
	
	// This is a copy of some AWT code but modified to avaid unnecessary repaints.
	// Ugly way of doing it but I couldn't find any other way.
	private class ManualRepaintManager extends RepaintManager{
		Queue<RegionNode> dirtyRegions;
		
		public ManualRepaintManager(){
			dirtyRegions = new ConcurrentLinkedQueue<RegionNode>();
		}
		
		public void addDirtyRegion(JComponent component, int x, int y, int w, int h){
			Container container = component;
			while (!container.isOpaque() && container.getParent() != null){
				x += container.getX();
				y += container.getY();
				container = container.getParent();
			}
			
			Rectangle existing = null;
			for (RegionNode node : dirtyRegions){
				if (node.container == container)
					existing = node.region;
			}
			if (existing != null){
	            SwingUtilities.computeUnion(x, y, w, h, existing);
			}else
				dirtyRegions.offer(new RegionNode(container, x, y, w, h));
		}
		
		public void paintDirtyRegions(){
			while (!dirtyRegions.isEmpty()){
				RegionNode node = dirtyRegions.poll();
				if (node.container.isShowing()){
					Graphics g = node.container.getGraphics();
					if (g != null){
						g.setClip(node.region);
						node.container.paint(g);
						g.dispose();
					}
				}
			}
		}
		
		private class RegionNode{
			final Container container;
			final Rectangle region;
			public RegionNode(Container container, int x, int y, int w, int h){
				this.container = container;
				region = new Rectangle(x, y, w, h);
			}
		}
	}
	
}
