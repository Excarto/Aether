import static java.lang.Math.*;

// Position for mounting weapons or systems. Each UnitType has some number of these associated.

public class Hardpoint{
	public static final double AUTO_RANDOMNESS = 1.2;
	
	public final double posX;
	public final double posY;
	public final int mass;
	
	private final double centerDistance;
	private final double centerAngle;
	private final double posYShift;
	
	public Hardpoint(double posX, double posY, double posZ, int mass){
		this.posX = posX;
		this.posY = posY;
		this.mass = mass;
		
		centerDistance = hypot(posX-0.5, posY-0.5);
		centerAngle = toDegrees(atan2(posY-0.5, posX-0.5))-90;
		
		posYShift = -posZ/2*sin(toRadians(Main.config.unitRenderAngle));
	}
	
	public final double getRotatedPosX(double angle){
		return 0.5-centerDistance*sin(toRadians(centerAngle+angle));
	}
	public final double getRotatedPosY(double angle){
		return 0.5+centerDistance*cos(toRadians(centerAngle+angle)) + posYShift;
	}
	
	// Randomly pick a compatible Component, weighted by mass
	public ComponentType autoLoadout(Unit unit){
		double highestVal = 0;
		ComponentType highestType = null;
		for (ComponentType type : Main.systemTypes){
			if (type.mass <= mass){
				double val = sqrt(type.mass)*(random()+1/AUTO_RANDOMNESS);
				if (val > highestVal){
					highestVal = val;
					highestType = type;
				}
			}
		}
		for (Component component : unit){
			if (component.type == highestType)
				return null;
		}
		return highestType;
	}
}
