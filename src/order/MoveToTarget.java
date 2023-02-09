import static java.lang.Math.*;

// Order to come to a stop on a moving target, accounting for target acceleration

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
			
			// Compute running average acceleration of target
			double newTargetVelX = target.getVelX(), newTargetVelY = target.getVelY();
			targetAccelX = targetAccelX*15/16 + Utility.clamp((newTargetVelX-targetVelX)/TURNS_SET_ACCEL, 0.05)/16;
			targetAccelY = targetAccelY*15/16 + Utility.clamp((newTargetVelY-targetVelY)/TURNS_SET_ACCEL, 0.05)/16;
			targetVelX = newTargetVelX;
			targetVelY = newTargetVelY;
			
			// Try to account for target acceleration and avoid overshoot. Empirical damping formula used
			timeToTarget = sqrt(2*host.distance(target)/((Controllable)host).getAccel());
			timeToTarget = pow(timeToTarget, 0.893);
		}
	}
	
}
