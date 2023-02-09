import java.util.*;
import java.io.*;

// Reads in, writes, and stores user-configurable options

public class Options{
	public final int resX, resY;
	public final boolean useAntiAliasing;
	public final int scalingQuality;
	public final int framesPerSec;
	public final int menuFramesPerSec;
	public final double zoomRatio;
	public final double moveZoomRatio;
	public final int zoomLevels;
	public final double renderSizeScaling;
	public final int renderAnglesMultiplier;
	public final int audioChannels;
	public final double scrollSpeed;
	public final double accelRate;
	public final double cameraMoveMultiplier;
	public final int defaultAutoRepair;
	public final boolean repairAutoFinish;
	public final double masterVolume;
	public final double musicVolume;
	public final int maxPointerLineLength;
	public final int statusSize;
	public final int targetFadeTime;
	public final double thrustScale;
	public final double debrisAmount;
	public final double debrisRenderAnglesMultiplier;
	public final int clientPort;
	public final int serverPort;
	public final int UDPPort;
	public final int serverBroadcastPort, clientBroadcastPort;
	public final int lobbyHostPort, lobbyClientPort;
	public final String lobbyServer;
	public final boolean forceTCP;
	public final boolean UPnPEnabled;
	public final String username;
	public final boolean fullscreen;
	public final boolean borderless;
	public final boolean useHardwareAccel;
	public final String explodingShipExplosion;
	public final double soundFalloffRate;
	public final double minSoundVolume;
	public final int difficulty;
	
	public final String file;
	
	public Options(String file){
		this.file = file;
		Map<String, String> data = null;
		try{
			data = Utility.readDataFile(file);
		}catch (IOException ex){
			Main.crash(file.toString());
		}
		
		String[] screenSize = getString(data, "screen_size").split("x");
		resX = Integer.valueOf(screenSize[0]);
		resY = Integer.valueOf(screenSize[1]);
		
		String windowMode = getString(data, "window_mode");
		if (windowMode.equals("fullscreen")){
			fullscreen = true;
			borderless = true;
		}else if (windowMode.equals("window")){
			fullscreen = false;
			borderless = true;
		}else if (windowMode.equals("bordered")){
			fullscreen = false;
			borderless = false;
		}else{
			fullscreen = false;
			borderless = true;
			Main.crash(file + " " + "window_mode");
		}
		
		scalingQuality = getInt(data, "scaling_quality");
		useAntiAliasing = getBoolean(data, "antialiasing");
		masterVolume = (float)getDouble(data, "master_volume");
		musicVolume = (float)getDouble(data, "music_volume");
		framesPerSec = getInt(data, "frames_per_sec");
		menuFramesPerSec = getInt(data, "menu_frames_per_sec");
		debrisAmount = getDouble(data, "debris_amount");
		zoomRatio = getDouble(data, "zoom_ratio");
		moveZoomRatio = getDouble(data, "camera_move_zoom_ratio");
		zoomLevels = getInt(data, "zoom_levels");
		renderSizeScaling = getDouble(data, "render_scaling");
		renderAnglesMultiplier = getInt(data, "render_angles_multiplier");
		audioChannels = getInt(data, "audio_channels");
		scrollSpeed = getDouble(data, "camera_scroll_speed")/Main.TPS;
		accelRate = getDouble(data, "camera_accel_rate")/Main.TPS/Main.TPS;
		cameraMoveMultiplier = getDouble(data, "camera_move_multiplier");
		maxPointerLineLength = getInt(data, "max_pointer_line_length");
		defaultAutoRepair = getInt(data, "default_auto_repair");
		repairAutoFinish = getBoolean(data, "repair_auto_finish");
		statusSize = getInt(data, "unit_status_size");
		targetFadeTime = getInt(data, "target_fade_time")*Main.TPS;
		thrustScale = getDouble(data, "thrust_scale")/Main.TPS/Main.TPS;
		debrisRenderAnglesMultiplier = getDouble(data, "debris_render_angles_multiplier");
		clientPort = getInt(data, "client_port");
		serverPort = getInt(data, "server_listen_port");
		UDPPort = getInt(data, "udp_port");
		serverBroadcastPort = getInt(data, "server_broadcast_port");
		clientBroadcastPort = serverBroadcastPort;
		lobbyHostPort = getInt(data, "lobby_host_port");
		lobbyClientPort = getInt(data, "lobby_client_port");
		lobbyServer = getString(data, "lobby_server");
		forceTCP = getBoolean(data, "force_tcp");
		UPnPEnabled = getBoolean(data, "enable_upnp");
		username = getString(data, "username");
		useHardwareAccel = getBoolean(data, "use_hardware_accel");
		explodingShipExplosion = getString(data, "death_sequence_explosion");
		soundFalloffRate = getDouble(data, "sound_falloff_multiplier")/(0.5*Main.RES_X_NARROW);
		minSoundVolume = getDouble(data, "min_sound_volume");
		difficulty = getInt(data, "difficulty");
	}
	
	private int getInt(Map<String, String> data, String label){
		try{
			return Integer.valueOf(data.get(label));
		}catch (Exception e){}
		Main.crash(file + "  " + label);
		return 0;
	}
	
	private double getDouble(Map<String, String> data, String label){
		try{
			return Double.valueOf(data.get(label));
		}catch (Exception e){}
		Main.crash(file + "  " + label);
		return 0;
	}
	
	private boolean getBoolean(Map<String, String> data, String label){
		try{
			return Boolean.valueOf(data.get(label));
		}catch (Exception e){}
		Main.crash(file + "  " + label);
		return false;
	}
	
	private String getString(Map<String, String> data, String label){
		try{
			if (data.containsKey(label))
				return data.get(label);
		}catch (Exception e){}
		Main.crash(file + "  " + label);
		return null;
	}
}
