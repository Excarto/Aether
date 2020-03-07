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
	
	public final int outfittedCost;
	
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
		
		int craftCost = 0;
		int numTypes = 0;
		for (CraftType craftType : Main.craftTypes){
			if (craftType.mass <= this.craftMass){
				numTypes++;
				craftCost += craftType.cost;
			}
		}
		outfittedCost = this.cost + (numTypes > 0 ? craftCost/numTypes : 0);
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
		
		String[][] specs = {
				{"Vision Radius",			String.valueOf(10*(int)round(visionRange/10.0))},
				{"Storage Space",			String.valueOf(storageSpace),
						"Used for both ammunition and repair material"},
				{"Sensor Signature",		String.valueOf(-radarSize),
						"A larger signature means this ship is more easily spotted"},
				{"Capture Rate",			String.valueOf(captureRate*Main.TPS),
						"How quickly the ship can capture mission objectives"},
					{"Armament", "CATEGORY"},
				{"Number of Mounts",		String.valueOf(weaponHardpoints.length)},
				{"Average Mount Mass",		weaponMass},
				{"Average Mount Arc", 		weaponArc},
					{"Defense", "CATEGORY"},
				{"Front Shields",			String.valueOf(frontShield)},
				{"Rear Shields",			String.valueOf(rearShield)},
				{"Shield Recharge (sec.)",	String.valueOf(-(int)((frontShield+rearShield)/2/(shieldRecharge*Main.TPS))),
						"How long it takes down shields to fully recharge"},
				{"Hull",					String.valueOf(hull)},
				{"Armor",					String.valueOf(armor),
						"Armor reduces hull damage from lighter weapons, but helps little against heavier weapons"},
					{"Engines", "CATEGORY"},
				{"Forward Accel. (empty)",	String.valueOf(thrust/mass*Main.TPS*Main.TPS),
						"Acceleration with no weapons, storage, or crafts"},
				{"Forward Accel. (loaded)",	String.valueOf(thrust/maxMass*Main.TPS*Main.TPS),
						"Acceleration with maximum possible mass of weapons, storage, and crafts"},
				{"Maneuvering Accel.",      String.valueOf(turnThrust/((mass+maxMass)/2)*Main.TPS*Main.TPS)},
					{"Energy", 	"CATEGORY"},
				{"Power (Energy/Sec)",		String.valueOf(power*Main.TPS/1000),
						"Capacitor recharge rate"},
				{"Capacitor",				String.valueOf(capacitor/1000)},
					{"Systems", "CATEGORY"},
				{"Number of Mounts",		String.valueOf(systemHardpoints.length)},
				{"Average Mount Mass",		String.valueOf(systemMass)},
					{"Craft", "CATEGORY"},
				{"Craft Bay Size",			String.valueOf(totalCraftMass)},
				{"Maximum Craft Mass",		String.valueOf(craftMass)}
		};
		this.specs = specs;
	}
	
}
