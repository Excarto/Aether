import java.awt.*;

// Order for craft to dock with mothership

public class Dock extends Escort{
	
	public Dock(Ship ship){
		super(ship, 0, 0, 0);
	}
	
	public void act(){
		super.act();
		
		Ship ship = (Ship)target;
		if (runTime%5 == 0){
			// Cancel if unable to dock
			if (ship.type.craftMass < ((Craft)host).type.mass ||
					ship.type.totalCraftMass < ship.totalCraftMass()+((Craft)host).type.mass)
				((Controllable)host).orders().finish(this);
			
			// Dock if close enough
			if (host.distance(target) < Main.config.craftDockDistance &&
					host.speed(target) < Main.config.craftDockSpeed && ((Craft)host).canDock())
				ship.player.retrieveCraft(ship, (Craft)host);
		}
	}
	
	public Color getColor(){
		return new Color(0, 255, 0, OPAQUE_ALPHA);
	}
}
