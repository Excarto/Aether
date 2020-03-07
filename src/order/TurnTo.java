import static java.lang.Math.*;

public class TurnTo extends Order{
	static final double STOP_DIST_SLACK = 0.5;
	
	public final int targetAngle;
	
	private boolean isAccelerating;
	private int accelDirection, accelTime;
	private double turnAccel, stopDist;
	
	public TurnTo(double angle){
		this.targetAngle = (int)Utility.fixAngle(angle);
	}
	
	public void setHost(Controllable host){
		super.setHost(host);
		turnAccel = ((Controllable)host).getTurnAccel();
		setAccelTimeAndDirection();
		isAccelerating = true;
		act();
	}
	
	public void act(){
		if (runTime%32 == 0)
			turnAccel = ((Controllable)host).getTurnAccel();
		
		double turnSpeed = host.getTurnSpeed();
		stopDist = turnSpeed*turnSpeed/(2*turnAccel);
		
		if (stopDist > 170){
			((Controllable)host).stopTurn();
		}else{
			double angleDiff = Utility.fixAngle(targetAngle-host.getAngle());
			double distToTarget;
			if (signum(angleDiff) == signum(turnSpeed)){
				distToTarget = abs(angleDiff);
			}else
				distToTarget = 360-abs(angleDiff);
			
			if (isAccelerating){
				if ((distToTarget > (1 + STOP_DIST_SLACK)*stopDist || distToTarget < stopDist) && accelTime > 0){
					((Controllable)host).accelTurn(accelDirection > 0);
					accelTime--;
				}else
					isAccelerating = false;
			}else{
				if (abs(turnSpeed) < 0.0001 && distToTarget < 1){
					((Controllable)host).orders().finish(this);
				}else if (distToTarget > (1 + STOP_DIST_SLACK)*stopDist){
					accelDirection = angleDiff > 0 ? 1 : -1;
					((Controllable)host).accelTurn(accelDirection > 0);
				}else if (distToTarget > stopDist && distToTarget < stopDist+10*abs(turnSpeed)){
					accelDirection = turnSpeed > 0 ? -1 : 1;
					((Controllable)host).stopTurn();
				}
				
			}
		}
	}
	
	private void setAccelTimeAndDirection(){
		double distance = targetAngle-host.getAngle();
		if (distance > 0)
			distance -= 360;
		int minTotalTime = Integer.MAX_VALUE;
		
		double a, b, c;
		double turnAccel = 0.95*this.turnAccel;
		double turnSpeed = host.getTurnSpeed();
		a = 2*turnAccel*turnAccel;
		for (int direction = -1; direction <= 1; direction += 2){
			b = 4*turnAccel*turnSpeed;
			for (double target = distance; target < distance+721; target += 360){
				c = turnSpeed*turnSpeed-2*turnAccel*target;
				double timeToAccel = (sqrt(b*b-4*a*c)-b)/(2*a);
				if (timeToAccel > 0 && !Double.isNaN(timeToAccel)){
					double totalTime = timeToAccel+abs(turnSpeed/turnAccel+timeToAccel);
					totalTime = totalTime*(1.0 + abs(target)/1200.0);
					if (totalTime < minTotalTime){
						minTotalTime = (int)round(totalTime);
						this.accelDirection = -direction;
						this.accelTime = (int)timeToAccel;
					}
				}
			}
			turnAccel = -turnAccel;
			distance -= 360;
		}
	}
	
	public int getArc(){
		if (host == null)
			return 0;
		int angle = (int)host.getAngle();
		double speed = host.getTurnSpeed();
		int arc = (int)Utility.fixAngle(angle-targetAngle);
		int stopAngle = (int)signum(speed)*(int)stopDist;
		int stoppedArc = arc+stopAngle;
		
		if (stoppedArc > 180){
			arc -= 360;
		}else if (stoppedArc < -180)
			arc += 360;
		return -arc;
	}
	
	public static double approxTurnTime(Controllable controllable, double angle){
		double accel = controllable.getTurnAccel();
		double deltaAngle = Utility.fixAngle(angle-((Sprite)controllable).getAngle());
		double turnSpeed = ((Sprite)controllable).getTurnSpeed();
		double distToward, distAway;
		if (turnSpeed > 0){
			if (deltaAngle > 0){
				distToward = deltaAngle;
				distAway = 360-deltaAngle;
			}else{
				distToward = 360+deltaAngle;
				distAway = -deltaAngle;
			}
		}else{
			if (deltaAngle > 0){
				distToward = 360-deltaAngle;
				distAway = deltaAngle;
			}else{
				distToward = -deltaAngle;
				distAway = 360+deltaAngle;
			}
		}
		turnSpeed = abs(turnSpeed);
		
		double timeToward = sqrt(4*distToward/accel + 2*turnSpeed*turnSpeed/(accel*accel)) - turnSpeed/accel;
		double timeAway = sqrt(4*distAway/accel + 2*turnSpeed*turnSpeed/(accel*accel)) + turnSpeed/accel;
		return min(timeToward > 0 ? timeToward : Double.POSITIVE_INFINITY,
				timeAway > 0 ? timeAway : Double.POSITIVE_INFINITY);
	}
}
