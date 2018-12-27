import static java.lang.Math.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.math.*;
import javax.imageio.*;
import javax.swing.*;
import javax.swing.plaf.*;
import java.security.*;
import org.imgscalr.*;
import com.pagosoft.plaf.*;
import com.pagosoft.plaf.themes.*;

public final class Main{
	public static final int VERSION = 30, SUBVERSION = 0;
	public static final int RES_X_NARROW = 1024, RES_Y = 768;
	public static final int BIT_DEPTH = 16;
	public static final int TPS = 100;
	public static final int MIN_MEMORY = 900+Renderable.MIN_CACHE_MEMORY;
	public static final boolean DEBUG = false;
	
	static final String OS_NAME = java.lang.System.getProperty("os.name").toLowerCase();
	static final boolean IS_WINDOWS = OS_NAME.contains("win");
	static final boolean IS_MAC = OS_NAME.contains("mac");
	static final boolean IS_UNIXLIKE = OS_NAME.contains("nix") || OS_NAME.contains("nux") || OS_NAME.contains("aix");
	
	public static int resX;
	public static int refreshRate;
	public static boolean useAntiAliasing;
	public static int scalingQuality;
	public static int framesPerSec;
	public static double zoomRatio;
	public static double moveZoomRatio;
	public static int zoomLevels;
	public static double renderSizeScaling;
	public static int renderAnglesMultiplier;
	public static int audioChannels;
	public static double scrollSpeed;
	public static double accelRate;
	public static double cameraMoveMultiplier;
	public static int defaultAutoRepair;
	public static double masterVolume;
	public static double musicVolume;
	public static int maxPointerLineLength;
	public static int statusSize;
	public static int targetFadeTime;
	public static double thrustScale;
	public static double debrisAmount;
	public static double debrisRenderAnglesMultiplier;
	public static int clientPort;
	public static int serverPort;
	public static int UDPPort;
	public static int serverBroadcastPort, clientBroadcastPort;
	public static int lobbyHostPort, lobbyClientPort;
	public static String lobbyServer;
	public static boolean forceTCP;
	public static boolean UPnPEnabled;
	public static String username;
	public static boolean fullscreen, startFullscreen;
	public static boolean useHardwareAccel;
	public static String explodingShipExplosion;
	public static double soundFalloffRate;
	public static double minSoundVolume;
	//public static boolean useGLG2D;
	
	public static double kineticShieldDamage;
	public static double explosiveShieldDamage;
	public static double energyPerThrust;
	public static double energyPerTurnThrust;
	public static double energyPerShield;
	public static double armorExplosiveEffectiveness;
	public static double armorKineticEffectiveness;
	public static double explosiveSubtargetChance;
	public static double explosiveComponentChance;
	public static double explosiveComponentDamage;
	public static double kineticSubtargetChance;
	public static double kineticComponentChance;
	public static double kineticComponentDamage;
	public static double EMComponentChance;
	public static int targetAccelTimeframe;
	public static double debrisVisionSize;
	public static double unitRenderAngle;
	public static double craftDockDistance;
	public static double craftDockSpeed;
	public static double repairDistance;
	public static double repairSpeed;
	public static double captureDistance;
	public static double captureSpeed;
	public static double maneuverThrust;
	public static double massPerMaterial;
	public static double scrapReturn;
	public static double maxComponentDamage;
	public static double maxScrapDamage;
	public static double explosionImpulsePerMass, impactImpulsePerDamage;
	public static int netAdjustTime;
	public static double unitAccelMultiplier, unitTurnAccelMultiplier;
	public static double unitVisionMultiplier;
	
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
	
	public static String dataHash;
	public static Game game;
	public static Server server;
	public static Map<Integer, Control> controlVals;
	public static DisplayMode[] displayModes;
	public static RenderingHints menuHints, inGameHints, fastHints;
	public static Scalr.Method scaleMethod;
	public static Sound errorSound;
	public static UPnPHandler UPnPHandler;
	public static boolean isExiting;
	
	private static JComponent windowPanel;
	private static JFrame frame;
	private static Stack<Window> windowStack;
	private static KeyboardFocusManager manager;
	private static KeyEventDispatcher dispatcher;
	private static LinkedList<double[]> ammoMap;
	public static GraphicsConfiguration graphicsConfiguration;
	private static StringBuilder dataString;
	private static BufferStrategy bufferStrategy;
	private static Graphics bufferGraphics;
	private static BufferedImage backgroundBuffer;
	private static RepaintManager normalManager, manualManager;
	private static BufferedImage background;
	
	public Main(){
		readOptions();
		startFullscreen = fullscreen;
		
		if (useHardwareAccel){// && !useGLG2D){
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
					return super.getGraphics();
				}else
					return backgroundBuffer.getGraphics();
			}
		};
		frame.setUndecorated(fullscreen);
		frame.setFocusTraversalKeysEnabled(false);
		
		windowStack = new Stack<Window>();
		
		try{
			background = Main.convert(ImageIO.read(new File("data/menu_background.png")));
		}catch(IOException e){
			background = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		}
		windowPanel = new JPanel(){
			public void paintComponent(Graphics g){
				g.drawImage(background, -(background.getWidth()-Main.resX)/2, 0, null);
			}
		};
		windowPanel.setDoubleBuffered(false);
		windowPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
		windowPanel.setFocusTraversalKeysEnabled(false);
		//windowPanel.setBackground(Window.BACKGROUND_COLOR);
		
		findDisplayModes();
		setDisplayMode();
		setRenderQuality();
		
		try{
			Arena.starBig = ImageIO.read(new File("data/star_big.png"));
			Arena.starSmall = ImageIO.read(new File("data/star_small.png"));
		}catch (IOException ex){
			Arena.starBig = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			Arena.starSmall = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		}
		
		BufferedImage icon;
		try{
			icon = ImageIO.read(new File("data/icon.png"));
		}catch(IOException e){
			icon = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		}
		frame.setIconImage(icon);
		frame.setResizable(fullscreen);
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
		if (IS_UNIXLIKE){
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
		
		readConfig();
		readControls();
		readTypes();
		readNames();
		
		dataHash = getHash(dataString.toString());
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
		
		Sound.initialize(audioChannels, masterVolume);
		
		frame.setContentPane(windowPanel);
		frame.pack();
		
		try{
			Thread.sleep(500);
		}catch (Exception e){}
		
		MainMenu menu = new MainMenu();
		addWindow(menu);
		
		frame.setVisible(true);
		
		LoadWindow.start();
		menu.start();
		
		Renderable.startLoadThread();
		ExplosionType.startLoadThread();
	}
	
	private static void setDefaultFont(){
		JLabel testLabel = new JLabel();
		Font defaultFont;
		int fontSize = 15;
		do{
			defaultFont = new Font("Arial", Font.BOLD, --fontSize);
		}while (testLabel.getFontMetrics(defaultFont).stringWidth("AbCdEfGhIjKlMnOpQrStUvWxYz") > 170);
		
		for (String componentName : new String[]{
				"Button", "ToggleButton", "RadioButton", "CheckBox", "ColorChooser",
				"ComboBox", "Label", "List", "RadioButtonMenuItem", "CheckBoxMenuItem",
				"Menu", "ScrollPane", "TabbedPane", "TextField", "TextArea", "ToolTip"})
			UIManager.put(componentName + ".font", defaultFont);
	}
	
	private static void findDisplayModes(){
		GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		
		Map<DisplayMode,Integer> modeFitness = new HashMap<DisplayMode,Integer>();
		for (DisplayMode mode : device.getDisplayModes()){
			if (mode.getHeight() == RES_Y && mode.getWidth() >= RES_X_NARROW){
				int fitness = (mode.getBitDepth() == BIT_DEPTH ? 2 : 0) +
						(mode.getBitDepth() == DisplayMode.BIT_DEPTH_MULTI ? 1 : 0) +
						(mode.getRefreshRate() == device.getDisplayMode().getRefreshRate() ? 3 : 0);
				DisplayMode sameWidthMode = null;
				for (DisplayMode existingMode : modeFitness.keySet()){
					if (existingMode.getWidth() == mode.getWidth())
						sameWidthMode = existingMode;
				}
				if (sameWidthMode == null){
					modeFitness.put(mode, fitness);
				}else if (modeFitness.get(sameWidthMode) < fitness){
					modeFitness.remove(sameWidthMode);
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
			
			if (startFullscreen){
				device.setFullScreenWindow(frame);
				
				DisplayMode selectedMode = displayModes[0];
				for (DisplayMode mode : displayModes){
					if (mode.getWidth() == resX){
						selectedMode = mode;
						break;
					}
					if (mode.getWidth() > selectedMode.getWidth())
						selectedMode = mode;
				}
				
				device.setDisplayMode(selectedMode);
				resX = selectedMode.getWidth();
				refreshRate = selectedMode.getRefreshRate();
			}
		}catch(Exception ex){
			crash("Video mode not supported: "+ex.getMessage());
		}
		
		windowPanel.setPreferredSize(new Dimension(resX, RES_Y));
	}
	
	public static void setRenderQuality(){
		menuHints = new RenderingHints(null);
		menuHints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		inGameHints = new RenderingHints(null);
		inGameHints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		if (useAntiAliasing){
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
		if (scalingQuality <= 1){
			scaleMethod = Scalr.Method.SPEED;
		}else if (scalingQuality == 2){
			scaleMethod = Scalr.Method.BALANCED;
		}else if (scalingQuality >= 3)
			scaleMethod = Scalr.Method.QUALITY;
		fastHints = new RenderingHints(null);
		fastHints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		fastHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
		fastHints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		fastHints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
	}
	
	public static void readOptions(){
		try{
			Map<String, String> data = readDataFile("data/options.txt");
			resX = Integer.valueOf(data.get("screen_width"));
			scalingQuality = Integer.valueOf(data.get("scaling_quality"));
			useAntiAliasing = Boolean.valueOf(data.get("antialiasing"));
			masterVolume = Float.valueOf(data.get("master_volume"));
			musicVolume = Float.valueOf(data.get("music_volume"));
			framesPerSec = Integer.valueOf(data.get("frames_per_sec"));
			debrisAmount = Double.valueOf(data.get("debris_amount"));
			zoomRatio = Double.valueOf(data.get("zoom_ratio"));
			moveZoomRatio = Double.valueOf(data.get("camera_move_zoom_ratio"));
			zoomLevels = Integer.valueOf(data.get("zoom_levels"));
			renderSizeScaling = Double.valueOf(data.get("render_scaling"));
			renderAnglesMultiplier  = Integer.valueOf(data.get("render_angles_multiplier"));
			audioChannels = Integer.valueOf(data.get("audio_channels"));
			scrollSpeed = Double.valueOf(data.get("camera_scroll_speed"))/TPS;
			accelRate = Double.valueOf(data.get("camera_accel_rate"))/TPS/TPS;
			cameraMoveMultiplier =  Double.valueOf(data.get("camera_move_multiplier"));
			maxPointerLineLength = Integer.valueOf(data.get("max_pointer_line_length"));
			defaultAutoRepair = Integer.valueOf(data.get("default_auto_repair"));
			statusSize = Integer.valueOf(data.get("unit_status_size"));
			targetFadeTime = Integer.valueOf(data.get("target_fade_time"))*TPS;
			thrustScale = Double.valueOf(data.get("thrust_scale"))/TPS/TPS;
			debrisRenderAnglesMultiplier = Double.valueOf(data.get("debris_render_angles_multiplier"));
			clientPort = Integer.valueOf(data.get("client_port"));
			serverPort = Integer.valueOf(data.get("server_listen_port"));
			UDPPort = Integer.valueOf(data.get("udp_port"));
			serverBroadcastPort = Integer.valueOf(data.get("server_broadcast_port"));
			clientBroadcastPort = serverBroadcastPort;
			lobbyHostPort = Integer.valueOf(data.get("lobby_host_port"));
			lobbyClientPort = Integer.valueOf(data.get("lobby_client_port"));
			lobbyServer = data.get("lobby_server");
			forceTCP = Boolean.valueOf(data.get("force_tcp"));
			UPnPEnabled = Boolean.valueOf(data.get("enable_upnp"));
			username = data.get("username");
			fullscreen = Boolean.valueOf(data.get("fullscreen"));
			useHardwareAccel = Boolean.valueOf(data.get("use_hardware_accel"));
			explodingShipExplosion = data.get("death_sequence_explosion");
			soundFalloffRate = Double.valueOf(data.get("sound_falloff_multiplier"))/(0.5*RES_X_NARROW);
			
			minSoundVolume = Double.valueOf(data.get("min_sound_volume"));
			//useGLG2D = Boolean.valueOf(data.get("use_glg2d"));
		}catch (Exception e){
			crash("data/options.txt");
		}
	}
	
	public static void readConfig(){
		try{
			Map<String, String> data = readDataFile("data/config.txt");
			debrisVisionSize = Double.valueOf(data.get("debris_vision_size"));
			unitRenderAngle = Double.valueOf(data.get("unit_render_angle"));
			kineticShieldDamage = Double.valueOf(data.get("kinetic_shield_damage"));
			explosiveShieldDamage = Double.valueOf(data.get("explosive_shield_damage"));
			unitVisionMultiplier = Double.valueOf(data.get("unit_vision_multiplier"));
			unitAccelMultiplier = Double.valueOf(data.get("unit_accel_multiplier"));
			unitTurnAccelMultiplier = Double.valueOf(data.get("unit_turn_accel_multiplier"));
			thrustScale *= unitAccelMultiplier;
			energyPerThrust = Double.valueOf(data.get("energy_per_thrust_second"))/TPS/unitAccelMultiplier;
			energyPerTurnThrust = Double.valueOf(data.get("energy_per_turn_thrust_second"))/TPS/unitTurnAccelMultiplier;
			energyPerShield = Double.valueOf(data.get("energy_per_shield"));
			targetAccelTimeframe = (int)(Double.valueOf(data.get("targeting_accel_timeframe"))*TPS);
			armorExplosiveEffectiveness = Double.valueOf(data.get("armor_explosive_effectiveness"));
			armorKineticEffectiveness = Double.valueOf(data.get("armor_kinetic_effectiveness"));
			explosiveSubtargetChance = Double.valueOf(data.get("explosive_hit_subtarget_chance"));
			explosiveComponentChance = Double.valueOf(data.get("explosive_hit_component_chance"));
			explosiveComponentDamage = Double.valueOf(data.get("explosive_hit_component_max_damage"));
			kineticSubtargetChance = Double.valueOf(data.get("kinetic_hit_subtarget_chance"));
			kineticComponentChance = Double.valueOf(data.get("kinetic_hit_component_chance"));
			kineticComponentChance = Double.valueOf(data.get("kinetic_hit_component_max_damage"));
			EMComponentChance = Double.valueOf(data.get("em_hit_component_chance"));
			craftDockDistance = Double.valueOf(data.get("craft_dock_distance"));
			craftDockSpeed = Double.valueOf(data.get("craft_dock_speed"))/TPS;
			repairDistance = Double.valueOf(data.get("repair_distance"));
			repairSpeed = Double.valueOf(data.get("repair_speed"))/TPS;
			captureDistance = Double.valueOf(data.get("capture_distance"));
			captureSpeed = Double.valueOf(data.get("capture_speed"))/TPS;
			maneuverThrust = Double.valueOf(data.get("maneuver_thrust"));
			massPerMaterial = Double.valueOf(data.get("mass_per_repair_material"));
			scrapReturn = Double.valueOf(data.get("scrap_return"));
			maxComponentDamage = Double.valueOf(data.get("max_component_damage"));
			maxScrapDamage = Double.valueOf(data.get("max_scrap_damage"));
			explosionImpulsePerMass = Double.valueOf(data.get("explosion_impulse_per_mass"))/TPS;
			impactImpulsePerDamage = Double.valueOf(data.get("impact_impulse_per_damage"))/TPS;
			netAdjustTime = (int)(Double.valueOf(data.get("net_update_adjust_time"))*TPS);
			
			appendData(data, "");
		}catch (Exception e){
			crash("data/config.txt");
		}
	}
	
	public static void readControls(){
		try{
			Map<String, String> data = readDataFile("data/controls.txt");
			controlVals = new HashMap<Integer, Control>(32);
			for (String string : data.keySet())
				controlVals.put(Integer.valueOf(data.get(string)), Control.valueOf(string.toUpperCase()));
		}catch (Exception ex){
			crash("data/controls.dat");
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
				String type = readDataFile("data/weapons/" + availibleWeaponTypes[x] + "/data.txt").get("type");
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
			
			String[] availibleShipTypes = new File("data/ships").list();
			Arrays.sort(availibleShipTypes);
			shipTypes = new ShipType[availibleShipTypes.length];
			for (int x = 0; x < availibleShipTypes.length; x++)
				shipTypes[x] = new ShipType(availibleShipTypes[x]);
			
			String[] availibleCraftTypes = new File("data/crafts").list();
			Arrays.sort(availibleCraftTypes);
			craftTypes = new CraftType[availibleCraftTypes.length];
			for (int x = 0; x < availibleCraftTypes.length; x++)
				craftTypes[x] = new CraftType(availibleCraftTypes[x]);
			
			String[] availibleSystemTypes = new File("data/systems").list();
			Arrays.sort(availibleSystemTypes);
			systemTypes = new SystemType[availibleSystemTypes.length];
			for (int x = 0; x < availibleSystemTypes.length; x++){
				String type = readDataFile("data/systems/" + availibleSystemTypes[x] + "/data.txt").get("type");
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
			
			String[] availibleMissions = new File("data/missions").list();
			Arrays.sort(availibleMissions);
			missions = new Mission[availibleMissions.length];
			for (int x = 0; x < availibleMissions.length; x++)
				missions[x] = new Mission(availibleMissions[x]);
			
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
				backgroundBuffer = getCompatibleImage(resX, RES_Y, false);
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
	
	public static Color getColor(double val, double grayness){
		if (val > 1.0)
			val = 1.0;
		else if (val < 0.0)
			val = 0.0;
		int gray = (int)(255*grayness);
		return new Color((int)(min(255, gray+255-510*(val-0.5))),
				(int)(min(255, gray+510*val)), gray);
	}
	
	public static Point getMousePosition(){
		if (startFullscreen){
			return MouseInfo.getPointerInfo().getLocation();
		}else
			return windowPanel.getMousePosition();
	}
	
	public static boolean isPrintable(char c){
	    Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
	    return (!Character.isISOControl(c)) &&
	            c != KeyEvent.CHAR_UNDEFINED &&
	            block != null &&
	            block != Character.UnicodeBlock.SPECIALS;
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
	
	public static Map<String, String> readDataFile(String file){
		Map<String, String> output = new TreeMap<String, String>();
		try{
			BufferedReader input = new BufferedReader(new FileReader(file));
			String property;
			while((property = input.readLine()) != null){
				if (!property.trim().isEmpty() && property.charAt(0) != '#'){
					int y = 0;
					while (property.charAt(++y) != '=');
					output.put(property.substring(0, y).trim().toLowerCase(), property.substring(y+1).trim());
				}
			}
			input.close();
		}catch (Exception e){
			e.printStackTrace();
			crash(file.toString());
		}
		return output;
	}
	
	public static String getHash(String input){
		try{
			MessageDigest algorithm = MessageDigest.getInstance("SHA");
			algorithm.update(input.getBytes(), 0, input.length());
		    return new BigInteger(1, algorithm.digest()).toString(16);
		}catch (Exception e){
			return null;
		}
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
	
	public static void print(String str){
		Window window = getCurrentWindow();
		if (window instanceof GameWindow)
			((GameWindow)window).receiveChat(str, 0);
		java.lang.System.out.println("****  " + str);
	}
	
	public static void startGame(final Game game, final int randomSeed){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				Main.game = game;
				
				for (Player player : game.players){
					if (player instanceof HumanPlayer){
						GameWindow window = new GameWindow((HumanPlayer)player, game.gameSpeed, randomSeed);
						((HumanPlayer)player).setWindow(window);
						addWindow(window);
					}
				}
				
				addWindow(new LoadWindow());
				
				new Thread("StartGameThread"){
					public void run(){
						LoadWindow.awaitLoad();
						game.start(randomSeed);
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
	
	public static String filter(String string, char[] validChars){
		StringBuilder filteredName = new StringBuilder(string);
		int index = 0;
		while (index < filteredName.length()){
			boolean isValid = false;
			for (int x = 0; x < validChars.length; x++)
				isValid = isValid || filteredName.charAt(index) == validChars[x];
			if (isValid){
				index++;
			}else
				filteredName.deleteCharAt(index);
		}
		return filteredName.toString();
	}
	
	public static void main(String[] args){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				new Main();
		}});
	}
	
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
