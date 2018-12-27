

public class SensorType extends SystemType{
	
	public final double detectability;
	public final double stealthResistance;
	public final double refreshEnergy;
	public final int refreshPeriod, pingLifetime;
	public final double posAccuracy, velAccuracy;
	
	public SensorType(String type){
		super(type);
		
		detectability = getDouble("detectability");
		stealthResistance = getDouble("stealth_resistance");
		refreshEnergy = getDouble("energy_per_refresh");
		refreshPeriod = (int)(getDouble("refresh_time")*Main.TPS);
		pingLifetime = (int)(getDouble("ping_lifetime")*Main.TPS);
		posAccuracy = getDouble("position_accuracy");
		velAccuracy = getDouble("velocity_accuracy");
	}
	
	public double getAveragePowerUse(){
		return refreshEnergy/refreshPeriod + super.getAveragePowerUse();
	}
	
	public void genSpecs(){
		specs = new String[][]{
				{"Range",					String.valueOf(radius)},
				{"Position Accuracy",		String.valueOf(posAccuracy)},
				{"Velocity Accuracy",		String.valueOf(velAccuracy)},
				{"Stealth Resistance",		String.valueOf(stealthResistance)},
				{"Refresh Rate (Hz)",		String.valueOf((double)Main.TPS/refreshPeriod)},
				{"Detectability",			String.valueOf(-detectability)},
					{"Resources", "CATEGORY"},
				{"Energy Use (Per Second)",	String.valueOf(-getAveragePowerUse()*Main.TPS/1000)},
				{"Energy To Engage",		String.valueOf(-engageEnergy/1000)},
				{"Required Mass",			String.valueOf(-mass)}
		};
	}
}
