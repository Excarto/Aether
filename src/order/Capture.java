import java.awt.*;

// Order to capture objective. Uses a MoveTo order to get close, then switches to staying in
// place and capturing until complete or it get pushed away

public class Capture extends TrackOrder{
	public final static int TURNS_CAPTURE = Main.TPS;
	
	final Arena.Objective objective;
	final MoveTo moveOrder;
	Unit host;
	
	public Capture(Arena.Objective objective){
		super(objective);
		this.objective = objective;
		moveOrder = new MoveTo(objective);
	}
	
	public void setHost(Controllable host){
		super.setHost(host);
		this.host = (Unit)host;
		
		// Clear existing Capture orders of the same objective for other units
		for (Controllable controllable : ((Controllable)host).getPlayer().controllables){
			if (controllable.orders().getOrder() instanceof Capture){
				Capture order = (Capture)controllable.orders().getOrder();
				if (order.objective == objective && order != this)
					controllable.orders().finish(order);
			}
		}
	}
	
	public void act(){
		// Check if done
		if (objective.isCaptured && objective.owner == host.getPlayer().team
				&& objective.capAmount >= objective.capSize)
			host.orders().finish(this);
		
		// Check if close enough to capture
		if (host.distance(objective) < Main.config.captureDistance &&
					host.speed(objective) < Main.config.captureSpeed && host.getTurnSpeed() < 0.1){
			host.orders().finish(moveOrder);
			
			host.stopTurn();
			host.accelManeuver(180+host.velBearing(objective));
			
			if (runTime%TURNS_CAPTURE == 0)
				host.getPlayer().capture(objective, (int)(host.type.captureRate*TURNS_CAPTURE));
		}else{
			if (!host.orders().isActive(moveOrder))
				host.orders().stackOrder(moveOrder, this);
		}
	}
	
	public boolean isCapturing(){
		return !host.orders().isActive(moveOrder);
	}
	
	public Color getColor(){
		return Arena.OBJ_COLOR_NEUTRAL;
	}
}
