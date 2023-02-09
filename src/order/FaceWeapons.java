import static java.lang.Math.*;
import java.util.*;

// Order to turn unit to face maximum firepower toward targets, used as suborder by many other orders

public class FaceWeapons extends Order{
	static final int MIN_NUM_ANGLES = 2;
	public static final double NO_ANGLE = 1000;
	
	ArrayList<Double> arcAngle, arcWeight;
	double previousAngle, previousLead;
	double leadFactor;
	
	public FaceWeapons(){
		arcAngle = new ArrayList<Double>(16);
		arcWeight = new ArrayList<Double>(16);
		previousAngle = NO_ANGLE;
		leadFactor = 2.0;
	}
	
	public void act(){
		Unit host = (Unit)this.host;
		double turnSpeed = host.getTurnSpeed();
		double stopDist = signum(turnSpeed)*turnSpeed*turnSpeed/(2*host.getTurnAccel());
		
		if (arcAngle.size() <= MIN_NUM_ANGLES || abs(stopDist) > 100)
			host.stopTurn();
		
		if (runTime%5 == 0){
			double angle = getFaceAngle(null);
			double lead = 0.0;
			if (angle != NO_ANGLE){
				if (previousAngle != NO_ANGLE && abs(Utility.fixAngle(angle - host.getAngle())) < 40){
					// Numerically compute difference between target angle and true angle between turns, then compute lead term to try to compensate
					// Otherwise it will lag behind the target angle
					double targetAngleRate = Utility.fixAngle(angle - previousAngle);
					double turnLag = Utility.fixAngle(previousAngle + previousLead - host.getAngle());
					if (signum(turnLag) != signum(targetAngleRate))
						turnLag = 0.0;
					turnLag = Utility.clamp(turnLag, abs(32*targetAngleRate));
					lead = Utility.clamp(turnLag + targetAngleRate, 10.0);
					//lead = Utility.clamp(15.0*(angle-Game.centerAbout(previousAngle, angle)), 6.0);
				}
				host.orders().stackOrder(new TurnTo(angle+lead), this);
			}
			previousAngle = angle;
			previousLead = lead;
		}
	}
	
	public double getFaceAngle(Target target){
		Unit host = (Unit)this.host;
		double turnSpeed = host.getTurnSpeed();
		double stopDist = signum(turnSpeed)*turnSpeed*turnSpeed/(2*host.getTurnAccel());
		
		arcAngle.clear();
		arcWeight.clear();
		
		// Prefer the direction currently facing
		arcAngle.add(stopDist-90.0);
		arcWeight.add(0.1);
		arcAngle.add(stopDist+90.0);
		arcWeight.add(-0.1);
		
		// Compute all starting and ending angles of weapon arcs weighted by importance
		for (Weapon weapon : host.weapons){
			if (weapon.getHull() > 0 && !Double.isNaN(weapon.getTargetAngle()) && weapon.getArc() < 180 &&
					(target == null || weapon.getTarget() == target)){
				double startAngle = Utility.fixAngle(weapon.getTargetAngle()-weapon.getMountAngle()-weapon.getArc());
				double endAngle = Utility.fixAngle(weapon.getTargetAngle()-weapon.getMountAngle()+weapon.getArc());
				double weight = weapon.getMass();
				if (weapon.type instanceof MissileType)
					weight /= 8;
				if (weapon.getFireMode() == Weapon.FireMode.AUTONOMOUS)
					weight /= 2;
				if (!weapon.isInRange())
					weight /= 4;
				
				for (int x = 0; x <= arcAngle.size(); x++){
					if (x == arcAngle.size() || startAngle <= arcAngle.get(x)){
						arcAngle.add(x, startAngle);
						arcWeight.add(x, weight);
						break;
					}
				}
				for (int x = 0; x <= arcAngle.size(); x++){
					if (x == arcAngle.size() || endAngle <= arcAngle.get(x)){
						arcAngle.add(x, endAngle);
						arcWeight.add(x, -weight);
						break;
					}
				}
			}
		}
		
		// Find angle segment with greatest sum of arc weights, and set face angle to midpoint of segment
		if (arcAngle.size() > MIN_NUM_ANGLES){
			double highestStart = 0, highestEnd = 0;
			double currentWeight = 0;
			double highestWeight = Double.NEGATIVE_INFINITY;
			for (int x = 0; x <= arcAngle.size(); x++){
				double angle, weight;
				if (x < arcAngle.size()){
					angle = arcAngle.get(x);
					weight = arcWeight.get(x);
				}else{
					weight = arcWeight.get(0);
					if (weight < 0.0){
						angle = arcAngle.get(0)+360.0;
					}else
						break;
				}
				
				if (currentWeight+weight >= highestWeight){
					highestWeight = currentWeight+weight;
					highestStart = angle;
				}else if (currentWeight == highestWeight){
					highestEnd = angle;
				}
				currentWeight += weight;
			}
			return host.getAngle()+(highestStart+highestEnd)/2;
		}else
			return NO_ANGLE;
	}
	
}
