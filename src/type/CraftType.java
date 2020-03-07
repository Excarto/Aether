import static java.lang.Math.*;
import java.io.*;

public final class CraftType extends UnitType{
	
	public static double smallestCraftMass = Double.MAX_VALUE;
	public final Sound launchSound, dockSound;
	public final double launchTimeMultiplier;
	
	public CraftType(String type){
		super(type, "crafts");
		
		launchSound = new Sound(new File("data/crafts/" + type + "/launch.wav"));
		dockSound = new Sound(new File("data/crafts/" + type + "/dock.wav"));
		
		launchTimeMultiplier = getDouble("launch_time_multiplier");
		
		smallestCraftMass = min(smallestCraftMass, mass);
	}
	
	public void load(){
		super.load();
		launchSound.load();
		dockSound.load();
	}
	
	protected void genSpecs(){
		double maxMass = mass+storageSpace;
		String weaponMass, weaponArc;
		if (weaponHardpoints.length > 0){
			int weaponMassInt = 0;
			for (Hardpoint hardpoint : weaponHardpoints)
				weaponMassInt += hardpoint.mass;
			weaponMass = String.valueOf(weaponMassInt/weaponHardpoints.length);
			maxMass += weaponMassInt;
			int wpnArc = 0;
			for (WeaponHardpoint hardpoint : weaponHardpoints)
				wpnArc += hardpoint.arc*2;
			weaponArc = String.valueOf(wpnArc/weaponHardpoints.length);
		}else
			weaponMass = weaponArc = "0";
		
		String systemMass;
		if (systemHardpoints.length > 0){
			int systemMassInt = 0;
			for (Hardpoint hardpoint : systemHardpoints)
				systemMassInt += hardpoint.mass;
			systemMass = String.valueOf(systemMassInt/systemHardpoints.length);
			maxMass += systemMassInt;
		}else
			systemMass = "0";
		
		specs = new String[][] {
				{"Vision Radius",			String.valueOf(10*(int)round(visionRange/10.0))},
				{"Sensor Signature",		String.valueOf(-radarSize)},
				{"Capture Rate",			String.valueOf(captureRate*Main.TPS)},
					{"Armament", "CATEGORY"},
				{"Number of Mounts",		String.valueOf(weaponHardpoints.length)},
				{"Average Mount Mass",		weaponMass},
				{"Average Mount Arc", 		weaponArc},
				{"Ammo Storage",			String.valueOf(storageSpace)},
					{"Defense", "CATEGORY"},
				{"Hull",					String.valueOf(hull)},
				{"Armor",					String.valueOf(armor)},
					{"Engines", "CATEGORY"},
				{"Forward Accel. (empty)",	String.valueOf(thrust/mass*Main.TPS*Main.TPS)},
				{"Sustained Forward Accel.",String.valueOf(min(power/Main.config.energyPerThrust, thrust)/mass*Main.TPS*Main.TPS)},
				{"Forward Accel. (loaded)",	String.valueOf(thrust/maxMass*Main.TPS*Main.TPS)},
				{"Manuevering Accel.",      String.valueOf(turnThrust/((mass+maxMass)/2)*Main.TPS*Main.TPS)},
					{"Energy", 	"CATEGORY"},
				{"Capacitor",				String.valueOf(capacitor/1000)},
				{"Power (Energy/Sec)",		String.valueOf(power*Main.TPS/1000)},
					{"Systems", "CATEGORY"},
				{"Number of Mounts",		String.valueOf(systemHardpoints.length)},
				{"Average Mount Mass",		String.valueOf(systemMass)}
		};
	}
}
