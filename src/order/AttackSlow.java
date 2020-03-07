import static java.lang.Math.*;
import java.awt.*;

public class AttackSlow extends MoveToTarget{
	public static final int STANDOFF_DIST = 300;
	
	final Target target;
	final FaceWeapons faceOrder;
	boolean isFacing;
	double distance;
	
	public AttackSlow(Target tar){
		super(tar);
		this.target = tar;
		isFacing = true;
		
		faceOrder = new FaceWeapons();
	}
	
	public void setHost(Controllable host){
		super.setHost(host);
		faceOrder.setHost(host);
		((Unit)host).setTarget(target);
	}
	
	public void act(){
		Unit host = (Unit)this.host;
		distance = host.distance(target);
		
		if (runTime%8 == 0){
			double targetAngle = faceOrder.getFaceAngle(target);
			double leadTime = 0;
			if (targetAngle != FaceWeapons.NO_ANGLE)
				leadTime = Main.TPS/4 + 1.1*TurnTo.approxTurnTime(host, targetAngle);
			
			if (isFacing && (targetAngle == FaceWeapons.NO_ANGLE || !target.isVisible() || !host.weaponsInRange(target, leadTime))){
				host.orders().finish(faceOrder);
				isFacing = false;
			}else if (!isFacing && target.isVisible() && targetAngle != FaceWeapons.NO_ANGLE && host.weaponsInRange(target, leadTime))
				isFacing = true;
			
			if (isFacing && !host.orders().isActive(faceOrder))
				host.orders().stackOrder(faceOrder, this);
		}
		
		if (!isFacing)
			super.act();
		if (isFacing && abs(host.bearing(target)) < 20 &&
				host.radVel(target) > -host.getAccel()*Main.TPS/8 && distance > 300)
			host.accelForward();
		if (distance < host.getSize()/2 && host.speed(target)*Main.TPS*2 < host.getSize()){
			if (target.isVisible()){
				host.accelForward();
			}else
				host.orders().finish(this);
		}
	}
	
	public Color getColor(){
		return new Color(255, 0, 0, TRANSLUCENT_ALPHA);
	}
	
	public double getPosX(){
		if (host == null)
			return super.getPosX();
		if (distance < STANDOFF_DIST){
			return host.getPosX();
		}else
			return super.getPosX()+STANDOFF_DIST*(host.getPosX()-super.getPosX())/distance;
	}
	public double getPosY(){
		if (host == null)
			return super.getPosY();
		if (distance < STANDOFF_DIST){
			return host.getPosY();
		}else
			return super.getPosY()+STANDOFF_DIST*(host.getPosY()-super.getPosY())/distance;
	}
}
