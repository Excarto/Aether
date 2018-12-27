

public class DetectorType extends SystemType{
	
	public final double posAccuracy, velAccuracy;
	public final double stealthResistance;
	public final int pingLifetime;
	
	public DetectorType(String type){
		super (type);
		
		stealthResistance = getDouble("stealth_resistance");
		posAccuracy = getDouble("position_accuracy");
		velAccuracy = getDouble("velocity_accuracy");
		pingLifetime = (int)(getDouble("ping_lifetime")*Main.TPS);
	}
	
	public void genSpecs(){
		specs = new String[][] {
				{"Stealth Resistance",		String.valueOf(stealthResistance)},
				//{"Range",					String.valueOf(radius)},
				{"Position Accuracy",		String.valueOf(posAccuracy)},
				{"Velocity Accuracy",		String.valueOf(velAccuracy)},
					{"Resources", "CATEGORY"},
				{"Energy Use (Per Second)",	String.valueOf(-getAveragePowerUse()*Main.TPS/1000)},
				{"Energy To Engage",		String.valueOf(-engageEnergy/1000)},
				{"Required Mass",			String.valueOf(-mass)}
		};
	}
}
