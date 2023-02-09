import static java.lang.Math.*;
import java.awt.*;

// Base class for orders targeting a stationary point in space

public abstract class StationaryOrder extends LocatableOrder{
	double completionDist;
	double completionSpeed;
	
	final double velX, velY;
	double posX, posY, renderPosX, renderPosY;
	boolean finished;

	public StationaryOrder(double posX, double posY,
			double velX, double velY,
			double completionDist, double completionSpeed){
		this.posX = posX;
		this.posY = posY;
		this.velX = velX;
		this.velY = velY;
		
		this.completionDist = completionDist;
		this.completionSpeed = completionSpeed;
		
		renderPosX = Double.NaN;
		renderPosY = Double.NaN;
	}
	
	public void act(){
		if (!finished){
			if (abs(posX-host.getPosX()) < completionDist &&
					abs(posY-host.getPosY()) < completionDist &&
					((abs(velX-host.getVelX()) < completionSpeed &&
							abs(velY-host.getVelY()) < completionSpeed) || completionSpeed <= 0))
				finished = true;
		}else{
			((Controllable)host).stopTurn();
			if (abs(host.getTurnSpeed()) < 0.01)
				((Controllable)host).orders().finish(this);
		}
	}
	
	public void move(){
		super.move();
		posX += velX;
		posY += velY;
	}
	
	public void recordPos(){
		renderPosX = posX;
		renderPosY = posY;
	}
	public double getRenderPosX(){
		return renderPosX;
	}
	public double getRenderPosY(){
		return renderPosY;
	}
	
	protected double getDx(){
		return posX-host.getPosX();
	}
	protected double getDy(){
		return posY-host.getPosY();
	}
	protected double getVx(){
		return velX-host.getVelX();
	}
	protected double getVy(){
		return velY-host.getVelY();
	}
	
	public double getPosX(){
		return posX;
	}
	public double getPosY(){
		return posY;
	}
	public double getVelX(){
		return velX;
	}
	public double getVelY(){
		return velY;
	}
	
	public Color getColor(){
		return new Color(100, 100, 200, TRANSLUCENT_ALPHA);
	}
}
