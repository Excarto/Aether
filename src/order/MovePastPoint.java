import static java.lang.Math.*;
import java.awt.*;

public class MovePastPoint extends StationaryOrder{
		
	private int turnsUntilSetAngle;
	private double targetAngle;

	public MovePastPoint(int posX, int posY, double velX, double velY){
		super(posX, posY, velX, velY, 300, -1);
		
		turnsUntilSetAngle = 0;
		setDrawArrow(true);
	}
	
	public void act(){
		super.act();
		
		if (finished){
			((Controllable)host).orders().stackOrder(null, this);
		}else{
			if (turnsUntilSetAngle-- <= 0){
				setTargetAngle();
				if (abs(Utility.fixAngle(targetAngle-host.getAngle())) > 2)
					((Controllable)host).orders().stackOrder(new TurnTo(targetAngle), this);
				turnsUntilSetAngle = 20;
			}
			if (abs(Utility.fixAngle(targetAngle-host.getAngle())) < 6)
				((Controllable)host).accelForward();
		}
		
	}
	
	private void setTargetAngle(){
		double Dx = getDx(), Dy = getDy(), Vx = getVx(), Vy = getVy();
    	double a = ((Controllable)host).getAccel();
    	
    	double time = Utility.getZero(new double[]{
    			4*(Dx*Dx+Dy*Dy),
				8*(Dx*Vx+Dy*Vy),
				4*(Vx*Vx+Vy*Vy),
				0,
				-a*a
    	});
    	
    	targetAngle = toDegrees(atan2(2*Dx+2*Vx*time, -(2*Dy+2*Vy*time)));
	}
	
	public Color getColor(){
		return new Color(100, 100, 180, OPAQUE_ALPHA);
	}
}
