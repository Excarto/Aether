import static java.lang.Math.*;

public class MoveToTarget extends TrackOrder{
	static final int TURNS_SET_ACCEL = 5;
	
	final MoveTo moveOrder;
	double targetVelX, targetVelY;
	double targetAccelX, targetAccelY;
	double timeToTarget;
	
	public MoveToTarget(final Locatable locatable){
		super(locatable);
		
		moveOrder = new MoveTo(new Locatable(){
			public double getPosX(){
				return target.getPosX() + targetAccelX*timeToTarget*timeToTarget/2;
			}
			public double getPosY(){
				return target.getPosY() + targetAccelY*timeToTarget*timeToTarget/2;
			}
			public double getVelX(){
				return target.getVelX() + targetAccelX*timeToTarget;
			}
			public double getVelY(){
				return target.getVelY() + targetAccelY*timeToTarget;
			}
		});
	}
	
	public void act(){
		if (runTime%TURNS_SET_ACCEL == 0){
			if (!((Controllable)host).orders().isActive(moveOrder))
				((Controllable)host).orders().stackOrder(moveOrder, this);
			
			double newTargetVelX = target.getVelX(), newTargetVelY = target.getVelY();
			targetAccelX = targetAccelX*15/16 + Utility.clamp((newTargetVelX-targetVelX)/TURNS_SET_ACCEL, 0.05)/16;
			targetAccelY = targetAccelY*15/16 + Utility.clamp((newTargetVelY-targetVelY)/TURNS_SET_ACCEL, 0.05)/16;
			targetVelX = newTargetVelX;
			targetVelY = newTargetVelY;
			
			timeToTarget = sqrt(2*host.distance(target)/((Controllable)host).getAccel());
			timeToTarget = pow(timeToTarget, 0.893);
		}
	}
	
}
