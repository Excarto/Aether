import static java.lang.Math.*;

public final class ShipType extends UnitType{
	
	public static int maxShipSize = 0;
	public static int minDeathTime = Integer.MAX_VALUE;
	
	public final double frontShield;
	public final double rearShield;
	public final double shieldRecharge;
	public final int totalCraftMass;
	public final double craftMass;
	public final int craftLaunchTime;
	public final double craftLaunchSpeed;
	public final double craftRepairRate;
	public final double craftRechargeRate;
	public final double repairRate;
	public final double selfRepairRate;
	public final double ammoTransferTimePerMass;
	public final int deathTime;
	
	public ShipType (String type){
		super(type, "ships");
		
		frontShield = getDouble("front_shield");
		rearShield = getDouble("rear_shield");
		shieldRecharge = getDouble("shield_recharge")/Main.TPS;
		totalCraftMass = getInt("total_craft_mass");
		craftMass = getDouble("craft_mass");
		craftLaunchTime = (int)(getDouble("craft_launch_time")*Main.TPS);
		craftLaunchSpeed = getDouble("craft_launch_speed")/Main.TPS;
		craftRepairRate = getDouble("craft_repair_rate")/Main.TPS;
		craftRechargeRate = getDouble("craft_recharge_rate")/Main.TPS;
		repairRate = getDouble("repair_rate")/Main.TPS;
		selfRepairRate = getDouble("self_repair_rate")/Main.TPS;
		ammoTransferTimePerMass = 1.0/(getDouble("ammo_transfer_rate")/Main.TPS);
		deathTime = (int)(getDouble("death_time")*Main.TPS);
		
		minDeathTime = min(minDeathTime, deathTime);
	}
	
	public CraftType autoLoadoutCraft(){
		CraftType type = null;
		double highestVal = 0.0;
		for (int x = 0; x < Main.craftTypes.length; x++){
			if (Main.craftTypes[x].mass <= craftMass){
				double val = random()*pow(Main.craftTypes[x].mass, 3.0);
				if (val > highestVal){
					type = Main.craftTypes[x];
					highestVal = val;
				}
			}
		}
		return type;
	}
	
	public void load(){
		super.load();
		
		maxShipSize = max(maxShipSize, renderable.size);
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
		
		String[][] temp ={
				{"Vision Radius",			String.valueOf(visionRange)},
				{"Storage Space",			String.valueOf(storageSpace)},
				{"Sensor Signature",		String.valueOf(-radarSize)},
				{"Capture Rate",			String.valueOf(captureRate*Main.TPS)},
					{"Armament", "CATEGORY"},
				{"Number of Mounts",		String.valueOf(weaponHardpoints.length)},
				{"Average Mount Mass",		weaponMass},
				{"Average Mount Arc", 		weaponArc},
					{"Defense", "CATEGORY"},
				{"Front Shields",			String.valueOf(frontShield)},
				{"Rear Shields",			String.valueOf(rearShield)},
				{"Shield Recharge (s)",		String.valueOf(-(frontShield+rearShield)/2/(shieldRecharge*Main.TPS))},
				{"Hull",					String.valueOf(hull)},
				{"Armor",					String.valueOf(armor)},
					{"Engines", "CATEGORY"},
				{"Forward Accel. (empty)",	String.valueOf(thrust/mass*Main.TPS*Main.TPS)},
				{"Forward Accel. (loaded)",	String.valueOf(thrust/maxMass*Main.TPS*Main.TPS)},
				{"Maneuvering Accel.",      String.valueOf(turnThrust/((mass+maxMass)/2)*Main.TPS*Main.TPS)},
					{"Energy", 	"CATEGORY"},
				{"Power (Energy/Sec)",		String.valueOf(power*Main.TPS/1000)},
				{"Capacitor",				String.valueOf(capacitor/1000)},
					{"Systems", "CATEGORY"},
				{"Number of Mounts",		String.valueOf(systemHardpoints.length)},
				{"Average Mount Mass",		String.valueOf(systemMass)}
				
		};
		//if (totalCraftMass > 0){
			String[][] temp2 ={
						{"Craft", "CATEGORY"},
					{"Craft Bay Size",		String.valueOf(totalCraftMass)},
					{"Maximum Craft Mass",	String.valueOf(craftMass)}};
					//{"Launch Rate (Per Minute)",String.valueOf(1/craftLaunchTime/Main.TPS/60)}};
			
			String[][] temp3 = new String[temp.length+temp2.length][2];
			for (int x = 0; x < temp.length; x++)
				temp3[x] = temp[x];
			for (int x = temp.length; x < temp.length+temp2.length; x++)
				temp3[x] = temp2[x-temp.length];
			temp = temp3;
		//}
		
		specs = temp;
	}
}
