import static java.lang.Math.*;
import java.awt.*;

// Order to move past a moving target, with no restriction on what speed

public class MovePastTarget extends TrackOrder{
	
	int setAngleTime;
	double targetVelX, targetVelY;
	double time;
	double targetAngle;
	
	public MovePastTarget(Locatable target){
		super(target);
		setAngleTime = 0;
		targetVelX = this.target.getVelX();
		targetVelY = this.target.getVelY();
		setDrawArrow(true);
	}
	
	public void act(){
		if (abs(Utility.fixAngle(targetAngle-host.getAngle())) < 18)
			((Controllable)host).accelForward();
		
		if (Main.game.turn-setAngleTime > 5){
			setTimeAndAngle();
			((Controllable)host).orders().stackOrder(new TurnTo(targetAngle), this);
		}
	}
	
	// Accelerate in order to set velocity to move toward target with radial component slightly larger than the current value
	static final double TANGENTIAL_INCREASE = 0.40; // Larger values make it more concerned with eliminating tangential velocity
	public void setTimeAndAngle(){
		double Dx = getDx(), Dy = getDy(), Vx = getVx(), Vy = getVy();
    	
    	double distance = sqrt(Dx*Dx+Dy*Dy);
    	double closingSpeed = -(Dx*Vx+Dy*Vy)/distance;
    	double Sc = closingSpeed+1.0;
    	
    	double deltaVx = Vx+Dx*Sc/distance;
    	double deltaVy = Vy+Dy*Sc/distance;
    	
    	double r = sqrt(Dx*Dx + Dy*Dy);
    	double rUnitX = Dx/r, rUnitY = Dy/r;
    	double deltaVRad = deltaVx*rUnitX + deltaVy*rUnitY;
    	double deltaVRadX = rUnitX*deltaVRad, deltaVRadY = rUnitY*deltaVRad;
    	deltaVx = (deltaVx - TANGENTIAL_INCREASE*deltaVRadX)/(1 - TANGENTIAL_INCREASE);
    	deltaVy = (deltaVy - TANGENTIAL_INCREASE*deltaVRadY)/(1 - TANGENTIAL_INCREASE);
    	
    	targetAngle = 90-toDegrees(atan2(-deltaVy, deltaVx));
    	time = distance/max(0, closingSpeed);
    	setAngleTime = Main.game.turn;
	}
	
	public double getTime(){
		return time;
	}
	
	public Color getColor(){
		return new Color(255, 0, 0, OPAQUE_ALPHA);
	}
}
