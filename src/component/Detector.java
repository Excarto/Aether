import javax.swing.JPanel;

public class Detector extends System{
	
	public final DetectorType type;
	static SystemPanel detectorPanel;
	
	public Detector(DetectorType type, Hardpoint hardpoint, Unit unit){
		super(type, hardpoint, unit);
		this.type = type;
	}
	
	public SensorSighting scannedBy(Sensor sensor){
		SensorSighting sighting = null;
		if (engaged){
			double strength = 1-(1-sensor.type.detectability)/type.stealthResistance-unit.distance(sensor.unit)/sensor.type.radius;
			if (strength > 0)
				sighting = new SensorSighting(strength, this, sensor.unit, type.posAccuracy, type.velAccuracy, type.pingLifetime);
		}
		return sighting;
	}
	
	public void initialize(){
		super.initialize();
		
		engaged = true;
	}
	
	public static void createMenu(){
		detectorPanel = new SystemPanel();
	}
	public JPanel getMenu(){
		detectorPanel.setSystem(this);
		return detectorPanel;
	}
	
}
