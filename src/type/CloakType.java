

public class CloakType extends SystemType{
	
	public final double effect;
	
	public CloakType(String type){
		super(type);
		
		effect = getDouble("effectiveness");
	}
	
	public void genSpecs(){
		specs = new String[][] {
				{"Strength",				String.valueOf(effect)},
				{"Range",					String.valueOf(radius)},
					{"Resources", "CATEGORY"},
				{"Energy Use (Per Second)",	String.valueOf(-powerUse*Main.TPS/1000)},
				{"Energy To Engage",		String.valueOf(-engageEnergy/1000)},
				{"Required Mass",			String.valueOf(-mass)}
		};
	}
}
