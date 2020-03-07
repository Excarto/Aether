import static java.lang.Math.*;

public class MoveTo extends Order{
	static final int TURNS_SET_TIME = 5;
	
	final Locatable target;
	int turnsUntilSetAngle;
	int timeToAccel;
	double targetAngle;
	
	public MoveTo(Locatable target){
		this.target = target;
	}
	
	public void act(){
		
		if (turnsUntilSetAngle-- < 0){
			setTimeAndAngle();
			if (timeToAccel > 1){
				double deltaTarget = Utility.fixAngle(targetAngle-host.getAngle());
				double dampedTargetAngle = targetAngle;
				if (abs(deltaTarget) < 5 && abs(host.bearing(target)) > 90)
					dampedTargetAngle -= deltaTarget*0.95;
				
				((Controllable)host).orders().stackOrder(new TurnTo(dampedTargetAngle), this);
			}
			turnsUntilSetAngle = TURNS_SET_TIME;
		}
		
		if (timeToAccel > 0){
			double deltaTarget = Utility.fixAngle(targetAngle-host.getAngle());
			//double angleThresh = max(3, min(24, 4*Main.TPS/timeToAccel));
			double angleThresh = max(5, min(30, 10*timeToAccel/Main.TPS));
			if (abs(deltaTarget) < angleThresh){
				((Controllable)host).accelForward();
				timeToAccel--;
			}
		}
	}
	
	static final double TANGENTIAL_INCREASE = 0.23;
	private void setTimeAndAngle(){
		double Dx = target.getPosX()-host.getPosX(), Dy = target.getPosY()-host.getPosY();
		double Vx = target.getVelX()-host.getVelX(), Vy = target.getVelY()-host.getVelY();
    	double a = 0.85*((Controllable)host).getAccel();
    	
    	double distance = sqrt(Dx*Dx+Dy*Dy);
    	double turnTime = 0.91*TurnTo.approxTurnTime((Controllable)host, 180+host.heading(target));
    	double bufferedTurnTime = turnTime + 3*Main.TPS/16;
    	double Sc = sqrt(a*a*bufferedTurnTime*bufferedTurnTime+2*a*distance)-a*bufferedTurnTime;
    	
    	double deltaVx = Vx+Dx*Sc/distance, deltaVy = Vy+Dy*Sc/distance;
    	
    	double r = sqrt(Dx*Dx + Dy*Dy);
    	double rUnitX = Dx/r, rUnitY = Dy/r;
    	double deltaVRad = deltaVx*rUnitX + deltaVy*rUnitY;
    	double deltaVRadX = rUnitX*deltaVRad, deltaVRadY = rUnitY*deltaVRad;
    	deltaVx = (deltaVx - TANGENTIAL_INCREASE*deltaVRadX)/(1 - TANGENTIAL_INCREASE);
    	deltaVy = (deltaVy - TANGENTIAL_INCREASE*deltaVRadY)/(1 - TANGENTIAL_INCREASE);
    	
    	double deltaV = sqrt(deltaVx*deltaVx+deltaVy*deltaVy);
    	
    	targetAngle = 90-toDegrees(atan2(-deltaVy, deltaVx));
    	timeToAccel = (int)round(deltaV/a);
    	
    	if (turnTime > 14*timeToAccel)
    		timeToAccel = 0;
    	
    	// Takes effect when tta < 3*TURNS_SET_TIME
    	timeToAccel /= 3;
	}
	
}
