import static java.lang.Math.*;
import java.awt.*;

// Order for missile to impact target. Will adjust course to move past target while conserving
// an amount of fuel determined by the MissileType. Once close enough, will switch to a
// MovePastTarget order for final approach

public class Impact extends TrackOrder{
	
	public final Target target;
	final MovePastTarget finalApproachOrder;
	boolean finalApproach;
	int turnsSetVars;
	int timeToAccel;
	double targetAngle;
	double distance, closingSpeed;
	
	public Impact(Target target){
		super(target);
		this.target = target;
		finalApproachOrder = new MovePastTarget(target);
		finalApproach = false;
	}
	
	public void setHost(Controllable host){
		super.setHost(host);

		finalApproachOrder.setHost(host);
	}
	
	public void act(){
		if (!finalApproach){
			if (turnsSetVars++%10 == 0){
				// Check if enough fuel to execute MovePastTarget order, otherwise continue initial approach
				finalApproachOrder.setTimeAndAngle();
				if (finalApproachOrder.getTime()*((Missile)host).type.thrust*Main.config.energyPerThrust <
						0.95*((Missile)host).getEnergy()){
					finalApproach = true;
					((Controllable)host).orders().stackOrder(finalApproachOrder, this);
				}else{
					setTimeAndAngle();
					if (timeToAccel > 5)
						((Controllable)host).orders().stackOrder(new TurnTo(targetAngle), this);
				}
			}
			
			// Accelerate if necessary and if pointing right direction
			double deltaTarget = abs(Utility.fixAngle(targetAngle-host.getAngle()));
			if (timeToAccel > 0){
				double angleThresh = max(3, min(10, 500/timeToAccel));
				if (deltaTarget < angleThresh){
					((Controllable)host).accelForward();
					timeToAccel--;
				}
			}
		}
	}
	
	static final double TANGENTIAL_INCREASE = 0.26; // Larger value will make missile more concerned with eliminating tangential velocity
	private void setTimeAndAngle(){
		double Dx = getDx(), Dy = getDy(), Vx = getVx(), Vy = getVy();
		double a = ((Controllable)host).getAccel();
    	
		// Set cruising speed to maximum of current speed, and speed determined by closingFuel of host MissileType
    	distance = sqrt(Dx*Dx+Dy*Dy);
    	closingSpeed = -(Dx*Vx+Dy*Vy)/distance;
    	double deltaVRemaining = ((Missile)host).getEnergy()/Main.config.energyPerThrust/((Missile)host).type.projectileMass;
    	double targetClosingSpeed = (closingSpeed + deltaVRemaining)/(1.0+((Missile)host).type.closingFuel);
    	double Sc = max(closingSpeed, targetClosingSpeed);
    	
    	double deltaVx = Vx+Dx*Sc/distance;
    	double deltaVy = Vy+Dy*Sc/distance;
    	
    	double r = sqrt(Dx*Dx + Dy*Dy);
    	double rUnitX = Dx/r, rUnitY = Dy/r;
    	double deltaVRad = deltaVx*rUnitX + deltaVy*rUnitY;
    	double deltaVRadX = rUnitX*deltaVRad, deltaVRadY = rUnitY*deltaVRad;
    	deltaVx = (deltaVx - TANGENTIAL_INCREASE*deltaVRadX)/(1 - TANGENTIAL_INCREASE);
    	deltaVy = (deltaVy - TANGENTIAL_INCREASE*deltaVRadY)/(1 - TANGENTIAL_INCREASE);
    	
    	targetAngle = 90-toDegrees(atan2(-deltaVy, deltaVx));
    	timeToAccel = (int)round(sqrt(deltaVx*deltaVx+deltaVy*deltaVy)/a);
	}
	
	public double getETA(){
		if (finalApproach){
			return finalApproachOrder.getTime();
		}else
			return distance/max(0, closingSpeed);
	}
	
	public Color getColor(){
		return new Color(255, 0, 0, finalApproach ? OPAQUE_ALPHA : TRANSLUCENT_ALPHA);
	}
}
