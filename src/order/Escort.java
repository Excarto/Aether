import static java.lang.Math.*;
import java.awt.*;

// Order to maintain a specific bearing and distance to target

public class Escort extends MoveToTarget{
	
	final Sprite target;
	
	// If time to target escort position < slack, then use maneuvering thrusters to stay in place
	// and face weapons
	final int slack;
	
	private boolean isFacing;
	
	public Escort(final Sprite sprite, final int slack, final double angle, final double distance){
		super(new Locatable(){
			public double getPosX(){
				return sprite.getPosX()+distance*sin(toRadians(angle/*+target.getAngle()*/));
			}
			public double getPosY(){
				return sprite.getPosY()-distance*cos(toRadians(angle/*+target.getAngle()*/));
			}
			public double getVelX(){
				return sprite.getVelX();
			}
			public double getVelY(){
				return sprite.getVelY();
			}
			public boolean equals(Object other){
				return sprite.equals(other) || super.equals(other);
			}
		});
		this.target = sprite;
		this.slack = slack;
	}
	
	public void setHost(Controllable host){
		super.setHost(host);
		isFacing = true;
	}
	
	public void act(){
		if (isFacing){
			((Unit)host).accelManeuver(180+host.velBearing(this));
		}else
			super.act();
		
		if (runTime%20 == 0){
			double a = ((Controllable)host).getAccel();
			double timeToTarget = sqrt(4*host.distance(this)/a) + 2.0*host.speed(this)/a;
			if (isFacing && timeToTarget > slack){
				isFacing = false;
			}else if (!isFacing && timeToTarget < slack*3/4){
				((Controllable)host).orders().stackOrder(new FaceWeapons(), this);
				isFacing = true;
			}
		}
	}
	
	public Color getColor(){
		return new Color(0, 255, 0, 55);
	}
}
