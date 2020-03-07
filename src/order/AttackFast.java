import static java.lang.Math.*;

public class AttackFast extends MovePastTarget{
	
	final FaceWeapons faceOrder;
	final Target target;
	boolean isFacing;
	
	public AttackFast(Target target){
		super(target);
		this.target = target;
		faceOrder = new FaceWeapons();
	}
	
	public void setHost(Controllable host){
		super.setHost(host);
		faceOrder.setHost(host);
		((Unit)host).setTarget(target);
	}
	
	public void act(){
		Unit host = (Unit)this.host;
		
		if (time%11 == 0){
			double targetAngle = faceOrder.getFaceAngle(target);
			double leadTime = targetAngle != FaceWeapons.NO_ANGLE ? TurnTo.approxTurnTime(host, targetAngle) : 0;
			
			if (isFacing && !(target.isVisible() && host.weaponsInRange(target, leadTime))){
				host.orders().finish(faceOrder);
				isFacing = false;
			}else if (!isFacing && target.isVisible() && host.weaponsInRange(target, leadTime)){
				host.orders().stackOrder(faceOrder, this);
				isFacing = true;
			}
		}
		
		if (!isFacing){
			super.act();
		}else{
			if (abs(Utility.fixAngle(targetAngle-host.getAngle())) < 10)
				((Controllable)host).accelForward();
			if (Main.game.turn-setAngleTime > 5)
				setTimeAndAngle();
		}
	}
	
}
