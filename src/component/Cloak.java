import javax.swing.*;

// Buyable system used to reduce radar detection range
public class Cloak extends System {
	
	private static SystemPanel cloakPanel;
	
	public final CloakType type;
	
	public Cloak(CloakType type, Hardpoint hardpoint, Unit unit) {
		super(type, hardpoint, unit);
		this.type = type;
	}
	
	public void initialize(){
		super.initialize();
		
		engaged = true;
	}
	
	public static void createMenu(){
		cloakPanel = new SystemPanel();
	}
	public JPanel getMenu(){
		cloakPanel.setSystem(this);
		return cloakPanel;
	}
}
