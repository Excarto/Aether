import static java.lang.Math.*;
import java.awt.*;

public class Orbit extends TrackOrder{
	private static final double C_SPEED = 0.5, C_CLOSE = 1.2, C_ORBIT = 1.0;
	private static final int TURNS_SET_ANGLE = 21;
	
	final double orbDist;
	int turnsUntilSetAngle;
	int turnsAccel;
	double targetAngle;
	
	public Orbit(Locatable target, double orbDist){
		super(target);
		this.orbDist = orbDist;
	}
	
	public void act(){
		if (turnsUntilSetAngle-- <= 0){
			turnsUntilSetAngle = TURNS_SET_ANGLE;
			setTimeAndAngle();
			((Controllable)host).orders().stackOrder(new TurnTo(targetAngle), this);
		}
		if (turnsAccel > 0 && abs(Utility.fixAngle(host.getAngle()-targetAngle)) < 30){
			((Controllable)host).accelForward();
			turnsAccel--;
		}
	}
	
	private void setTimeAndAngle(){
		double dx = getDx(), dy = getDy(), vx = getVx(), vy = getVy();
		double dist = sqrt(dx*dx + dy*dy);
		double a = ((Controllable)host).getAccel();
		
		double vOrbit = (dx*vy - dy*vx)/dist;
		double vClose = (dx*vx + dy*vy)/dist;
		double aCentripital = vOrbit*vOrbit/dist;
		
		double vOrbitTarget = (vOrbit > 0 ? 1 : -1)*sqrt(C_SPEED*a*orbDist);
		double aCloseMax = orbDist > dist ? 0.8*a : max(0, a-aCentripital);
    	double vCloseTarget = signum(orbDist-dist)*sqrt(2*aCloseMax*abs(orbDist-dist));
    	
    	double aClose = C_CLOSE*a*(vCloseTarget-vClose)/abs(vOrbitTarget);
    	double aOrbit = C_ORBIT*a*(vOrbitTarget-vOrbit)/abs(vOrbitTarget);
    	
    	double aCloseTot = aClose-aCentripital;
    	double aAngle = toDegrees(atan2(-aOrbit, -aCloseTot));
    	double aMag = sqrt(aOrbit*aOrbit + aCloseTot*aCloseTot);
    	
    	turnsAccel = (int)(TURNS_SET_ANGLE*aMag/a);
    	targetAngle = host.heading(target)+aAngle;
	}
	
	public Color getColor(){
		return new Color(240, 125, 0, TRANSLUCENT_ALPHA);
	}
}
