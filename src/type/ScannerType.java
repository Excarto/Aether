
public class ScannerType extends SystemType{
	
	public ScannerType (String type){
		super (type);
		
	}
	
	public void genSpecs(){
		specs = new String[][] {
				{"Range",					String.valueOf(radius)},
					{"Resources", "CATEGORY"},
				{"Energy Use (Per Second)",	String.valueOf(-getAveragePowerUse()*Main.TPS/1000)},
				{"Energy To Engage",		String.valueOf(-engageEnergy/1000)},
				{"Required Mass",			String.valueOf(-mass)}
		};
	}
}
