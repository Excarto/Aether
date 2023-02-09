import javax.swing.JPanel;

// Buyable system used to temporarily increase thrust

public class Booster extends System{
	
	private static BoosterPanel boosterPanel;
	
	public final BoosterType type;
	private int fuel;
	
	public Booster(BoosterType type, Hardpoint hardpoint, Unit unit){
		super(type, hardpoint, unit);
		this.type = type;
	}
	
	public void initialize(){
		super.initialize();
		
		engaged = false;
		fuel = type.lifetime;
	}
	
	public void act(){
		super.act();
		
		if (Main.game.turn-unit.accelForwardTime <= 1)
			fuel--;
		if (fuel <= 0)
			setEngaged(false);
	}
	
	public void takeHit(double damage){
		super.takeHit(damage);
		if (getHull() < 0 && fuel < type.lifetime && type.cannotReengage)
			fuel = 0;
	}
	
	public double getThrust(){
		return isActive() ? type.thrust : 0;
	}
	
	public int getFuel(){
		return fuel;
	}
	
	public boolean setEngaged(boolean engaged){
		if (!engaged && fuel < type.lifetime && type.cannotReengage)
			fuel = 0;
		return super.setEngaged(engaged && fuel > 0);
	}
	
	public static void createMenu(){
		boosterPanel = new BoosterPanel();
	}
	public JPanel getMenu(){
		boosterPanel.setSystem(this);
		return boosterPanel;
	}
	
}
