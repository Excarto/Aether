
// Order to move to a stationary point in space. Basically just a wrapper vor MoveOrder

public class MoveToPoint extends StationaryOrder{
	
	final MoveTo moveOrder;
	
	public MoveToPoint(double posX, double posY, double velX, double velY){
		super(posX, posY, velX, velY, 250, 0.062);
		moveOrder = new MoveTo(this);
	}
	
	public void act(){
		super.act();
		if (!((Controllable)host).orders().isActive(moveOrder))
			((Controllable)host).orders().stackOrder(moveOrder, this);
	}
}
