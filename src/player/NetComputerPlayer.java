import java.util.*;

public class NetComputerPlayer extends ComputerPlayer{

	public NetComputerPlayer(String name, int team, List<Ship> ships, int budget, Arena arena) {
		super(name, team, ships, budget, arena);
	}
	
	public void fireWeapon(Weapon weapon){
		Main.game.sendMsg(weapon.getFireMsg());
		super.fireWeapon(weapon);
	}
	
	public void contact(Projectile projectile, Controllable controllable){
		ContactMsg msg = projectile.getContactMsg(controllable);
		projectile.contact(controllable);
		if (controllable.getHull() <= 0){
			controllable.explode();
			msg.explodes = true;
		}
		Main.game.sendMsg(msg);
	}
	
	public void contact(Beam beam, Controllable controllable, double posX, double posY){
		BeamContactMsg msg = beam.getContactMsg(controllable, posX, posY);
		beam.contact(controllable, posX, posY);
		if (controllable.getHull() <= 0){
			controllable.explode();
			msg.explodes = true;
		}
		Main.game.sendMsg(msg);
	}
	
	public void launchCraft(Ship ship, Craft craft, boolean catapult){
		Main.game.sendMsg(ship.getLaunchMsg(craft, catapult));
		super.launchCraft(ship, craft, catapult);
	}
	
	public void retrieveCraft(Ship ship, Craft craft){
		Main.game.sendMsg(ship.getDockMsg(craft));
		super.retrieveCraft(ship, craft);
	}
	
	public void capture(Arena.Objective objective, int amount){
		Main.game.sendMsg(objective.getCapMsg(this, amount));
		objective.capture(this, amount);
	}
	
	public void repair(Repairable repairable, double material, boolean isScrap){
		repairable.repair(material, isScrap);
		if (!repairable.getPlayer().isMaster()){
			RepairMsg msg = new RepairMsg();
			msg.repairable = repairable;
			msg.material = material;
			msg.isScrap = isScrap;
			Main.game.sendMsg(msg);
		}
	}
	
	public void warpIn(Ship ship){
		Main.game.sendMsg(ship.getWarpMsg());
		confirmWarp(ship);
	}
}
