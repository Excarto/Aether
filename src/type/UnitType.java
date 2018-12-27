import static java.lang.Math.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.*;
import org.imgscalr.*;

public abstract class UnitType extends BuyType{
	static final int NUM_CONTACT_MAPS = 36;
	public static final int ICON_SIZE = 38;
	
	public static int maxNumRenderAngles = 0;
	
	public final Renderable renderable;
	public final boolean[][][] contactMap;
	public final BufferedImage topImg;
	
	public final double scale;
	public final int iconSize;
	public final double capacitor;
	public final double power;
	public final double armor;
	public final double thrust;
	public final double turnThrust;
	public final int visionRange;
	public final double radarSize;
	public final double storageSpace;
	public final double captureRate;
	public final double contactScale;
	public final WeaponHardpoint[] weaponHardpoints;
	public final Hardpoint[] systemHardpoints;
	public final Thruster[][] thrusters;
	public final String iconLabel;
	public final int iconLabelWidth;
	public final double deathExplosionImpulse;
	
	public BufferedImage icon;
	
	private final String folder;
	
	public UnitType(String type, String folder){
		super(type, folder);
		
		scale = getDouble("scale");
		iconSize = getInt("icon_size");
		capacitor = getDouble("capacitor");
		power = getDouble("power")/Main.TPS;
		armor = getDouble("armor");
		thrust = getDouble("forward_thrust")*Main.unitAccelMultiplier/Main.TPS/Main.TPS;
		turnThrust = getDouble("turning_thrust")*Main.unitTurnAccelMultiplier/Main.TPS/Main.TPS;
		visionRange = (int)(getInt("vision_size")*Main.unitVisionMultiplier);
		radarSize = getDouble("radar_signature");
		storageSpace = getDouble("storage_space");
		captureRate = getDouble("capture_rate")/Main.TPS;
		contactScale = getDouble("contact_scale") > 0 ? getDouble("contact_scale") : 1.0;
		iconLabel = getString("icon_label");
		deathExplosionImpulse = mass*Main.explosionImpulsePerMass;
		
		iconLabelWidth = Main.getStringWidth(iconLabel, Target.UNIT_LABEL_FONT);
		
		int hardpointCount = 0;
		while (hasValue("weapon" + ++hardpointCount + "_mass"));
		weaponHardpoints = new WeaponHardpoint[hardpointCount-1];
		for (int x = 0; x < weaponHardpoints.length; x++){
			int index = x+1;
			weaponHardpoints[x] = new WeaponHardpoint(
					0.5+getDouble("weapon" + index + "_x_pos")/2,
					0.5-getDouble("weapon" + index + "_y_pos")/2,
					getDouble("weapon" + index + "_z_pos")/2,
					getInt("weapon" + index + "_mass"),
					getInt("weapon" + index + "_angle"),
					getInt("weapon" + index + "_arc"));
		}
		
		hardpointCount = 0;
		while (hasValue("system" + ++hardpointCount + "_mass"));
		systemHardpoints = new Hardpoint[hardpointCount-1];
		for (int x = 0; x < systemHardpoints.length; x++){
			int index = x+1;
			systemHardpoints[x] = new Hardpoint(
					0.5+getDouble("system" + index + "_x_pos")/2,
					0.5-getDouble("system" + index + "_y_pos")/2,
					0.0,
					getInt("system" + index + "_mass"));
		}
		
		thrusters = new Thruster[3][];
		
		int thrusterCount = 0;
		double scaleSum = 0.0;
		while (hasValue("thruster" + (thrusterCount+1) + "_type")){
			thrusterCount++;
			scaleSum += getDouble("thruster" + thrusterCount + "_scale");
		}
		thrusters[Thruster.FORWARD] = new Thruster[thrusterCount];
		for (int x = 1; x <= thrusterCount; x++){
			thrusters[Thruster.FORWARD][x-1] = new Thruster(getInt("thruster" + x + "_type")-1,
					getDouble("thruster" + x + "_x_pos"),
					-getDouble("thruster" + x + "_y_pos"),
					getDouble("thruster" + x + "_z_pos"),
					getDouble("thruster" + x + "_angle_offset"),
					getDouble("thruster" + x + "_z_angle"),
					Thruster.FORWARD,
					getInt("thruster" + x + "_z_order"),
					thrust*getDouble("thruster" + x + "_scale")/scaleSum,
					Main.unitRenderAngle);
		}
		
		for (int direction : new int[]{Thruster.LEFT, Thruster.RIGHT}){
			String label = (direction == Thruster.LEFT ? "left" : "right") + "_thruster";
			thrusterCount = 0;
			//double momentAvg = 0.0;
			while (hasValue(label + (thrusterCount+1) + "_type")){
				thrusterCount++;
			//	momentAvg += hypot(getDouble(label + thrusterCount + "_x_pos"), getDouble(label + thrusterCount + "_y_pos"));
			}
			//momentAvg /= thrusterCount;
			thrusters[direction] = new Thruster[thrusterCount];
			for (int x = 1; x <= thrusterCount; x++){
				//double moment = hypot(getDouble(label + thrusterCount + "_x_pos"), getDouble(label + thrusterCount + "_y_pos"));
				thrusters[direction][x-1] = new Thruster(getInt(label + x + "_type")-1,
						getDouble(label + x + "_x_pos"),
						-getDouble(label + x + "_y_pos"),
						getDouble(label + x + "_z_pos"),
						getDouble(label + x + "_angle_offset"),
						getDouble(label + x + "_z_angle"),
						direction,
						getInt(label + x + "_z_order"),
						turnThrust*/*(momentAvg/moment)**/(Main.energyPerTurnThrust/Main.energyPerThrust)/thrusterCount,
						Main.unitRenderAngle);
			}
		}
		
		BufferedImage temp = null;
		File file = new File("data/" + folder + "/" + type + "/top.png");
		try{
			temp = ImageIO.read(file);
		}catch(IOException e){
			Main.crash(file.getPath());
		}
		topImg = temp;
		
		int numRenderAngles = Main.renderAnglesMultiplier*getInt("num_render_angles");
		maxNumRenderAngles = max(maxNumRenderAngles, numRenderAngles);
		renderable = new Renderable(iconSize+1, numRenderAngles);
		contactMap = new boolean[NUM_CONTACT_MAPS][][];
		this.folder = folder;
	}
	
	public void load(){
		super.load();
		
		icon = Scalr.resize(perspectiveImg, org.imgscalr.Scalr.Method.ULTRA_QUALITY, ICON_SIZE, ICON_SIZE);
		
		Set<File> imageFiles = new TreeSet<File>();
		for (File file : new File("data/" + folder + "/" + name + "/renders/").listFiles()){
			if (file.getName().endsWith(".png"))
				imageFiles.add(file);
		}
		BufferedImage[] inputImg = new BufferedImage[imageFiles.size()];
		Iterator<File> fileIterator = imageFiles.iterator();
		for (int x = 0; x < inputImg.length; x++){
			File file = fileIterator.next();
			try{
				inputImg[x] = ImageIO.read(file);
			}catch(IOException e){
				Main.crash(file.getPath());
			}
		}
		renderable.load(inputImg, scale);
		
		for (int x = 0; x < contactMap.length; x++)
			contactMap[x] = renderable.getContactMap(x*360.0/NUM_CONTACT_MAPS, contactScale);
		
		for (int x = 0; x < thrusters.length; x++){
			for (Thruster thruster : thrusters[x])
				thruster.setHostSize(renderable.size);
		}
	}
}
