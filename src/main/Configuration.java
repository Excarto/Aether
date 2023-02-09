import java.util.*;
import java.io.*;

// Contains game configuration values that are read in from text file, but cannot be configured in-game

public class Configuration{
	
	public final double kineticShieldDamage;
	public final double explosiveShieldDamage;
	public final double energyPerThrust;
	public final double energyPerTurnThrust;
	public final double energyPerShield;
	public final double armorExplosiveEffectiveness;
	public final double armorKineticEffectiveness;
	public final double explosiveSubtargetChance;
	public final double explosiveComponentChance;
	public final double explosiveComponentDamage;
	public final double kineticSubtargetChance;
	public final double kineticComponentChance;
	public final double kineticComponentDamage;
	public final double EMComponentChance;
	public final int targetAccelTimeframe;
	public final double debrisVisionSize;
	public final double unitRenderAngle;
	public final double craftDockDistance;
	public final double craftDockSpeed;
	public final double repairDistance;
	public final double repairSpeed;
	public final double captureDistance;
	public final double captureSpeed;
	public final double maneuverThrust;
	public final double massPerMaterial;
	public final double scrapReturn;
	public final double maxComponentDamage;
	public final double maxScrapDamage;
	public final double explosionImpulsePerMass, impactImpulsePerDamage;
	public final int netAdjustTime;
	public final double unitAccelMultiplier, unitTurnAccelMultiplier;
	public final double unitVisionMultiplier;
	public final double systemRangeMultiplier;
	
	private final String file;
	
	public Configuration(String file){
		this.file = file;
		Map<String, String> data = null;
		try{
			data = Utility.readDataFile(file);
		}catch (IOException ex){
			Main.crash(file.toString());
		}
		
		debrisVisionSize = getDouble(data, "debris_vision_size");
		unitRenderAngle = getDouble(data, "unit_render_angle");
		kineticShieldDamage = getDouble(data, "kinetic_shield_damage");
		explosiveShieldDamage = getDouble(data, "explosive_shield_damage");
		unitVisionMultiplier = getDouble(data, "unit_vision_multiplier");
		unitAccelMultiplier = getDouble(data, "unit_accel_multiplier");
		unitTurnAccelMultiplier = getDouble(data, "unit_turn_accel_multiplier");
		energyPerThrust = getDouble(data, "energy_per_thrust_second")/Main.TPS/unitAccelMultiplier;
		energyPerTurnThrust = getDouble(data, "energy_per_turn_thrust_second")/Main.TPS/unitTurnAccelMultiplier;
		energyPerShield = getDouble(data, "energy_per_shield");
		targetAccelTimeframe = (int)(getDouble(data, "targeting_accel_timeframe")*Main.TPS);
		armorExplosiveEffectiveness = getDouble(data, "armor_explosive_effectiveness");
		armorKineticEffectiveness = getDouble(data, "armor_kinetic_effectiveness");
		explosiveSubtargetChance = getDouble(data, "explosive_hit_subtarget_chance");
		explosiveComponentChance = getDouble(data, "explosive_hit_component_chance");
		explosiveComponentDamage = getDouble(data, "explosive_hit_component_max_damage");
		kineticSubtargetChance = getDouble(data, "kinetic_hit_subtarget_chance");
		kineticComponentChance = getDouble(data, "kinetic_hit_component_chance");
		kineticComponentDamage = getDouble(data, "kinetic_hit_component_max_damage");
		EMComponentChance = getDouble(data, "em_hit_component_chance");
		craftDockDistance = getDouble(data, "craft_dock_distance");
		craftDockSpeed = getDouble(data, "craft_dock_speed")/Main.TPS;
		repairDistance = getDouble(data, "repair_distance");
		repairSpeed = getDouble(data, "repair_speed")/Main.TPS;
		captureDistance = getDouble(data, "capture_distance");
		captureSpeed = getDouble(data, "capture_speed")/Main.TPS;
		maneuverThrust = getDouble(data, "maneuver_thrust");
		massPerMaterial = getDouble(data, "mass_per_repair_material");
		scrapReturn = getDouble(data, "scrap_return");
		maxComponentDamage = getDouble(data, "max_component_damage");
		maxScrapDamage = getDouble(data, "max_scrap_damage");
		explosionImpulsePerMass = getDouble(data, "explosion_impulse_per_mass")/Main.TPS;
		impactImpulsePerDamage = getDouble(data, "impact_impulse_per_damage")/Main.TPS;
		netAdjustTime = (int)(getDouble(data, "net_update_adjust_time")*Main.TPS);
		systemRangeMultiplier = getDouble(data, "system_range_multiplier");
	
		Main.appendData(data, "");
	}
	
	private double getDouble(Map<String, String> data, String label){
		try{
			return Double.valueOf(data.get(label));
		}catch (Exception e){}
		Main.crash(file + "  " + label);
		return 0;
	}
}
