
// Base class for orders targeting a moving object

public abstract class TrackOrder extends LocatableOrder{
	public final Locatable target;
	double renderPosX, renderPosY;
	
	public TrackOrder(Locatable target){
		super();
		this.target = target;
		renderPosX = Double.NaN;
		renderPosY = Double.NaN;
	}
	
	public void recordPos(){
		renderPosX = getPosX();
		renderPosY = getPosY();
	}
	public double getRenderPosX(){
		return renderPosX;
	}
	public double getRenderPosY(){
		return renderPosY;
	}
	
	protected double getDx(){
		return this.getPosX()-host.getPosX();
	}
	protected double getDy(){
		return this.getPosY()-host.getPosY();
	}
	protected double getVx(){
		return target.getVelX()-host.getVelX();
	}
	protected double getVy(){
		return target.getVelY()-host.getVelY();
	}
	
	public double getPosX(){
		return target.getPosX();
	}
	public double getPosY(){
		return target.getPosY();
	}
	public double getVelX(){
		return target.getVelX();
	}
	public double getVelY(){
		return target.getVelY();
	}
}
