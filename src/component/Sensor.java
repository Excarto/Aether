import javax.swing.*;

public class Sensor extends System{
	
	public final SensorType type;
	private int timeToRefresh;
	private static SensorPanel sensorPanel;
	
	public Sensor(SensorType type, Hardpoint hardpoint, Unit unit){
		super(type, hardpoint, unit);
		this.type = type;
		
		timeToRefresh = 0;
	}
	
	public void act(){
		super.act();
		
		if (engaged){
			if (timeToRefresh > 0)
				timeToRefresh--;
			
			if (timeToRefresh == 0){
				if (unit.drainEnergy(type.refreshEnergy, "Systems", type.name)){
					timeToRefresh = type.refreshPeriod;
					for (Player player : Main.game.players){
						if (player.team != unit.player.team){
							for (Controllable target : player.controllables){
								if (target instanceof Unit){
									SensorSighting sighting = scan((Sprite)target);
									if (sighting != null)
										unit.player.addSensorSighting(sighting, (Sprite)target);
									if (target instanceof Unit)
										((Unit)target).scannedBy(this);
								}
							}
						}
					}
				}
			}
			
		}
	}
	
	protected SensorSighting scan(Sprite target){
		double strength = 1-(1-target.getRadarSize())/type.stealthResistance-unit.distance(target)/type.radius;
		if (strength > 0)
			return new SensorSighting(strength, this, target, type.posAccuracy, type.velAccuracy, type.pingLifetime);
		return null;
	}
	
	public int getTimeToRefresh(){
		return timeToRefresh;
	}
	
	public void initialize(){
		super.initialize();
		
		engaged = true;
		timeToRefresh = type.refreshPeriod;
	}
	
	public static void createMenu(){
		sensorPanel = new SensorPanel();
	}
	public JPanel getMenu(){
		sensorPanel.setSystem(this);
		return sensorPanel;
	}
	
}
