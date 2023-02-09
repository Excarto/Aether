import javax.swing.JPanel;

// Buyable system used to reveal enemy unit status

public class Scanner extends System{
	
	public final ScannerType type;
	static SystemPanel scannerPanel;
	
	public Scanner(ScannerType type, Hardpoint hardpoint, Unit unit){
		super(type, hardpoint, unit);
		this.type = type;
	}
	
	public void initialize(){
		super.initialize();
		
		engaged = true;
	}
	
	public boolean isScanned(Unit target){
		return isActive() && unit.distance(target) < type.radius;
	}
	
	public static void createMenu(){
		scannerPanel = new SystemPanel();
	}
	public JPanel getMenu(){
		scannerPanel.setSystem(this);
		return scannerPanel;
	}
	
}
